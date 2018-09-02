package in.xnnyygn.xgossip;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Member implements Comparable<Member> {

    private final MemberEndpoint endpoint;
    private final long timeAdded;
    private final long timeRemoved;

    public Member(@Nonnull MemberEndpoint endpoint) {
        this(endpoint, System.currentTimeMillis());
    }

    public Member(@Nonnull MemberEndpoint endpoint, long timeAdded) {
        this(endpoint, timeAdded, 0);
    }

    public Member(MemberEndpoint endpoint, long timeAdded, long timeRemoved) {
        this.endpoint = endpoint;
        this.timeAdded = timeAdded;
        this.timeRemoved = timeRemoved;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    public boolean doesExist() {
        // bias is add
        return timeAdded >= timeRemoved;
    }

    public long getTimeAdded() {
        return timeAdded;
    }

    public long getTimeRemoved() {
        return timeRemoved;
    }

    @Override
    public int compareTo(@Nonnull Member o) {
        return endpoint.compareTo(o.endpoint);
    }

    public byte[] toBytes() {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(byteOutput);
        try {
            dataOutput.writeUTF(endpoint.getHost());
            dataOutput.writeInt(endpoint.getPort());
            dataOutput.writeLong(timeAdded);
            dataOutput.writeLong(timeRemoved);
        } catch (IOException e) {
            throw new DigestException(e);
        }
        return byteOutput.toByteArray();
    }

    @Override
    public String toString() {
        return "Member{" +
                "endpoint=" + endpoint +
                ", timeAdded=" + timeAdded +
                ", timeRemoved=" + timeRemoved +
                '}';
    }

}
