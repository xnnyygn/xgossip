package in.xnnyygn.xgossip.updates;

import in.xnnyygn.xgossip.MemberEndpoint;

public class MemberSuspectedNotification extends AbstractUpdate {

    private final MemberEndpoint endpoint;

    public MemberSuspectedNotification(MemberEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean shouldFeedback() {
        return false;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String toString() {
        return "MemberSuspectedNotification{" +
                "endpoint=" + endpoint +
                '}';
    }

}
