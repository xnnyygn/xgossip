package in.xnnyygn.xgossip.rpc.messages;

import in.xnnyygn.xgossip.Member;

import java.util.*;

public class MembersMergeResponse extends AbstractMessage {

    private final long exchangeAt;
    private final Map<Long, Boolean> updatedMap;
    private final Collection<Member> members;
    private final byte[] membersDigest;
    private final int hopCount;

    public MembersMergeResponse(long exchangeAt, Collection<Member> members, byte[] membersDigest) {
        this(exchangeAt, members, membersDigest, 1);
    }

    public MembersMergeResponse(long exchangeAt, Collection<Member> members, byte[] membersDigest, int hopCount) {
        this(exchangeAt, Collections.emptyMap(), members, membersDigest, hopCount);
    }

    public MembersMergeResponse(long exchangeAt, Map<Long, Boolean> updatedMap, Collection<Member> members, byte[] membersDigest) {
        this(exchangeAt, updatedMap, members, membersDigest, 1);
    }

    public MembersMergeResponse(long exchangeAt, Map<Long, Boolean> updatedMap, Collection<Member> members, byte[] membersDigest, int hopCount) {
        this.exchangeAt = exchangeAt;
        this.updatedMap = updatedMap;
        this.members = members;
        this.membersDigest = membersDigest;
        this.hopCount = hopCount;
    }

    public long getExchangeAt() {
        return exchangeAt;
    }

    public Map<Long, Boolean> getUpdatedMap() {
        return updatedMap;
    }

    public Collection<Member> getMembers() {
        return members;
    }

    public byte[] getMembersDigest() {
        return membersDigest;
    }

    public int getHopCount() {
        return hopCount;
    }

    @Override
    public String toString() {
        return "MembersMergeResponse{" +
                "exchangeAt=" + exchangeAt +
                ", updatedMap=" + updatedMap +
                ", members.size=" + members.size() +
                ", membersDigest=" + Base64.getEncoder().encodeToString(membersDigest) +
                ", hopCount=" + hopCount +
                '}';
    }

}
