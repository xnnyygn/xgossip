package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class PingRequestRpc extends AbstractMessage {

    private final MemberEndpoint endpoint;

    public PingRequestRpc(MemberEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

}
