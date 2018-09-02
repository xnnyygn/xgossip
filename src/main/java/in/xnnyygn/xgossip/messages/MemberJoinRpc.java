package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class MemberJoinRpc extends AbstractMessage {

    private final MemberEndpoint endpoint;

    public MemberJoinRpc(MemberEndpoint endpoint) {
        super();
        this.endpoint = endpoint;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

}
