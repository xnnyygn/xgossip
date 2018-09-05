package in.xnnyygn.xgossip.updates;

public abstract class AbstractUpdate {

    private final long id;

    public AbstractUpdate(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

}
