package patagonia.edn.parser;

import java.util.UUID;

import patagonia.edn.EdnSyntaxException;
import patagonia.edn.Tag;


class UuidHandler implements TagHandler {

    public Object transform(Tag tag, Object value) {
        if (!(value instanceof String)) {
             throw new EdnSyntaxException(tag.toString() +
                                          " expectes a String.");
        }
        return UUID.fromString((String) value);
    }

}
