package in.xnnyygn.xgossip.support;

import in.xnnyygn.xgossip.rpc.messages.AbstractMessage;
import in.xnnyygn.xgossip.rpc.messages.RemoteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);
    private final Map<Class<? extends AbstractMessage>, Handler<? extends AbstractMessage>> handlerMap = new HashMap<>();

    public <T extends AbstractMessage> void register(Class<T> clazz, Handler<T> handler) {
        handlerMap.put(clazz, handler);
    }

    /**
     * Remember to register handler before any invoking of post.
     *
     * @param message message
     * @param <T>     payload type
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> void post(RemoteMessage<T> message) {
        Class<? extends AbstractMessage> payloadClass = message.getPayloadClass();
        Handler<T> handler = (Handler<T>) handlerMap.get(payloadClass);
        if (handler == null) {
            logger.warn("no handler for remote message, class {}", payloadClass);
            return;
        }
        handler.handle(message);
    }

    public interface Handler<T extends AbstractMessage> {

        void handle(RemoteMessage<T> message);

    }

}
