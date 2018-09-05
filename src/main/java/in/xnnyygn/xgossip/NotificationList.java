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
    private final int threshold;
    private final Map<MemberEndpoint, MemberState> memberStateMap = new HashMap<>();

    public NotificationList(int threshold) {
        this.threshold = threshold;
    }

    public synchronized void suspectMember(MemberEndpoint endpoint, long timestamp, MemberEndpoint by) {
        logger.info("suspect member {} by {}", endpoint, by);
        MemberState state = memberStateMap.get(endpoint);
        if (state == null) {
            memberStateMap.put(endpoint, new MemberState(endpoint, timestamp, by));
        } else {
            state.suspect(timestamp, by);
        }
    }

    public synchronized void trustMember(MemberEndpoint endpoint, long timestamp, MemberEndpoint by) {
        MemberState state = memberStateMap.get(endpoint);
        if (state == null) {
            return;
        }
        logger.info("trust member {} by {}", endpoint, by);
        state.trust(timestamp, by);
    }

    public synchronized List<MemberNotification> take(int n) {
        List<MemberState> states = memberStateMap.values().stream().limit(n).collect(Collectors.toList());
        List<MemberNotification> result = new ArrayList<>();
        for (MemberState state : states) {
            result.add(state.toNotification());
            if (state.increaseAndGetCount() >= threshold) {
                logger.debug("remove notification of {}", state.getEndpoint());
                memberStateMap.remove(state.getEndpoint());
            }
        }
        return result;
    }

    public void add(MemberNotification notification) {
        if (notification.isSuspected()) {
            suspectMember(notification.getEndpoint(), notification.getTimestamp(), notification.getBy());
        } else {
            trustMember(notification.getEndpoint(), notification.getTimestamp(), notification.getBy());
        }
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
