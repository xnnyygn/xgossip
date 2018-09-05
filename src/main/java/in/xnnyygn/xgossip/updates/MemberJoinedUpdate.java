package in.xnnyygn.xgossip.updates;

import in.xnnyygn.xgossip.Member;
import in.xnnyygn.xgossip.MemberEndpoint;

public class MemberJoinedUpdate extends AbstractUpdate {

    private final MemberEndpoint endpoint;
    private final long timeJoined;

    public MemberJoinedUpdate(long id, MemberEndpoint endpoint, long timeJoined) {
        super(id);
        this.endpoint = endpoint;
        this.timeJoined = timeJoined;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    public long getTimeJoined() {
        return timeJoined;
    }

    @Override
    public String toString() {
        return "MemberJoinedUpdate{" +
                "endpoint=" + endpoint +
                ", timeJoined=" + timeJoined +
                '}';
    }

}
