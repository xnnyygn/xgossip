package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.messages.AbstractMessage;
import in.xnnyygn.xgossip.messages.RemoteMessage;
import in.xnnyygn.xgossip.rpc.Transporter;

import java.util.ArrayList;
import java.util.List;

public class MockTransporter implements Transporter {

    private final List<Message> messages = new ArrayList<>();

    @Override
    public void initialize() {
    }

    @Override
    public <T extends AbstractMessage> void send(MemberEndpoint endpoint, T message) {
        messages.add(new Message(endpoint, message));
    }

    @Override
    public <M extends AbstractMessage, R extends AbstractMessage> void reply(RemoteMessage<M> remoteMessage, R response) {
        messages.add(new Message(remoteMessage.getSender(), response));
    }

    @Override
    public void close() {
    }

    public List<Message> getMessages() {
        return messages;
    }

    public static class Message {

        private final MemberEndpoint recipient;
        private final AbstractMessage payload;

        Message(MemberEndpoint recipient, AbstractMessage payload) {
            this.recipient = recipient;
            this.payload = payload;
        }

        public MemberEndpoint getRecipient() {
            return recipient;
        }

        public AbstractMessage getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "payload=" + payload +
                    ", recipient=" + recipient +
                    '}';
        }

    }

}
