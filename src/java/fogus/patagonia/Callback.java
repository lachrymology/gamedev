package fogus.patagonia;

import java.io.InputStream;

public interface Callback {
    void completed(InputStream inputStream);
    void failed(Exception e);
    void cancelled();
}
