package in.xnnyygn.xgossip.updates;

public abstract class AbstractUpdate {

    private long id;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public abstract boolean shouldFeedback();

}
