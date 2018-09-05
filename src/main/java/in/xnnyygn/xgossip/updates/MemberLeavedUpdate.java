package in.xnnyygn.xgossip.updates;

import in.xnnyygn.xgossip.MemberEndpoint;

public class MemberLeavedUpdate extends AbstractUpdate {

    private final MemberEndpoint endpoint;
    private final long timeLeaved;

    public MemberLeavedUpdate(long id, MemberEndpoint endpoint, long timeLeaved) {
        super(id);
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
        return "MemberLeavedUpdate{" +
                "endpoint=" + endpoint +
                ", timeLeaved=" + timeLeaved +
                '}';
    }

}
