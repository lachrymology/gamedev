package patagonia;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


import patagonia.callbacks.Callback;
import patagonia.edn.Keyword;
import patagonia.edn.printer.Printers;
import patagonia.errors.PatagoniaException;
import patagonia.http.HttpHost;
import patagonia.http.HttpRequestInterceptor;
import patagonia.http.HttpResponse;
import patagonia.http.client.HttpClient;
import patagonia.http.client.methods.HttpPost;
import patagonia.http.concurrent.FutureCallback;
import patagonia.http.entity.InputStreamEntity;
import patagonia.http.entity.StringEntity;
import patagonia.http.impl.DefaultConnectionReuseStrategy;
import patagonia.http.impl.client.DefaultHttpClient;
import patagonia.http.impl.nio.DefaultHttpClientIODispatch;
import patagonia.http.impl.nio.pool.BasicNIOConnPool;
import patagonia.http.impl.nio.reactor.DefaultConnectingIOReactor;
import patagonia.http.impl.nio.reactor.IOReactorConfig;
import patagonia.http.message.BasicHttpEntityEnclosingRequest;
import patagonia.http.nio.protocol.BasicAsyncRequestProducer;
import patagonia.http.nio.protocol.BasicAsyncResponseConsumer;
import patagonia.http.nio.protocol.HttpAsyncRequestExecutor;
import patagonia.http.nio.protocol.HttpAsyncRequester;
import patagonia.http.nio.reactor.ConnectingIOReactor;
import patagonia.http.nio.reactor.IOEventDispatch;
import patagonia.http.nio.reactor.IOReactorException;
import patagonia.http.params.CoreConnectionPNames;
import patagonia.http.params.CoreProtocolPNames;
import patagonia.http.params.HttpParams;
import patagonia.http.params.SyncBasicHttpParams;
import patagonia.http.protocol.BasicHttpContext;
import patagonia.http.protocol.HttpProcessor;
import patagonia.http.protocol.ImmutableHttpProcessor;
import patagonia.http.protocol.RequestConnControl;
import patagonia.http.protocol.RequestContent;
import patagonia.http.protocol.RequestExpectContinue;
import patagonia.http.protocol.RequestTargetHost;
import patagonia.http.protocol.RequestUserAgent;
import patagonia.processes.AttachmentProcess;

public class LongPollClient implements IClient {
	private static Logger log = Logger.getLogger(LongPollClient.class.toString());
	
	private String host;
	private int port;
	private String path;
	private volatile Map<String,String> credentials = new HashMap<String,String>();
	private volatile UUID channel;
	
	private ConnectingIOReactor ioReactor;
    private HttpProcessor httpproc;
    private HttpParams params;
    private BasicNIOConnPool pool;
	
    public LongPollClient(String host, int port, String path, Map<String, String> credentials) {
        this.host = host;
        this.port = port;
        this.path = path;
        this.credentials = credentials;
        try {
			this.init();
		} catch (PatagoniaException e) {
			e.printStackTrace();
		}
    }
    
    private void initParams() {
        this.params = new SyncBasicHttpParams();
        this.params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 3000000)
                .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.USER_AGENT, "DSR");
    }
    
    private void initHTTPProcess() {
    	this.httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[]{
            new RequestContent(),
            new RequestTargetHost(),
            new RequestConnControl(),
            new RequestUserAgent(),
            new RequestExpectContinue()
        });
    }
    
    private void initReactor() throws PatagoniaException {
        IOReactorConfig config = new IOReactorConfig();
        config.setIoThreadCount(1);

        try {
        	this.ioReactor = new DefaultConnectingIOReactor(config);
        } catch (IOReactorException e) {
            throw new PatagoniaException(e.getMessage());
        }
    }
    
    private void initPool() {
        this.pool = new BasicNIOConnPool(this.ioReactor, this.params);
        this.pool.setDefaultMaxPerRoute(2);    //
        this.pool.setMaxTotal(2);              //
    }

    private Thread buildChannelThread() {
        final HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();
        final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(protocolHandler, this.params);
        
        return new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    log.severe("Interrupted");
                } catch (IOException e) {
                    log.severe("I/O error: " + e.getMessage());
                }
            }
        });
    }
    
    public void init() throws PatagoniaException {
    	initParams();
    	initHTTPProcess();
        initReactor();
        initPool();
        buildChannelThread().start();
    }
    
    private void listen(String endpoint, String method, final InputStream inputStream, final Callback callBack) {
    	DefaultConnectionReuseStrategy strategy = new DefaultConnectionReuseStrategy();   //
    	HttpAsyncRequester requester = new HttpAsyncRequester(this.httpproc, strategy, this.params);
    	
        final HttpHost target = new HttpHost(host, port, "http");   //
        
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(method, this.path + endpoint);
        request.setEntity(new InputStreamEntity(inputStream, -1));
        Util.cookieDecoration(this.credentials, request);
        
        requester.execute(
                new BasicAsyncRequestProducer(target, request),
                new BasicAsyncResponseConsumer(),
                pool,
                new BasicHttpContext(),
                new FutureCallback<HttpResponse>() {
                    public void completed(final HttpResponse response) {
                    	listen(callBack);
                    	
                    	callBack.completed(response);
                    }
                    public void failed(final Exception ex) {
                        callBack.failed(ex);
                    }
                    public void cancelled() {
                        callBack.cancelled();
                    }
                });
    }
    
    
    private void listen(Callback callback) {
    	Map<Keyword,Object> packet = Util.buildContextPacket(this.channel, this.credentials);
    	String message = Printers.printString(packet);
    	
    	listen("source", "POST", new ByteArrayInputStream(message.getBytes()), callback);
    }

	public void attach(String context, Callback callback) {
		try {
	        String url = "http://" + this.host + ":" + this.port + "/context/new/" + context;
	        
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpPost post = new HttpPost(url);
	        Util.cookieDecoration(this.credentials, post);
	        post.setEntity(new StringEntity("{}"));
	        HttpResponse resp = httpclient.execute(post);
	        AttachmentProcess attachProc = new AttachmentProcess(this);
	        attachProc.completed(resp);
	        
	        listen(callback);
	        
	    } catch (Exception e) {
	        log.severe("Error occurred");
	        e.printStackTrace();
	    }		
	}

	@Override
	public void setChannel(UUID uuid) {
		this.channel = uuid;
	}

    
}
