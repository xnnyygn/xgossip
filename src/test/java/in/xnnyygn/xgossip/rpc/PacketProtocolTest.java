package in.xnnyygn.xgossip.rpc;

import in.xnnyygn.xgossip.MemberEndpoint;
import in.xnnyygn.xgossip.messages.AbstractMessage;
import in.xnnyygn.xgossip.messages.MemberJoinRpc;
import in.xnnyygn.xgossip.messages.RemoteMessage;
import org.junit.Test;

import java.net.DatagramPacket;
import java.util.Arrays;

import static org.junit.Assert.*;

public class PacketProtocolTest {

    private PacketProtocol protocol = new PacketProtocol();

    @Test
    public void test() {
        MemberEndpoint sender = new MemberEndpoint("localhost", 5302);
        MemberEndpoint recipient = new MemberEndpoint("localhost", 5303);
        MemberJoinRpc rpc = new MemberJoinRpc(sender, System.currentTimeMillis());

        DatagramPacket packet = protocol.toPacket(sender, rpc, recipient);
        assertEquals(recipient.getHost(), packet.getAddress().getHostName());
        assertEquals(recipient.getPort(), packet.getPort());

        RemoteMessage<? extends AbstractMessage> message = protocol.fromPacket(packet);
        assertEquals(sender, message.getSender());
        MemberJoinRpc rpc2 = (MemberJoinRpc) message.get();
        assertEquals(rpc.getEndpoint(), rpc2.getEndpoint());
    }

}