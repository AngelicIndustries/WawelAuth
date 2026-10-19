package industries.angelic.wawelauth.wawelnet;

/**
 * Handles a single HTTP route.
 * <ul>
 * <li>Return {@code null} &rarr; 204 No Content</li>
 * <li>Return {@link BinaryResponse} &rarr; raw bytes with custom content type</li>
 * <li>Return any other Object &rarr; serialized as JSON via GSON</li>
 * </ul>
 */
@FunctionalInterface
public interface RouteHandler {

    Object handle(RequestContext ctx) throws Exception;
}
