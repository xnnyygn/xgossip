package in.xnnyygn.xgossip.rpc.messages;

import in.xnnyygn.xgossip.MemberNotification;
import in.xnnyygn.xgossip.updates.AbstractUpdate;

import java.util.*;

public class MemberUpdatesRpc extends AbstractMessage {

    private final long exchangeAt;
    private final List<AbstractUpdate> updates;
    private final List<MemberNotification> notifications;
    private final byte[] membersDigest;

    public MemberUpdatesRpc(List<AbstractUpdate> updates, byte[] membersDigest) {
        this(System.currentTimeMillis(), updates, Collections.emptyList(), membersDigest);
    }

    public MemberUpdatesRpc(List<AbstractUpdate> updates, List<MemberNotification> notifications, byte[] membersDigest) {
        this(System.currentTimeMillis(), updates, notifications, membersDigest);
    }

    public MemberUpdatesRpc(long exchangeAt, List<AbstractUpdate> updates, List<MemberNotification> notifications, byte[] membersDigest) {
        this.exchangeAt = exchangeAt;
        this.updates = updates;
        this.notifications = notifications;
        this.membersDigest = membersDigest;
    }

    public List<AbstractUpdate> getUpdates() {
        return updates;
    }

    public List<MemberNotification> getNotifications() {
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
