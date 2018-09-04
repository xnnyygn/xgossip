package in.xnnyygn.xgossip.rpc;

import in.xnnyygn.xgossip.MemberEndpoint;
import in.xnnyygn.xgossip.messages.AbstractMessage;
import in.xnnyygn.xgossip.messages.RemoteMessage;

public interface Transporter {

    void initialize();

    <T extends AbstractMessage> void send(MemberEndpoint endpoint, T message);

    <M extends AbstractMessage, R extends AbstractMessage> void reply(RemoteMessage<M> remoteMessage, R response);

    void close();

}
