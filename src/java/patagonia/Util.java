package patagonia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import patagonia.edn.Keyword;

public class Util {
	public static String slurp(InputStream in) throws IOException {
    	InputStreamReader is = new InputStreamReader(in);
    	StringBuilder sb = new StringBuilder();
    	BufferedReader br = new BufferedReader(is);
    	String read = br.readLine();

    	while(read != null) {
    	    sb.append(read);
    	    read = br.readLine();
    	}

    	return sb.toString();
    }
	
	public static String[] parseCookie(Header cook) {
		return cook.getValue().split("=");
	}

	public static String pickleCookies(Map<String,String> credentials) {
		// TODO: More robustness please
		return "session-id="  + credentials.get("session-id") + ";" +
			   "session-key=" + credentials.get("session-key");
	}
	
	public static void cookieDecoration(Map<String,String> credentials, HttpRequest request) {
        if (credentials.get("session-id") != null) {
        	String cook = Util.pickleCookies(credentials);
        	request.setHeader("Cookie", cook);
        }
	}

	public static Keyword kw(String str) {
		return Keyword.newKeyword(str);
	}
	
	@SuppressWarnings("serial")
	public static HashMap<Keyword, Object> buildMessagePacket(final UUID channel, final Map<String, String> credentials) {
		return new HashMap<Keyword,Object>() {{
			put(kw("version"), "0.1");
			put(kw("tag"), kw("push"));
			put(kw("context-id"), channel);
			put(kw("session-id"), UUID.fromString(credentials.get("session-id")));
		}};
	} 
}
