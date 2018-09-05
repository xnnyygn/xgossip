package in.xnnyygn.xgossip;

public class MemberNotification {

    private final MemberEndpoint endpoint;
    private final boolean suspected;
    private final long timestamp;
    private final MemberEndpoint by;

    public MemberNotification(MemberEndpoint endpoint, boolean suspected, long timestamp, MemberEndpoint by) {
        this.endpoint = endpoint;
        this.suspected = suspected;
        this.timestamp = timestamp;
        this.by = by;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    public boolean isSuspected() {
        return suspected;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MemberEndpoint getBy() {
        return by;
    }

}
