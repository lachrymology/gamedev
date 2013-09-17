package patagonia.processes;


import patagonia.Client;
import patagonia.Util;
import patagonia.callbacks.Callback;
import patagonia.http.Header;
import patagonia.http.HttpResponse;


public class LoginProcess extends Callback {
	private String name;
	private String email;
    private Client client;
    
    public static String url(String proto, String host, int port, String name, String email) {
    	return proto + "://" + host + ":" + port + "/" + "login?name=" + name + "&email=" + email;
    }
	
	public LoginProcess(String name, String email, final Client client) {
		// TODO stronger checks here
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
