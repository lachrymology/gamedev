package patagonia.callbacks;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import patagonia.Client;
import patagonia.Util;


public class LoginCallback extends Callback {
	private String name;
	private String email;
    private Client client;
	
	public LoginCallback(String name, String email, final Client client) {
		// TODO stringer checks here
		this.name = name;
		this.email = email;
        this.client = client;
	}

	@Override
	public void completed(HttpResponse response) {
		for (Header header : response.getHeaders("Set-Cookie")) {
			String[] val = Util.parseCookie(header);

			//TODO stronger checks here
			this.client.addCredential(val[0], val[1]);
		}
		
		this.client.addCredential("name",  this.name);
		this.client.addCredential("email", this.email);
	}
}
