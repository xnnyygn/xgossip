package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.messages.MemberJoinResponse;
import in.xnnyygn.xgossip.messages.MemberJoinRpc;
import in.xnnyygn.xgossip.messages.RemoteMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MemberManagerImplTest {

    private MemberListContext context;
    private MemberManagerImpl manager;

    @Before
    public void setUp() {
        MemberEndpoint selfEndpoint = new MemberEndpoint("localhost", 5302);

        context = new MemberListContext();
        context.setTransporter(new MockTransporter());
        context.setSelfEndpoint(selfEndpoint);
        context.setMemberList(new MemberList(new Member(selfEndpoint)));

        manager = new MemberManagerImpl(context);
    }

    @Test
    public void testJoinNoSeed() {
        manager.join(Collections.emptyList());
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        assertEquals(0, mockTransporter.getMessages().size());
        assertEquals(1, context.getMemberList().getAll().size());
    }

    @Test
    public void testJoin() {
        manager.join(Collections.singletonList(new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        assertEquals(1, mockTransporter.getMessages().size());

        manager.onReceiveMemberJoinResponse(new RemoteMessage<>(new MemberJoinResponse(Arrays.asList(
                new Member(new MemberEndpoint("localhost", 5303)),
                new Member(new MemberEndpoint("localhost", 5304))
        )), new MemberEndpoint("localhost", 5303)));
        assertEquals(3, context.getMemberList().getAll().size());
    }

    @Test
    public void testJoinRoleSeed() {
        MemberEndpoint endpoint1 = new MemberEndpoint("localhost", 5304);
        context.getMemberList().add(new Member(endpoint1));
        MemberEndpoint endpoint2 = new MemberEndpoint("localhost", 5303);
        manager.onReceiveMemberJoinRpc(new RemoteMessage<>(new MemberJoinRpc(endpoint2, System.currentTimeMillis()), endpoint2));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(2, messages.size());
        assertEquals(3, ((MemberJoinResponse) messages.get(0).getPayload()).getMembers().size());
        assertEquals(1, context.getUpdateList().take(10).size());
        assertEquals(endpoint1, messages.get(1).getRecipient());
    }

}