package in.xnnyygn.xgossip.messages;

public class MembersMergedResponse extends AbstractMessage {

    private final long exchangeAt;

    public MembersMergedResponse(long exchangeAt) {
        this.exchangeAt = exchangeAt;
    }

    public long getExchangeAt() {
        return exchangeAt;
    }

    @Override
    public String toString() {
        return "MembersMergedResponse{" +
                "exchangeAt=" + exchangeAt +
                '}';
    }

}
