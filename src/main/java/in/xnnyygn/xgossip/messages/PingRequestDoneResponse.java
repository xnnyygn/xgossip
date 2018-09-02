package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class PingRequestDoneResponse extends AbstractMessage {

    private final MemberEndpoint endpoint;

    public PingRequestDoneResponse(MemberEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

}
