package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class RemoteMessage<T extends AbstractMessage> {

    private final T message;
    private final MemberEndpoint sender;

    public RemoteMessage(T message, MemberEndpoint sender) {
        this.message = message;
        this.sender = sender;
    }

    public T get() {
        return message;
    }

    public MemberEndpoint getSender() {
        return sender;
    }
}
