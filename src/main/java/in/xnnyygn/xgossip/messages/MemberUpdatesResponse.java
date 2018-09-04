package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.updates.AbstractUpdate;

import java.util.*;

public class MemberUpdatesResponse extends AbstractMessage {

    private final long exchangeAt;
    private final Map<Long, Boolean> updatedMap;
    private final List<AbstractUpdate> updates;
    // TODO add notifications
    private final byte[] membersDigest;
    private final int hopCount;

    public MemberUpdatesResponse(long exchangeAt, Map<Long, Boolean> updatedMap, List<AbstractUpdate> updates, byte[] membersDigest) {
        this(exchangeAt, updatedMap, updates, membersDigest, 1);
    }

    public MemberUpdatesResponse(long exchangeAt, Map<Long, Boolean> updatedMap, List<AbstractUpdate> updates, byte[] membersDigest, int hopCount) {
        this.exchangeAt = exchangeAt;
        this.updatedMap = updatedMap;
        this.updates = updates;
        this.membersDigest = membersDigest;
        this.hopCount = hopCount;
    }

    public long getExchangeAt() {
        return exchangeAt;
    }

    public Map<Long, Boolean> getUpdatedMap() {
        return updatedMap;
    }

    public Set<Long> getUpdateIds() {
        return updatedMap.keySet();
    }

    public List<AbstractUpdate> getUpdates() {
        return updates;
    }

    public byte[] getMembersDigest() {
        return membersDigest;
    }

    public int getHopCount() {
        return hopCount;
    }

    @Override
    public String toString() {
        return "MemberUpdatesResponse{" +
                "exchangeAt=" + exchangeAt +
                ", updatedMap=" + updatedMap +
                ", updates.size=" + updates.size() +
                ", membersDigest=" + Base64.getEncoder().encodeToString(membersDigest) +
                ", hopCount=" + hopCount +
                '}';
    }

}
