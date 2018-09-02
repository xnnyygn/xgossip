package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.messages.AbstractMessage;
import in.xnnyygn.xgossip.messages.RemoteMessage;

import java.util.ArrayList;
import java.util.List;

public class MockTransporter implements Transporter {

    private final List<Message> messages = new ArrayList<>();

    @Override
    public void send(MemberEndpoint endpoint, AbstractMessage message) {
        messages.add(new Message(endpoint, message));
    }

    @Override
    public <T extends AbstractMessage> void reply(RemoteMessage<T> remoteMessage, AbstractMessage response) {
        messages.add(new Message(remoteMessage.getSender(), response));
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

    }

}
