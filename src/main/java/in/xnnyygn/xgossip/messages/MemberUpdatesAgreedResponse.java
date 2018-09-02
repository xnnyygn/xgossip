package in.xnnyygn.xgossip.messages;

import java.util.Map;

public class MemberUpdatesAgreedResponse extends AbstractMessage {

    private final long exchangeAt;
    private final Map<Long, Boolean> updatedMap;

    public MemberUpdatesAgreedResponse(long exchangeAt, Map<Long, Boolean> updatedMap) {
        this.exchangeAt = exchangeAt;
        this.updatedMap = updatedMap;
    }

    public long getExchangeAt() {
        return exchangeAt;
    }

    public Map<Long, Boolean> getUpdatedMap() {
        return updatedMap;
    }

    @Override
    public String toString() {
        return "MemberUpdatesAgreedResponse{" +
                "exchangeAt=" + exchangeAt +
                ", updatedMap=" + updatedMap +
                '}';
    }

}
