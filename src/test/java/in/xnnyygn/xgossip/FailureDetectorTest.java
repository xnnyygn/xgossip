package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.messages.*;
import in.xnnyygn.xgossip.support.MessageDispatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FailureDetectorTest {

    private MemberListContext context;
    private FailureDetector detector;

    @Before
    public void setUp() {
        MemberEndpoint selfEndpoint = new MemberEndpoint("localhost", 5302);

        context = new MemberListContext();
        context.setTransporter(new MockTransporter());
        context.setScheduler(new MockScheduler());
        context.setSelfEndpoint(selfEndpoint);
        context.setMemberList(new MemberList(selfEndpoint, System.currentTimeMillis()));
        context.setMessageDispatcher(new MessageDispatcher());

        detector = new FailureDetector(context);
        detector.initialize();
    }

    @Test
    public void testPingPong() {
        MemberEndpoint endpoint = new MemberEndpoint("localhost", 5303);
        context.getMemberList().add(endpoint, System.currentTimeMillis());
        detector.ping();
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        PingRpc pingRpc = (PingRpc) mockTransporter.getMessages().get(0).getPayload();
        context.getMessageDispatcher().post(new RemoteMessage<>(new PingResponse(pingRpc.getPingAt()), endpoint));
        List<LatencyRecorder.RankingItem> latencyRanking = detector.getLatencyRanking();
        assertEquals(1, latencyRanking.size());
    }

    @Test
    public void testReceivePing() {
        detector.onReceivePingRpc(new RemoteMessage<>(new PingRpc(), new MemberEndpoint("localhost", 5303)));
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        assertEquals(1, mockTransporter.getMessages().size());
        assertTrue(mockTransporter.getMessages().get(0).getPayload() instanceof PingResponse);
    }

    @Test
    public void testPingTimeout() {
        MemberEndpoint endpoint = new MemberEndpoint("localhost", 5303);
        context.getMemberList().add(endpoint, System.currentTimeMillis());
        detector.ping();
        MockScheduler mockScheduler = (MockScheduler) context.getScheduler();
        mockScheduler.runLastCommand();
    }

    @Test
    public void testProxyPing1() {
        MemberEndpoint endpoint1 = new MemberEndpoint("localhost", 5303);
        MemberEndpoint endpoint2 = new MemberEndpoint("localhost", 5304);
        context.getMemberList().add(endpoint1, System.currentTimeMillis());
        context.getMemberList().add(endpoint2, System.currentTimeMillis());
        detector.ping();
        MockScheduler mockScheduler = (MockScheduler) context.getScheduler();
        mockScheduler.runLastCommand();
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        assertEquals(2, mockTransporter.getMessages().size());
        assertTrue(mockTransporter.getMessages().get(1).getPayload() instanceof PingRequestRpc);
    }

    @Test
    public void testProxyPing2() {
        MemberEndpoint endpoint1 = new MemberEndpoint("localhost", 5303);
        MemberEndpoint endpoint2 = new MemberEndpoint("localhost", 5304);
        context.getMemberList().add(endpoint1, System.currentTimeMillis());
        context.getMemberList().add(endpoint2, System.currentTimeMillis());
        detector.ping();
        MockScheduler mockScheduler = (MockScheduler) context.getScheduler();
        mockScheduler.runLastCommand();
        MockTransporter mockTransporter = (MockTransporter) context.getTransporter();
        assertEquals(2, mockTransporter.getMessages().size());
        MockTransporter.Message message = mockTransporter.getMessages().get(1);
        PingRequestRpc pingRequestRpc = (PingRequestRpc) message.getPayload();
        context.getMessageDispatcher().post(new RemoteMessage<>(
                new ProxyPingDoneResponse(pingRequestRpc.getPingAt(), pingRequestRpc.getEndpoint()),
                message.getRecipient()
        ));
        List<LatencyRecorder.RankingItem> latencyRanking = detector.getLatencyRanking();
        assertEquals(1, latencyRanking.size());
    }

}