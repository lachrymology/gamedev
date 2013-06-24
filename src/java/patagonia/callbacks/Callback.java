package patagonia.callbacks;

import java.io.InputStream;

import org.apache.http.HttpResponse;

public abstract class Callback {
    public void completed(InputStream inputStream) {
    	throw new UnsupportedOperationException();
    }
    
    public void completed(HttpResponse response) {
    	throw new UnsupportedOperationException();
    }
    
    public void failed(Exception e) {
    	throw new UnsupportedOperationException();
    }
    
    public void cancelled() {
    	throw new UnsupportedOperationException();
    }
}
