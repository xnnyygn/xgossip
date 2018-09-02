package in.xnnyygn.xgossip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ThreadSafe
public class MemberList {

    private static final Logger logger = LoggerFactory.getLogger(MemberList.class);
    @GuardedBy("this")
    private final Map<MemberEndpoint, Member> memberMap = new HashMap<>();
    private final Random memberRandom = new Random();
    private final MemberEndpoint selfEndpoint;
    private volatile Snapshot snapshot;

    public MemberList(Member self) {
        memberMap.put(self.getEndpoint(), self);
        Collection<Member> members = Collections.singletonList(self);
        snapshot = new Snapshot(members, generateDigest(members));
        selfEndpoint = self.getEndpoint();
    }

    public static byte[] generateDigest(Collection<Member> members) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            members.stream().sorted().forEach(m -> messageDigest.update(m.toBytes()));
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException(e);
        }
    }

    public synchronized UpdateResult mergeAll(Collection<Member> members) {
        boolean anyUpdated = false;
        for (Member member : members) {
            if (doMerge(member)) {
                anyUpdated = true;
            }
        }
        if (anyUpdated) {
            updateSnapshot();
        }
        return new UpdateResult(anyUpdated, snapshot.getDigest());
    }

    private boolean doMerge(Member member) {
        return doMerge(member, (oldMember) ->
                oldMember.getTimeAdded() >= member.getTimeAdded() && oldMember.getTimeRemoved() >= member.getTimeRemoved()
        );
    }

    private boolean doMerge(Member member, Function<Member, Boolean> f) {
        Member oldMember = memberMap.get(member.getEndpoint());
        if (oldMember != null
                && f.apply(oldMember)) {
            return false;
        }
        logger.info("update member {}", member);
        memberMap.put(member.getEndpoint(), member);
        return true;
    }

    public synchronized UpdateResult add(Member member) {
        boolean added = doMerge(member, oldMember -> oldMember.getTimeAdded() >= member.getTimeAdded());
        if (added) {
            updateSnapshot();
        }
        return new UpdateResult(added, snapshot.getDigest());
    }

    @Nullable
    public MemberEndpoint getRandomEndpointExceptSelf() {
        return getFirstOrNull(getRandomEndpointsExcept(1, Collections.singleton(selfEndpoint)));
    }

    @Nullable
    public MemberEndpoint getRandomEndpointExcept(Set<MemberEndpoint> excludingEndpoints) {
        return getFirstOrNull(getRandomEndpointsExcept(1, excludingEndpoints));
    }

    private <T> T getFirstOrNull(Collection<T> c) {
        Iterator<T> it = c.iterator();
        return it.hasNext() ? it.next() : null;
    }

    @Nonnull
    public Collection<MemberEndpoint> getRandomEndpointsExcept(int n, Set<MemberEndpoint> excludingEndpoints) {
        List<MemberEndpoint> availableMembers = snapshot.getMembers().stream()
                .filter(m -> m.doesExist() && !excludingEndpoints.contains(m.getEndpoint()))
                .map(Member::getEndpoint)
                .collect(Collectors.toList());
        int nAvailable = availableMembers.size();
        if (nAvailable <= n) {
            return availableMembers;
        }
        Set<MemberEndpoint> result = new HashSet<>();
        while (result.size() < n) {
            result.add(availableMembers.get(memberRandom.nextInt(nAvailable)));
        }
        return result;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public byte[] getDigest() {
        return snapshot.getDigest();
    }

    public Collection<Member> getAll() {
        return snapshot.getMembers();
    }

    // within instance lock
    private void updateSnapshot() {
        Collection<Member> members = memberMap.values();
        snapshot = new Snapshot(new ArrayList<>(members), generateDigest(members));
    }

    /**
     * Snapshot of current member list.
     */
    public static class Snapshot {

        private final Collection<Member> members;
        private final byte[] digest;

        Snapshot(Collection<Member> members, byte[] digest) {
            this.members = members;
            this.digest = digest;
        }

        public Collection<Member> getMembers() {
            return members;
        }

        public byte[] getDigest() {
            return digest;
        }

    }

    /**
     * Update result.
     */
    public static class UpdateResult {

        private final boolean updated;
        private final byte[] digest;

        UpdateResult(boolean updated, byte[] digest) {
            this.updated = updated;
            this.digest = digest;
        }

        public boolean isUpdated() {
            return updated;
        }

        public byte[] getDigest() {
            return digest;
        }

        @Override
        public String toString() {
            return "UpdateResult{" +
                    "updated=" + updated +
                    ", digest=" + Base64.getEncoder().encodeToString(digest) +
                    '}';
        }

    }

}
