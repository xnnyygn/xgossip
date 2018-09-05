package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.messages.AbstractMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageCollector {

    private final List<Message> messages = new ArrayList<>();

    public void add(MemberEndpoint sender, MemberEndpoint recipient, AbstractMessage payload) {
        messages.add(new Message(sender, recipient, payload));
    }

    public List<Message> getMessages() {
        return messages;
    }

    public static class Message {

        private final MemberEndpoint sender;
        private final MemberEndpoint recipient;
        private final AbstractMessage payload;

        Message(MemberEndpoint sender, MemberEndpoint recipient, AbstractMessage payload) {
            this.sender = sender;
            this.recipient = recipient;
            this.payload = payload;
        }

        public MemberEndpoint getSender() {
            return sender;
        }

        public MemberEndpoint getRecipient() {
            return recipient;
        }

        public AbstractMessage getPayload() {
            return payload;
        }

    }

}
