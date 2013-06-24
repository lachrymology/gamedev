package patagonia.callbacks;

import java.io.InputStream;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;


public class LoginCallback implements Callback {
    private Map<String, String> credentials;
	
	public LoginCallback(final Map<String, String> credentials) {
        this.credentials = credentials;
	}

	@Override
	public void completed(HttpResponse response) {
		for (Header header : response.getHeaders("Set-Cookie")) {
			String[] val = header.getValue().split("=");

			//TODO stronger checks here
			credentials.put(val[0], val[1]);
		}		
	}

	@Override
	public void failed(Exception e) {
	}

	@Override
	public void cancelled() {
	}

	@Override
	public void completed(InputStream inputStream) {
	}
}
