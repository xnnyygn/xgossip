package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.updates.AbstractUpdate;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import in.xnnyygn.xgossip.updates.MemberLeavedUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@ThreadSafe
public class UpdateList {

    private static final Logger logger = LoggerFactory.getLogger(UpdateList.class);
    private final ConcurrentMap<Long, UpdateEntry> entryMap = new ConcurrentHashMap<>();
    private final AtomicLong entryId = new AtomicLong();
    private final int threshold;

    public UpdateList(int threshold) {
        this.threshold = threshold;
    }

    public long add(AbstractUpdate update) {
        if (update instanceof MemberJoinedUpdate) {
            MemberJoinedUpdate memberJoinedUpdate = (MemberJoinedUpdate) update;
            return memberJoined(memberJoinedUpdate.getEndpoint(), memberJoinedUpdate.getTimeJoined());
        }
        if (update instanceof MemberLeavedUpdate) {
            MemberLeavedUpdate memberLeavedUpdate = (MemberLeavedUpdate) update;
            return memberLeaved(memberLeavedUpdate.getEndpoint(), memberLeavedUpdate.getTimeLeaved());
        }
        throw new IllegalArgumentException("unsupported update " + update.getClass());
    }

    public long memberJoined(MemberEndpoint endpoint, long timeJoined) {
        long updateId = entryId.incrementAndGet();
        MemberJoinedUpdate update = new MemberJoinedUpdate(updateId, endpoint, timeJoined);
        entryMap.put(updateId, new UpdateEntry(update));
        logger.debug("add member joined update, id {}", updateId);
        return updateId;
    }

    public long memberLeaved(MemberEndpoint endpoint, long timeLeaved) {
        long updateId = entryId.incrementAndGet();
        MemberLeavedUpdate update = new MemberLeavedUpdate(updateId, endpoint, timeLeaved);
        entryMap.put(updateId, new UpdateEntry(update));
        logger.debug("add member leaved update, id {}", updateId);
        return updateId;
    }

    public List<AbstractUpdate> take(int n) {
        return takeExcept(n, Collections.emptySet());
    }

    public List<AbstractUpdate> takeExcept(int n, Set<Long> excluding) {
        return entryMap.values().stream()
                .filter(e -> !excluding.contains(e.getId()))
                .sorted()
                .limit(n)
                .map(UpdateEntry::getUpdate)
                .collect(Collectors.toList());
    }

    public void decreaseUsefulness(long id) {
        UpdateEntry entry = entryMap.get(id);
        if (entry == null) {
            return;
        }
        logger.debug("decrease usefulness of update {}", id);
        if (entry.increaseCountAndGet() >= threshold) {
            logger.debug("delete update {}", id);
            entryMap.remove(id);
        }
    }

    private static class UpdateEntry implements Comparable<UpdateEntry> {

        private final AbstractUpdate update;
        private final AtomicInteger count = new AtomicInteger();

        UpdateEntry(AbstractUpdate update) {
            this.update = update;
        }

        AbstractUpdate getUpdate() {
            return update;
        }

        long getId() {
            return update.getId();
        }

        int increaseCountAndGet() {
            return count.incrementAndGet();
        }

        @Override
        public int compareTo(@Nonnull UpdateEntry o) {
            return Integer.compare(count.get(), o.count.get());
        }

    }

}
