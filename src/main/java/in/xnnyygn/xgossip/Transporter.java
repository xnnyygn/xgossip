package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.messages.AbstractMessage;
import in.xnnyygn.xgossip.messages.RemoteMessage;

public interface Transporter {

    void send(MemberEndpoint endpoint, AbstractMessage message);

    <T extends AbstractMessage> void reply(RemoteMessage<T> remoteMessage, AbstractMessage response);

}
