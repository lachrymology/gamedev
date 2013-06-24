package patagonia.callbacks;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;

import patagonia.Client;
import patagonia.Util;
import patagonia.edn.parser.Parseable;
import patagonia.edn.parser.Parser;
import patagonia.edn.parser.Parsers;

public class AttachmentCallback extends Callback {
    private Client client;
    
	public AttachmentCallback(Client client) {
		this.client = client;
	}
	
	public void completed(InputStream inputStream) {
    	try {
    		String str = Util.slurp(inputStream);
			Parseable pbr = Parsers.newParseable(str);
			Parser p = Parsers.newParser(Parsers.defaultConfiguration());
			Object thing = p.nextValue(pbr);
			
			System.out.println("GOT: " + thing.getClass().getName());
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

}
