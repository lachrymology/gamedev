package patagonia;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import patagonia.callbacks.Callback;
import patagonia.edn.Keyword;
import patagonia.edn.printer.Printers;
import patagonia.errors.PatagoniaException;
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
        this.params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
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
    
    
    public void listen(Callback callback) {
    	Map<Keyword,Object> packet = Util.buildContextPacket(this.channel, this.credentials);
    	String message = Printers.printString(packet);
    	
    	listen("long_poll", "POST", new ByteArrayInputStream(message.getBytes()), callback);
    }

	public void attach() {
		try {
	        String url = "http://" + this.host + ":" + this.port + "/context/new/nga";
	        
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpPost post = new HttpPost(url);
	        Util.cookieDecoration(this.credentials, post);
	        post.setEntity(new StringEntity("{}"));
	        HttpResponse resp = httpclient.execute(post);
	        AttachmentProcess attachProc = new AttachmentProcess(this);
	        attachProc.completed(resp);
	        
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
