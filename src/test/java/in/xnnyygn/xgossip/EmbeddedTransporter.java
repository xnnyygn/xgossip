package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.messages.AbstractMessage;
import in.xnnyygn.xgossip.rpc.messages.RemoteMessage;
import in.xnnyygn.xgossip.rpc.Transporter;
import in.xnnyygn.xgossip.support.MessageDispatcher;

import java.util.Map;

public class EmbeddedTransporter implements Transporter {

    private final Map<MemberEndpoint, EmbeddedTransporter> transporterMap;
    private final MemberEndpoint selfEndpoint;
    private final MessageDispatcher messageDispatcher;
    private final MessageCollector messageCollector;


    public EmbeddedTransporter(Map<MemberEndpoint, EmbeddedTransporter> transporterMap, MemberEndpoint selfEndpoint,
                               MessageDispatcher messageDispatcher, MessageCollector messageCollector) {
        this.transporterMap = transporterMap;
        this.selfEndpoint = selfEndpoint;
        this.messageDispatcher = messageDispatcher;
        this.messageCollector = messageCollector;
    }

    @Override
    public void initialize() {
    }

    @Override
    public <T extends AbstractMessage> void send(MemberEndpoint endpoint, T message) {
        EmbeddedTransporter transporter = transporterMap.get(endpoint);
        if (transporter == null) {
            throw new IllegalStateException("no transporter for endpoint " + endpoint);
        }
        messageCollector.add(selfEndpoint, endpoint, message);
        transporter.messageDispatcher.post(new RemoteMessage<>(message, selfEndpoint));
    }

    @Override
    public <M extends AbstractMessage, R extends AbstractMessage> void reply(RemoteMessage<M> remoteMessage, R response) {
        send(remoteMessage.getSender(), response);
    }

    @Override
    public void close() {
    }

}
