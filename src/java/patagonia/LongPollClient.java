package patagonia;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;

public class LongPollClient {
	private static Logger log = Logger.getLogger(Client.class.toString());
	
	private String host;
	private int port;
	private String path;
	private volatile Map<String,String> credentials = new HashMap<String,String>();
	private volatile UUID channel;
	
	private ConnectingIOReactor ioReactor;
    private HttpProcessor httpproc;
    private HttpParams params;
    private BasicNIOConnPool pool;
	
    public LongPollClient(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
    }
}
