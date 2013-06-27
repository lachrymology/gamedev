package patagonia.callbacks;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;

import patagonia.Util;


public class NoopCallback extends Callback {
	public void completed(InputStream is) {
		try {
			String body = Util.slurp(is);
			System.out.println("Successfully completed the task: " + body);
		} catch (IOException ioe) {
			this.failed(ioe);
		}
	}

	@Override
	public void completed(HttpResponse response) {
		try {
			this.completed(response.getEntity().getContent());
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void failed(Exception e) {
		System.out.println("Failed to send the message");
    }
 
	public void cancelled() {
		System.out.println("Cancelled the request");
    }
}
