package fogus.patagonia.callbacks;

import java.io.InputStream;

import fogus.patagonia.Callback;

public class NoopCallback implements Callback {
	public void completed(InputStream body) {
		System.out.println("Successfully completed the task.");
	}
 
	public void failed(Exception e) {
		System.out.println("Failed to send the message");
    }
 
	public void cancelled() {
		System.out.println("Cancelled the request");
    }
}
