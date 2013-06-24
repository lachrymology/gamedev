package fogus.patagonia.callbacks;

import java.io.IOException;
import java.io.InputStream;

import fogus.patagonia.Callback;
import fogus.patagonia.Util;

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
}
