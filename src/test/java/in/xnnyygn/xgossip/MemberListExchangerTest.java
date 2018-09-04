package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.messages.*;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemberListExchangerTest {

    private MemberListContext context;
    private MemberListExchanger exchanger;

    @Before
    public void setUp() {
        MemberEndpoint selfEndpoint = new MemberEndpoint("localhost", 5302);

        context = new MemberListContext();
        context.setTransporter(new MockTransporter());
        context.setSelfEndpoint(selfEndpoint);
        context.setMemberList(new MemberList(new Member(selfEndpoint)));

        exchanger = new MemberListExchanger(context);
    }

    // case 1
    @Test
    public void testOnReceiveMemberUpdatesRpcSameDigest() {
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(Collections.emptyList(), context.getMemberList().getDigest());
        exchanger.onReceiveMemberUpdatesRpc(new RemoteMessage<>(rpc, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MemberUpdatesAgreedResponse);
    }

    // case 5
    // also no local update
    @Test
    public void testOnReceiveMemberUpdatesRpcNoUpdate1() {
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(Collections.emptyList(), new byte[0]);
        exchanger.onReceiveMemberUpdatesRpc(new RemoteMessage<>(rpc, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MembersMergeResponse);
    }

    // case 6
    // local update present
    @Test
    public void testOnReceiveMemberUpdatesRpcNoUpdate2() {
        context.getUpdateList().prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5304), System.currentTimeMillis()));
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(Collections.emptyList(), new byte[0]);
        exchanger.onReceiveMemberUpdatesRpc(new RemoteMessage<>(rpc, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MemberUpdatesResponse);
    }

    // case 2
    @Test
    public void testOnReceiveMemberUpdatesRpcApplyAndOk() {
        Member newMember = new Member(new MemberEndpoint("localhost", 5304));
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(Collections.singletonList(
                new MemberJoinedUpdate(10, newMember.getEndpoint(), newMember.getTimeAdded())
        ), MemberList.generateDigest(Arrays.asList(
                context.getMemberList().getAll().iterator().next(),
                newMember
        )));
        exchanger.onReceiveMemberUpdatesRpc(new RemoteMessage<>(rpc, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        MemberUpdatesAgreedResponse response = (MemberUpdatesAgreedResponse) messages.get(0).getPayload();
        assertTrue(response.getUpdatedMap().get(10L));
    }

    // case 3
    // no local update
    @Test
    public void testOnReceiveMemberUpdatesRpcApplyButFailed1() {
        Member newMember = new Member(new MemberEndpoint("localhost", 5304));
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(Collections.singletonList(
                new MemberJoinedUpdate(10, newMember.getEndpoint(), newMember.getTimeAdded())
        ), new byte[0]);
        exchanger.onReceiveMemberUpdatesRpc(new RemoteMessage<>(rpc, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MembersMergeResponse);
    }

    // case 4
    @Test
    public void testOnReceiveMemberUpdatesRpcApplyButFailed2() {
        context.getUpdateList().prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5305), System.currentTimeMillis()));
        Member newMember = new Member(new MemberEndpoint("localhost", 5304));
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(Collections.singletonList(
                new MemberJoinedUpdate(10, newMember.getEndpoint(), newMember.getTimeAdded())
        ), new byte[0]);
        exchanger.onReceiveMemberUpdatesRpc(new RemoteMessage<>(rpc, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        MemberUpdatesResponse response = (MemberUpdatesResponse) messages.get(0).getPayload();
        assertTrue(response.getUpdatedMap().get(10L));
    }

    @Test
    public void testOnReceiveMemberUpdatesAgreedResponse() {
        long updateId1 = context.getUpdateList().prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5304), System.currentTimeMillis()));
        long updateId2 = context.getUpdateList().prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5305), System.currentTimeMillis()));
        Map<Long, Boolean> updatedMap = new HashMap<>();
        updatedMap.put(updateId1, true);
        updatedMap.put(updateId2, false);
        MemberUpdatesAgreedResponse response = new MemberUpdatesAgreedResponse(System.currentTimeMillis(), updatedMap);
        exchanger.onReceiveMemberUpdatesAgreedResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
    }

    // case 1
    @Test
    public void testOnReceiveMembersMergeResponseApplyAndOk() {
        List<Member> members = Arrays.asList(
                context.getMemberList().getAll().iterator().next(),
                new Member(new MemberEndpoint("localhost", 5303)),
                new Member(new MemberEndpoint("localhost", 5304))
        );
        MembersMergeResponse response = new MembersMergeResponse(System.currentTimeMillis(), members, MemberList.generateDigest(members));
        exchanger.onReceiveMembersMergeResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MembersMergedResponse);
    }

    // case 2
    @Test
    public void testOnReceiveMembersMergeResponseApplyAndFailed() {
        List<Member> members = Arrays.asList(
                context.getMemberList().getAll().iterator().next(),
                new Member(new MemberEndpoint("localhost", 5303)),
                new Member(new MemberEndpoint("localhost", 5304))
        );
        MembersMergeResponse response = new MembersMergeResponse(System.currentTimeMillis(), members, new byte[0]);
        exchanger.onReceiveMembersMergeResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        MembersMergeResponse payload = (MembersMergeResponse) messages.get(0).getPayload();
        assertEquals(2, payload.getHopCount());
    }

    // case 3
    // hop count > 1
    @Test
    public void testOnReceiveMembersMergeResponseApplyAndFailed2() {
        List<Member> members = Arrays.asList(
                context.getMemberList().getAll().iterator().next(),
                new Member(new MemberEndpoint("localhost", 5303)),
                new Member(new MemberEndpoint("localhost", 5304))
        );
        MembersMergeResponse response = new MembersMergeResponse(System.currentTimeMillis(), members, new byte[0], 2);
        exchanger.onReceiveMembersMergeResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MemberUpdatesRpc);
    }

    // case 1
    @Test
    public void testOnReceiveMemberUpdatesResponseApplyAndOk() {
        Member newMember = new Member(new MemberEndpoint("localhost", 5303));
        MemberUpdatesResponse response = new MemberUpdatesResponse(
                System.currentTimeMillis(),
                Collections.emptyMap(),
                Collections.singletonList(new MemberJoinedUpdate(newMember.getEndpoint(), newMember.getTimeAdded())),
                MemberList.generateDigest(Arrays.asList(
                        context.getMemberList().getAll().iterator().next(),
                        newMember
                ))
        );
        exchanger.onReceiveMemberUpdatesResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MemberUpdatesAgreedResponse);
    }

    // case 2
    @Test
    public void testOnReceiveMemberUpdatesResponseNoLocalUpdate() {
        Member newMember = new Member(new MemberEndpoint("localhost", 5303));
        MemberUpdatesResponse response = new MemberUpdatesResponse(
                System.currentTimeMillis(),
                Collections.emptyMap(),
                Collections.singletonList(new MemberJoinedUpdate(newMember.getEndpoint(), newMember.getTimeAdded())),
                new byte[0]
        );
        exchanger.onReceiveMemberUpdatesResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MembersMergeResponse);
    }

    // case 3
    @Test
    public void testOnReceiveMemberUpdatesResponseWithLocalUpdate() {
        context.getUpdateList().prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5304), System.currentTimeMillis()));
        Member newMember = new Member(new MemberEndpoint("localhost", 5303));
        MemberUpdatesResponse response = new MemberUpdatesResponse(
                System.currentTimeMillis(),
                Collections.emptyMap(),
                Collections.singletonList(new MemberJoinedUpdate(newMember.getEndpoint(), newMember.getTimeAdded())),
                new byte[0]
        );
        exchanger.onReceiveMemberUpdatesResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof MemberUpdatesResponse);
    }

    @Test
    public void testOnReceiveMemberUpdatesResponseHopExceed() {
        context.getUpdateList().prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5304), System.currentTimeMillis()));
        Member newMember = new Member(new MemberEndpoint("localhost", 5303));
        MemberUpdatesResponse response = new MemberUpdatesResponse(
                System.currentTimeMillis(),
                Collections.emptyMap(),
                Collections.singletonList(new MemberJoinedUpdate(newMember.getEndpoint(), newMember.getTimeAdded())),
                new byte[0],
                11
        );
        exchanger.onReceiveMemberUpdatesResponse(new RemoteMessage<>(response, new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        List<MockTransporter.Message> messages = mockTransporter.getMessages();
        assertEquals(0, messages.size());
    }

}