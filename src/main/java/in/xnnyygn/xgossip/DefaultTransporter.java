package in.xnnyygn.xgossip;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xgossip.messages.AbstractMessage;
import in.xnnyygn.xgossip.messages.RemoteMessage;

public class DefaultTransporter implements Transporter {

    private final EventBus eventBus;

    public DefaultTransporter(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void send(MemberEndpoint endpoint, AbstractMessage message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends AbstractMessage> void reply(RemoteMessage<T> remoteMessage, AbstractMessage response) {
        throw new UnsupportedOperationException();
    }

}
