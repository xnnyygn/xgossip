package in.xnnyygn.xgossip;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@ThreadSafe
public class LatencyRecorder {

    private final ConcurrentMap<MemberEndpoint, Record> records = new ConcurrentHashMap<>();

    public void add(MemberEndpoint endpoint, long lastPing, long latency) {
        records.put(endpoint, new Record(endpoint, lastPing, latency));
    }

    public Set<MemberEndpoint> listSuspectedEndpoints() {
        return records.values().stream()
                .filter(r -> r.getLatency() < 0)
                .map(Record::getEndpoint)
                .collect(Collectors.toSet());
    }

    private static class Record {

        private final MemberEndpoint endpoint;
        private final long lastPing;
        private final long latency;

        Record(MemberEndpoint endpoint, long lastPing, long latency) {
            this.endpoint = endpoint;
            this.lastPing = lastPing;
            this.latency = latency;
        }

        MemberEndpoint getEndpoint() {
            return endpoint;
        }

        long getLastPing() {
            return lastPing;
        }

        long getLatency() {
            return latency;
        }

    }

}
