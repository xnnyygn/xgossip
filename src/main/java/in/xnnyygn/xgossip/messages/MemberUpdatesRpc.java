package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.updates.AbstractUpdate;

import java.util.*;

public class MemberUpdatesRpc extends AbstractMessage {

    private final long exchangeAt;
    private final List<AbstractUpdate> updates = new ArrayList<>();
    private final List<AbstractUpdate> notifications = new ArrayList<>();
    private final byte[] membersDigest;

    public MemberUpdatesRpc(List<AbstractUpdate> rawUpdates, byte[] membersDigest) {
        exchangeAt = System.currentTimeMillis();
        for (AbstractUpdate update : rawUpdates) {
            if (update.shouldFeedback()) {
                this.updates.add(update);
            } else {
                this.notifications.add(update);
            }
        }
        this.membersDigest = membersDigest;
    }

    public List<AbstractUpdate> getUpdates() {
        return updates;
    }

    public List<AbstractUpdate> getNotifications() {
        return notifications;
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
            map.put(update.getId(), false);
        }
        return map;
    }

    public long getExchangeAt() {
        return exchangeAt;
    }

    @Override
    public String toString() {
        return "MemberUpdatesRpc{" +
                "exchangeAt=" + getExchangeAt() +
                ", updates.size=" + updates.size() +
                ", notifications.size=" + notifications.size() +
                ", membersDigest=" + Base64.getEncoder().encodeToString(membersDigest) +
                '}';
    }

}
