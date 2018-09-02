package in.xnnyygn.xgossip;

import java.util.Collection;

public interface MemberManager {

    void initialize();

    void join(Collection<MemberEndpoint> seeds);

    // TODO get alive members, with self?

    // TODO leave

    // TODO add listener

    void shutdown();

}
