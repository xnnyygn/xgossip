package in.xnnyygn.xgossip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ThreadSafe
public class NotificationList {

    private static final Logger logger = LoggerFactory.getLogger(NotificationList.class);
    private final int countThreshold;
    private final Map<MemberEndpoint, MemberState> memberStateMap = new HashMap<>();

    public NotificationList(int countThreshold) {
        this.countThreshold = countThreshold;
    }

    public synchronized void suspectMember(MemberEndpoint endpoint, long timestamp, MemberEndpoint by) {
        MemberState state = memberStateMap.get(endpoint);
        if (state == null) {
            memberStateMap.put(endpoint, new MemberState(endpoint, timestamp, by));
            return;
        }
        state.suspect(timestamp, by);
    }

    public synchronized void trustMember(MemberEndpoint endpoint, long timestamp, MemberEndpoint by) {
        MemberState state = memberStateMap.get(endpoint);
        if (state == null) {
            return;
        }
        state.trust(timestamp, by);
    }

    public synchronized List<MemberNotification> take(int n) {
        List<MemberState> states = memberStateMap.values().stream().limit(n).collect(Collectors.toList());
        List<MemberNotification> result = new ArrayList<>();
        for (MemberState state : states) {
            result.add(state.toNotification());
            if (state.increaseAndGetCount() >= countThreshold) {
                logger.debug("remove notification for {}", state.getEndpoint());
                memberStateMap.remove(state.getEndpoint());
            }
        }
        return result;
    }

    private static class MemberState {

        private final MemberEndpoint endpoint;
        private boolean suspected = true;
        private long timestamp;
        private MemberEndpoint by;

        private int count = 0;

        MemberState(MemberEndpoint endpoint, long timestamp, MemberEndpoint by) {
            this.endpoint = endpoint;
            this.timestamp = timestamp;
            this.by = by;
        }

        MemberEndpoint getEndpoint() {
            return endpoint;
        }

        MemberNotification toNotification() {
            return new MemberNotification(endpoint, suspected, timestamp, by);
        }

        void suspect(long timestamp, MemberEndpoint by) {
            if (suspected) {
                return;
            }
            suspected = true;
            this.timestamp = timestamp;
            this.by = by;
        }

        void trust(long timestamp, MemberEndpoint by) {
            if (!suspected) {
                return;
            }
            suspected = false;
            this.timestamp = timestamp;
            this.by = by;
        }

        int increaseAndGetCount() {
            return ++count;
        }

    }

}
