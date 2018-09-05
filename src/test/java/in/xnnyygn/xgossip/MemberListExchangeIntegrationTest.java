package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.messages.MemberUpdatesRpc;
import in.xnnyygn.xgossip.rpc.messages.RemoteMessage;
import in.xnnyygn.xgossip.support.MessageDispatcher;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MemberListExchangeIntegrationTest {

    private final MessageCollector messageCollector = new MessageCollector();
    private MemberListContext context1;
    private MemberListExchanger exchanger1;

    private MemberListContext context2;
    private MemberListExchanger exchanger2;

    @Before
    public void setUp() {
        Map<MemberEndpoint, EmbeddedTransporter> transporterRegister = new HashMap<>();
        Member member1 = new Member(new MemberEndpoint("localhost", 5302));
        Member member2 = new Member(new MemberEndpoint("localhost", 5303));

        context1 = buildContext(member1, transporterRegister);
        context1.getMemberList().add(member2.getEndpoint(), member2.getTimeAdded());
        exchanger1 = new MemberListExchanger(context1);
        exchanger1.initialize();

        context2 = buildContext(member2, transporterRegister);
        context2.getMemberList().add(member1.getEndpoint(), member1.getTimeAdded());
        exchanger2 = new MemberListExchanger(context2);
        exchanger2.initialize();
    }

    private MemberListContext buildContext(Member member, Map<MemberEndpoint, EmbeddedTransporter> transporterRegister) {
        MessageDispatcher messageDispatcher = new MessageDispatcher();
        EmbeddedTransporter transporter = new EmbeddedTransporter(transporterRegister, member.getEndpoint(), messageDispatcher, messageCollector);
        transporterRegister.put(member.getEndpoint(), transporter);

        MemberListContext context = new MemberListContext();
        context.setSelfEndpoint(member.getEndpoint());
        context.setMemberList(new MemberList(member.getEndpoint(), member.getTimeAdded()));
        context.setMessageDispatcher(messageDispatcher);
        context.setTransporter(transporter);
        return context;
    }

    // 1 -> 2
    @Test
    public void testSame() {
        spreadUpdates();

        // -> MemberUpdatesRpc
        // <- MemberUpdatesAgreedResponse
        assertEquals(2, messageCollector.getMessages().size());
    }

    private void spreadUpdates() {
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(context1.getUpdateList().take(10), context1.getMemberList().getDigest());
        messageCollector.add(context1.getSelfEndpoint(), context2.getSelfEndpoint(), rpc);
        exchanger2.onReceiveMemberUpdatesRpc(new RemoteMessage<>(rpc, context1.getSelfEndpoint()));
    }

    private void printMessages() {
        for (MessageCollector.Message message : messageCollector.getMessages()) {
            System.out.println(message.getSender() + " -> " + message.getRecipient() + " " + message.getPayload());
        }
    }

    // 1 -> 2
    // local: (1, 2, 3), (3 added)
    // remote: (1, 2), no update
    @Test
    public void testLocalIsNewer() {
        Member member = new Member(new MemberEndpoint("localhost", 5304));
        context1.getMemberList().add(member.getEndpoint(), member.getTimeAdded());
        context1.getUpdateList().memberJoined(member.getEndpoint(), member.getTimeAdded());

        spreadUpdates();

        // -> MemberUpdatesRpc
        // <- MemberUpdatesAgreedResponse
        assertEquals(2, messageCollector.getMessages().size());
    }

    // 1 -> 2
    // local: (1, 2), no update
    // remote: (1, 2, 3), (3 added)
    @Test
    public void testRemoteIsNewer() {
        Member member = new Member(new MemberEndpoint("localhost", 5304));
        context2.getMemberList().add(member.getEndpoint(), member.getTimeAdded());
        context2.getUpdateList().memberJoined(member.getEndpoint(), member.getTimeAdded());

        spreadUpdates();

        // -> MemberUpdatesRpc
        // <- MemberUpdatesResponse
        // -> MemberUpdatesAgreedResponse
        assertEquals(3, messageCollector.getMessages().size());
    }

    // 1 -> 2
    // local: (1, 2, 3)
    // remote: (1, 2)
    @Test
    public void testLocalIsNewerNoUpdate() {
        Member member = new Member(new MemberEndpoint("localhost", 5304));
        context1.getMemberList().add(member.getEndpoint(), member.getTimeAdded());

        spreadUpdates();

        // -> MemberUpdatesRpc
        // <- MembersMergeResponse
        // -> MembersMergeResponse
        // <- MembersMergedResponse
        assertEquals(4, messageCollector.getMessages().size());
    }

    // 1 -> 2
    // local: (1, 2)
    // remote: (1, 2, 3)
    @Test
    public void testRemoteIsNewerNoUpdate() {
        Member member = new Member(new MemberEndpoint("localhost", 5304));
        context2.getMemberList().add(member.getEndpoint(), member.getTimeAdded());

        spreadUpdates();
        // -> MemberUpdatesRpc
        // <- MembersMergeResponse
        // -> MembersMergedResponse
        assertEquals(3, messageCollector.getMessages().size());
    }

    // 1 -> 2
    // local: (1, 2, 3)
    // remote: (1, 2, 4)
    @Test
    public void testDifferentNoUpdate() {
        context1.getMemberList().add(new MemberEndpoint("localhost", 5304), System.currentTimeMillis());
        context2.getMemberList().add(new MemberEndpoint("localhost", 5305), System.currentTimeMillis());

        spreadUpdates();
        // -> MemberUpdatesRpc
        // <- MembersMergeResponse
        // -> MembersMergeResponse
        // <- MembersMergedResponse
        assertEquals(4, messageCollector.getMessages().size());
    }

    // 1 -> 2
    // local: (1, 2, 3), (3 added)
    // remote: (1, 2, 4)
    @Test
    public void testDifferent2() {
        Member member = new Member(new MemberEndpoint("localhost", 5304));
        context1.getMemberList().add(member.getEndpoint(), member.getTimeAdded());
        context1.getUpdateList().memberJoined(member.getEndpoint(), member.getTimeAdded());
        context2.getMemberList().add(new MemberEndpoint("localhost", 5305), System.currentTimeMillis());

        spreadUpdates();
        // -> MemberUpdatesRpc
        // <- MembersMergeResponse
        // -> MembersMergedResponse
        assertEquals(3, messageCollector.getMessages().size());
    }

    // 1 -> 2
    // local: (1, 2, 3), (3 added)
    // remote: (1, 2, 4), (4 added)
    @Test
    public void testDifferent3() {
        Member member1 = new Member(new MemberEndpoint("localhost", 5304));
        context1.getMemberList().add(member1.getEndpoint(), member1.getTimeAdded());
        context1.getUpdateList().memberJoined(member1.getEndpoint(), member1.getTimeAdded());
        Member member2 = new Member(new MemberEndpoint("localhost", 5305));
        context2.getMemberList().add(member2.getEndpoint(), member2.getTimeAdded());
        context2.getUpdateList().memberJoined(member2.getEndpoint(), member2.getTimeAdded());

        spreadUpdates();

        // -> MemberUpdatesRpc
        // <- MemberUpdatesResponse
        // -> MemberUpdatesAgreedResponse
        assertEquals(3, messageCollector.getMessages().size());
    }

}
