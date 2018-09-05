package in.xnnyygn.xgossip;

import java.util.Collection;
import java.util.Set;

public interface MemberManager {

    void initialize();

    void join(Collection<MemberEndpoint> seedEndpoints);

    /**
     * List available members' endpoints.
     *
     * @return collection of endpoints
     */
    Set<MemberEndpoint> listAvailableEndpoints();

    void leave();

    // TODO add listener

    void shutdown();

}
