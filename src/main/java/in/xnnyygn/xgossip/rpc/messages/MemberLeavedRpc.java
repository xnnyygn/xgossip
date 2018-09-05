package in.xnnyygn.xgossip.rpc.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class MemberLeavedRpc extends AbstractMessage {

    private final MemberEndpoint endpoint;
    private final long timeLeaved;

    public MemberLeavedRpc(MemberEndpoint endpoint, long timeLeaved) {
        this.endpoint = endpoint;
        this.timeLeaved = timeLeaved;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    public long getTimeLeaved() {
        return timeLeaved;
    }

    @Override
    public String toString() {
        return "MemberLeavedRpc{" +
                "endpoint=" + endpoint +
                ", timeLeaved=" + timeLeaved +
                '}';
    }

}
