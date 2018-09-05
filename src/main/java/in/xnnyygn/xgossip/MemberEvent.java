package in.xnnyygn.xgossip;

public class MemberEvent {

    enum Kind {
        JOINED,
        SUSPECTED,
        BACKED,
        LEAVED
    }

    private final MemberEndpoint endpoint;
    private final Kind kind;

    public MemberEvent(MemberEndpoint endpoint, Kind kind) {
        this.endpoint = endpoint;
        this.kind = kind;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "MemberEvent{" +
                "endpoint=" + endpoint +
                ", kind=" + kind +
                '}';
    }

}
