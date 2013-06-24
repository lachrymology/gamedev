package patagonia.callbacks;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;

import patagonia.Util;


public class NoopCallback implements Callback {
	public void completed(InputStream is) {
		try {
			String body = Util.slurp(is);
			System.out.println("Successfully completed the task: " + body);
		} catch (IOException ioe) {
			this.failed(ioe);
		}
	}
 
	public void failed(Exception e) {
		System.out.println("Failed to send the message");
    }
 
	public void cancelled() {
		System.out.println("Cancelled the request");
    }

	@Override
	public void completed(HttpResponse response) {
		try {
			this.completed(response.getEntity().getContent());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}
	}
}
