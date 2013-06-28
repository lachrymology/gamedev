package patagonia.processes;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;

import patagonia.Client;
import patagonia.IClient;
import patagonia.Util;
import patagonia.callbacks.Callback;
import patagonia.edn.parser.Parseable;
import patagonia.edn.parser.Parser;
import patagonia.edn.parser.Parsers;

public class AttachmentProcess extends Callback {
    private IClient client;
    
	public AttachmentProcess(IClient client) {
		this.client = client;
	}
	
	public void completed(InputStream inputStream) {
    	try {
    		String str = Util.slurp(inputStream);
			Parseable pbr = Parsers.newParseable(str);
			Parser p = Parsers.newParser(Parsers.defaultConfiguration());
			List uuids = (List) p.nextValue(pbr);
			
			this.client.setChannel((UUID) uuids.get(0));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
    public void completed(HttpResponse response) {
    	try {
			this.completed(response.getEntity().getContent());
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	public static String url(String proto, String host, int port) {
		return proto + "://" + host + ":" + port + "/hi";
	}

}
