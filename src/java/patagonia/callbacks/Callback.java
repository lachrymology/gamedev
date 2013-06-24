package patagonia.callbacks;

import java.io.InputStream;

import org.apache.http.HttpResponse;

public interface Callback {
    void completed(InputStream inputStream);
    void completed(HttpResponse response);
    void failed(Exception e);
    void cancelled();
}
