package patagonia;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.InputStreamEntity;
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
import patagonia.callbacks.NoopCallback;
import patagonia.edn.Keyword;
import patagonia.edn.printer.Printers;
import patagonia.errors.PatagoniaException;
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
	
	private ConnectingIOReactor ioReactor;
    private HttpProcessor httpproc;
    private HttpParams params;
    private BasicNIOConnPool pool;
    private LongPollClient longpoll;
	
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
    
    private void listen(Callback callback) {
        this.longpoll = new LongPollClient("localhost", 8080, "/", this.credentials);
        longpoll.attach();
        longpoll.listen(callback);
    }
    
    public void attach(Callback callback) {
        try {
            String url = AttachmentProcess.url("http", this.host, this.port);
            
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            Util.cookieDecoration(this.credentials, get);
            HttpResponse resp = httpclient.execute(get);
            AttachmentProcess attachProc = new AttachmentProcess(this);
            attachProc.completed(resp);
            
            listen(callback);
            
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
    	
    	say("async", "POST", Printers.printString(packet), new NoopCallback());
    }
    
	public static void main(String[] args) {
		Client client = new Client("localhost", 8080, "/");
        try {
			client.init();
		} catch (PatagoniaException e) {
			e.printStackTrace();
		}
        
        client.login("fogus", "mfogus@d-a-s.com");
        client.attach(new NoopCallback());
        
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
