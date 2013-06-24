package patagonia.edn.parser;

/**
 * A Handler for {@code #inst} which translates the intant into a
 * {@link java.util.Date}.
 */
public final class InstantToDate extends AbstractInstantHandler {

    @Override
    protected Object transform(ParsedInstant pi) {
        return InstantUtils.makeDate(pi);
    }

}
