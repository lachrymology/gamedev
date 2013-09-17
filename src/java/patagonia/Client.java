package patagonia;


import patagonia.callbacks.Callback;
import patagonia.callbacks.NoopCallback;
import patagonia.edn.Keyword;
import patagonia.edn.printer.Printers;
import patagonia.errors.PatagoniaException;
import patagonia.http.HttpHost;
import patagonia.http.HttpRequestInterceptor;
import patagonia.http.HttpResponse;
import patagonia.http.client.HttpClient;
import patagonia.http.client.methods.HttpGet;
import patagonia.http.client.methods.HttpPost;
import patagonia.http.concurrent.FutureCallback;
import patagonia.http.entity.InputStreamEntity;
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
import patagonia.processes.LoginProcess;

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


public class Client implements IClient {
	private static Logger log = Logger.getLogger(Client.class.toString());
	
	private String host;
	private int port;
	private String path;
	private volatile Map<String,String> credentials = new HashMap<String,String>();
	
	public Map<String, String> getCredentials() {
		return credentials;
	}

	private volatile UUID channel;
	
	public UUID getChannel() {
		return channel;
	}

	private ConnectingIOReactor ioReactor;
    private HttpProcessor httpproc;
    private HttpParams params;
    private BasicNIOConnPool pool;
    private LongPollClient longpoll;

	private String context;
	
    public Client(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
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
    
    private void sendTo(String endpoint, String method, final InputStream inputStream, final Callback callBack) {
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

    public void destroy() throws IOException {
    	ioReactor.shutdown();
    }
    
    public void login(String name, String email) {
        try {
            String url = LoginProcess.url("http", this.host, this.port, name, email);
            
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost post = new HttpPost(url);
            HttpResponse resp = httpclient.execute(post);
            LoginProcess loginProc = new LoginProcess(name, email, this);
            loginProc.completed(resp);
            
        } catch (Exception e) {
            log.severe("Error occurred");
            e.printStackTrace();
        }
    }
    
    private void listen(String context, Callback callback) {
        this.longpoll = new LongPollClient(this.host, this.port, "/", this.credentials);
        longpoll.attach(context, callback);
    }
    
    public void attach(String context, Callback callback) {
        try {
        	this.context = context;
            String url = AttachmentProcess.url("http", this.host, this.port);
            
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            Util.cookieDecoration(this.credentials, get);
            HttpResponse resp = httpclient.execute(get);
            AttachmentProcess attachProc = new AttachmentProcess(this);
            attachProc.completed(resp);
            
            listen(this.context, callback);
            
        } catch (Exception e) {
            log.severe("Error occurred");
            e.printStackTrace();
        }
    }

    private void say(String endpoint, String method, String message, final Callback callBack) {
    	sendTo(endpoint, method, new ByteArrayInputStream(message.getBytes()), callBack);
    }
    
    public void send(String top, Map<String,Object>... parameters) {
    	Keyword topic = Keyword.newKeyword(top);
    	Map<Keyword,Object> packet = Util.buildMessagePacket(this.channel, this.credentials);
    	List<Map<String,Object>> messages = new ArrayList<Map<String,Object>>(Arrays.asList(parameters));
    	
    	packet.put(topic, messages);
    	
    	say("sink", "POST", Printers.printString(packet), new NoopCallback());
    }

    public void send(String top, UUID trace, Map<String,Object>... parameters) {
    	Keyword topic = Keyword.newKeyword(top);
    	Map<Keyword,Object> packet = Util.buildMessagePacket(this.channel, this.credentials);
    	List<Map<String,Object>> messages = new ArrayList<Map<String,Object>>(Arrays.asList(parameters));
    	
    	packet.put(topic, messages);
    	
    	if (trace != null) {
    		packet.put(Util.kw("patagonia/trace"), trace);
    	}
    	
    	say("sink", "POST", Printers.printString(packet), new NoopCallback());
    }
    
	public static void main(String[] args) {
		Client client = new Client("localhost", 8080, "/");
        try {
			client.init();
		} catch (PatagoniaException e) {
			e.printStackTrace();
		}
        
        client.login("fogus", "mfogus@d-a-s.com");
        client.attach("nga", new NoopCallback());
        
        client.sendTestMessages(client);
	}

	@SuppressWarnings({ "unchecked", "serial" })
	private void sendTestMessages(Client client) {
		client.send("dsr/capabilities",
  			  new HashMap<String,Object>() {{
  			      put("provider/id", 42);
  			      put("provider/peer", "http://localhost:8081");
  			      put("provider/descr", "Foo bar baz.");
  			  }},
  			  new HashMap<String,Object>() {{
  			      put("capability/name", "test");
  			      put("capability/provided", Boolean.TRUE);
  			      put("capability/provider", "test-service");
  			  }});
		
	}

	public void setChannel(UUID uuid) {
		this.channel = uuid;
	}

	public synchronized void addCredential(String k, String v) {
		this.credentials.put(k, v);
	}
}
