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
import java.util.stream.Collectors;

@ThreadSafe
public class MemberList {

    private static final Logger logger = LoggerFactory.getLogger(MemberList.class);
    @GuardedBy("this")
    private final Map<MemberEndpoint, Member> memberMap = new HashMap<>();
    private final Random memberRandom = new Random();
    private final MemberEndpoint selfEndpoint;
    private volatile Snapshot snapshot;

    public MemberList(MemberEndpoint selfEndpoint, long timestamp) {
        this.selfEndpoint = selfEndpoint;
        Member self = new Member(selfEndpoint, timestamp, 0);
        memberMap.put(selfEndpoint, self);
        updateSnapshot();
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
            MemberEndpoint endpoint = member.getEndpoint();
            Member oldMember = memberMap.get(endpoint);
            if (oldMember != null &&
                    oldMember.getTimeAdded() >= member.getTimeAdded() &&
                    oldMember.getTimeRemoved() >= member.getTimeRemoved()) {
                continue;
            }
            Member newMember = new Member(
                    endpoint,
                    Math.max((oldMember != null ? oldMember.getTimeAdded() : 0), member.getTimeAdded()),
                    Math.max((oldMember != null ? oldMember.getTimeRemoved() : 0), member.getTimeRemoved())
            );
            logger.info("update member {}", newMember);
            memberMap.put(endpoint, newMember);
            anyUpdated = true;
        }
        if (anyUpdated) {
            updateSnapshot();
        }
        return new UpdateResult(anyUpdated, snapshot.getDigest());
    }

    public synchronized UpdateResult add(MemberEndpoint endpoint, long timeAdded) {
        Member member = memberMap.get(endpoint);
        if (member != null && member.getTimeAdded() >= timeAdded) {
            return new UpdateResult(false, snapshot.getDigest());
        }
        Member newMember = new Member(endpoint, timeAdded, (member != null ? member.getTimeRemoved() : 0));
        logger.info("update member {}", newMember);
        memberMap.put(endpoint, newMember);
        updateSnapshot();
        return new UpdateResult(true, snapshot.getDigest());
    }

    public synchronized UpdateResult addAll(Collection<MemberEndpoint> endpoints, long timeAdded) {
        boolean anyAdded = false;
        for (MemberEndpoint endpoint : endpoints) {
            Member member = memberMap.get(endpoint);
            if (member != null && member.getTimeAdded() >= timeAdded) {
                continue;
            }
            Member newMember = new Member(endpoint, timeAdded, (member != null ? member.getTimeRemoved() : 0));
            logger.info("update member {}", newMember);
            memberMap.put(endpoint, newMember);
            anyAdded = true;
        }
        if (anyAdded) {
            updateSnapshot();
        }
        return new UpdateResult(anyAdded, snapshot.getDigest());
    }

    public synchronized UpdateResult remove(MemberEndpoint endpoint, long timeRemoved) {
        Member member = memberMap.get(endpoint);
        if (member != null && member.getTimeRemoved() >= timeRemoved) {
            return new UpdateResult(false, snapshot.getDigest());
        }
        Member newMember = new Member(endpoint, (member != null ? member.getTimeAdded() : 0), timeRemoved);
        logger.info("update member {}", newMember);
        memberMap.put(endpoint, newMember);
        updateSnapshot();
        return new UpdateResult(true, snapshot.getDigest());
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
    public Set<MemberEndpoint> getRandomEndpointsExcept(int n, Set<MemberEndpoint> excludingEndpoints) {
        List<MemberEndpoint> availableMembers = snapshot.getMembers().stream()
                .filter(m -> m.doesExist() && !excludingEndpoints.contains(m.getEndpoint()))
                .map(Member::getEndpoint)
                .collect(Collectors.toList());
        int nAvailable = availableMembers.size();
        if (nAvailable <= n) {
            return new HashSet<>(availableMembers);
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

    private void updateSnapshot() {
        Collection<Member> members = memberMap.values();
        snapshot = new Snapshot(new ArrayList<>(members), generateDigest(members));
    }

    @Override
    public String toString() {
        Snapshot snapshot = getSnapshot();
        return "MemberList{" +
                "selfEndpoint=" + selfEndpoint +
                ", members=" + snapshot.getMembers() +
                ", digest=" + Base64.getEncoder().encodeToString(snapshot.getDigest()) +
                '}';
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
