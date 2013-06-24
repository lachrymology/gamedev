package patagonia.callbacks;

import java.io.InputStream;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import patagonia.Util;


public class LoginCallback extends Callback {
	private String name;
	private String email;
    private Map<String, String> credentials;
	
	public LoginCallback(String name, String email, final Map<String, String> credentials) {
		// TODO stringer checks here
		this.name = name;
		this.email = email;
        this.credentials = credentials;
	}

	@Override
	public void completed(HttpResponse response) {
		for (Header header : response.getHeaders("Set-Cookie")) {
			String[] val = Util.parseCookie(header);

			//TODO stronger checks here
			this.credentials.put(val[0], val[1]);
		}
		
		this.credentials.put("name",  this.name);
		this.credentials.put("email", this.email);
	}
}
