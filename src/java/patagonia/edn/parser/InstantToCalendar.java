package patagonia.edn.parser;

/**
 * A Handler for {@code #inst} which translates the intant into a
 * {@link java.util.Calendar}.
 */
public class InstantToCalendar extends AbstractInstantHandler {

    @Override
    protected Object transform(ParsedInstant pi) {
        return InstantUtils.makeCalendar(pi);
    }

}
