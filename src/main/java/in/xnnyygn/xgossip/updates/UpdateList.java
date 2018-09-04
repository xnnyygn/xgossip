package in.xnnyygn.xgossip.updates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
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

    public long prepend(AbstractUpdate update) {
        long updateId = entryId.incrementAndGet();
        update.setId(updateId);
        entryMap.put(updateId, new UpdateEntry(update));
        logger.debug("prepend update {}", updateId);
        return updateId;
    }

    public List<AbstractUpdate> take(int n) {
        return takeExcept(n, Collections.emptySet());
    }

    public List<AbstractUpdate> takeExcept(int n, Set<Long> excluding) {
        List<UpdateEntry> entries =
                entryMap.values().stream()
                        .filter(e -> !excluding.contains(e.getId()))
                        .sorted()
                        .limit(n)
                        .collect(Collectors.toList());
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<AbstractUpdate> result = new ArrayList<>();
        for (UpdateEntry e : entries) {
            if (!e.shouldFeedback() && e.increaseCountAndGet() >= threshold) {
                entryMap.remove(e.getId());
            }
            result.add(e.getUpdate());
        }
        return result;
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

        boolean shouldFeedback() {
            return update.shouldFeedback();
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
