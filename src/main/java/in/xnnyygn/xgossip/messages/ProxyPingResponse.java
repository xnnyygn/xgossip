package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class ProxyPingResponse extends AbstractMessage {

    private final MemberEndpoint endpoint;

    public ProxyPingResponse(MemberEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

}
