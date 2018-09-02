package in.xnnyygn.xgossip;

import javax.annotation.Nonnull;
import java.util.Objects;

public class MemberEndpoint implements Comparable<MemberEndpoint> {

    private final String host;
    private final int port;

    public MemberEndpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberEndpoint)) return false;
        MemberEndpoint memberEndpoint = (MemberEndpoint) o;
        return port == memberEndpoint.port &&
                Objects.equals(host, memberEndpoint.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ':' + port;
    }

    @Override
    public int compareTo(@Nonnull MemberEndpoint o) {
        int hostCompared = host.compareTo(o.host);
        return hostCompared != 0 ? hostCompared : Integer.compare(port, o.port);
    }

}
