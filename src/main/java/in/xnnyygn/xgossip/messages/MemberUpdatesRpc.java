package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.updates.AbstractUpdate;

import java.util.*;

public class MemberUpdatesRpc extends AbstractMessage {

    private final List<AbstractUpdate> updates;
    private final byte[] membersDigest;

    public MemberUpdatesRpc(List<AbstractUpdate> updates, byte[] membersDigest) {
        this.updates = updates;
        this.membersDigest = membersDigest;
    }

    public List<AbstractUpdate> getUpdates() {
        return updates;
    }

    public byte[] getMembersDigest() {
        return membersDigest;
    }

    public boolean hasUpdate() {
        return !updates.isEmpty();
    }

    public Map<Long, Boolean> makeUpdateIdMap() {
        Map<Long, Boolean> map = new HashMap<>();
        for (AbstractUpdate update : updates) {
            map.put(update.getId(), true);
        }
        return map;
    }

    public long getExchangeAt() {
        return getTimestamp();
    }

    @Override
    public String toString() {
        return "MemberUpdatesRpc{" +
                "exchangeAt=" + getExchangeAt() +
                ", updates.size=" + updates.size() +
                ", membersDigest=" + Base64.getEncoder().encodeToString(membersDigest) +
                '}';
    }

}
