package in.xnnyygn.xgossip;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@ThreadSafe
public class LatencyRecorder {

    private final ConcurrentMap<MemberEndpoint, Record> records = new ConcurrentHashMap<>();

    public void add(MemberEndpoint endpoint, long pingAt, long latency) {
        records.put(endpoint, new Record(endpoint, pingAt, latency));
    }

    public Set<MemberEndpoint> listEndpointWithFailedPing() {
        return records.values().stream()
                .filter(Record::isFailed)
                .map(Record::getEndpoint)
                .collect(Collectors.toSet());
    }

    public List<RankingItem> getRanking() {
        return records.values().stream()
                .filter(r -> !r.isFailed())
                .sorted()
                .map(r -> new RankingItem(r.getEndpoint(), r.getLatency()))
                .collect(Collectors.toList());
    }

    public static class RankingItem {

        private final MemberEndpoint endpoint;
        private final long latency;

        RankingItem(MemberEndpoint endpoint, long latency) {
            this.endpoint = endpoint;
            this.latency = latency;
        }

        public MemberEndpoint getEndpoint() {
            return endpoint;
        }

        public long getLatency() {
            return latency;
        }

    }

    private static class Record implements Comparable<Record> {

        private final MemberEndpoint endpoint;
        private final long pingAt;
        private final long latency;

        Record(MemberEndpoint endpoint, long pingAt, long latency) {
            this.endpoint = endpoint;
            this.pingAt = pingAt;
            this.latency = latency;
        }

        MemberEndpoint getEndpoint() {
            return endpoint;
        }

        long getPingAt() {
            return pingAt;
        }

        long getLatency() {
            return latency;
        }

        boolean isFailed() {
            return latency < 0;
        }

        @Override
        public int compareTo(@Nonnull Record o) {
            return Long.compare(latency, o.latency);
        }

    }

}
