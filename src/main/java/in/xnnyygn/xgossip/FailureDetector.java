package in.xnnyygn.xgossip;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import in.xnnyygn.xgossip.messages.*;
import in.xnnyygn.xgossip.updates.MemberSuspectedNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

/**
 * Failure detector.
 */
class FailureDetector {

    private static final Logger logger = LoggerFactory.getLogger(FailureDetector.class);
    private static final long INTERVAL = 3000;
    private static final long PING_TIMEOUT = 1000;
    private static final long PROXY_PING_TIMEOUT = 2000;
    private final ConcurrentLinkedQueue<MemberEndpoint> suspectedMembers = new ConcurrentLinkedQueue<>();
    private final LatencyRecorder latencyRecorder = new LatencyRecorder();
    private final MemberListContext context;
    private volatile Ping lastPing = NO_PING;

    FailureDetector(MemberListContext context) {
        this.context = context;
    }

    void initialize() {
        MessageDispatcher dispatcher = context.getMessageDispatcher();
        dispatcher.register(PingRpc.class, this::onReceivePingRpc);
        dispatcher.register(PingResponse.class, m -> lastPing.onResponse(m));
        dispatcher.register(PingRequestRpc.class, this::onReceivePingRequestRpc);
        dispatcher.register(ProxyPingRpc.class, this::onReceiveProxyPingRpc);
        dispatcher.register(ProxyPingResponse.class, this::onReceiveProxyPingResponse);
        dispatcher.register(ProxyPingDoneResponse.class, m -> lastPing.onResponse(m));

        schedulePing();
    }

    private void schedulePing() {
        logger.debug("schedule ping");
        context.getScheduler().schedule(this::ping, INTERVAL);
    }

    void ping() {
        MemberEndpoint endpoint = selectMember();
        if (endpoint == null) {
            return;
        }
        PingRpc rpc = new PingRpc();
        context.getTransporter().send(endpoint, rpc);
        lastPing = new DirectPing(endpoint, rpc.getPingAt());
    }

    private MemberEndpoint selectMember() {
        Set<MemberEndpoint> pingFailed = latencyRecorder.listEndpointWithFailedPing();

        // find first not pinged member
        MemberEndpoint endpoint;
        do {
            endpoint = suspectedMembers.poll();
        } while (endpoint != null && pingFailed.contains(endpoint));

        if (endpoint != null) {
            return endpoint;
        }
        return context.getMemberList().getRandomEndpointExceptSelf();
    }

    // subscriber
    void onReceivePingRpc(RemoteMessage<PingRpc> message) {
        PingRpc rpc = message.get();
        context.getTransporter().reply(message, new PingResponse(rpc.getPingAt()));
    }

    // subscriber
    private void onReceivePingRequestRpc(RemoteMessage<PingRequestRpc> message) {
        PingRequestRpc rpc = message.get();
        context.getTransporter().send(rpc.getEndpoint(), new ProxyPingRpc(rpc.getPingAt(), message.getSender()));
    }

    // subscriber
    private void onReceiveProxyPingRpc(RemoteMessage<ProxyPingRpc> message) {
        ProxyPingRpc rpc = message.get();
        context.getTransporter().reply(message, new ProxyPingResponse(rpc.getPingAt(), rpc.getSourceEndpoint()));
    }

    // subscriber
    private void onReceiveProxyPingResponse(RemoteMessage<ProxyPingResponse> message) {
        ProxyPingResponse response = message.get();
        context.getTransporter().send(
                response.getSourceEndpoint(),
                new ProxyPingDoneResponse(response.getPingAt(), message.getSender())
        );
    }

    void addSuspectedMember(MemberEndpoint endpoint) {
        suspectedMembers.add(endpoint);
    }

    Set<MemberEndpoint> listSuspectedMembers() {
        return latencyRecorder.listEndpointWithFailedPing();
    }

    List<LatencyRecorder.RankingItem> getLatencyRanking() {
        return latencyRecorder.getRanking();
    }

    // ping lifecycle

    private void resetLastPing() {
        lastPing = NO_PING;
        schedulePing();
    }

    private void failedToPing(MemberEndpoint endpoint, long pingAt) {
        logger.info("ping {} timeout", endpoint);
        latencyRecorder.add(endpoint, pingAt, -1);
        context.getUpdateList().prepend(new MemberSuspectedNotification(endpoint));
        schedulePing();
    }

    private interface Ping {

        void onResponse(RemoteMessage<? extends AbstractPingResponse> message);

    }

    private static class NoPing implements Ping {

        @Override
        public void onResponse(RemoteMessage<? extends AbstractPingResponse> message) {
            logger.info("receive ping response from {} when no ping, ignore", message.getSender());
        }

    }

    private static final NoPing NO_PING = new NoPing();

    private abstract class AbstractPing implements Ping {

        final MemberEndpoint endpoint;
        final long pingAt;
        final ScheduledFuture<?> future;

        AbstractPing(MemberEndpoint endpoint, long pingAt, long timeout) {
            this.endpoint = endpoint;
            this.pingAt = pingAt;
            this.future = context.getScheduler().schedule(this::onTimeout, timeout);
        }

        abstract void onTimeout();

    }

    private class DirectPing extends AbstractPing {

        DirectPing(MemberEndpoint endpoint, long pingAt) {
            super(endpoint, pingAt, PING_TIMEOUT);
        }

        @Override
        public void onResponse(RemoteMessage<? extends AbstractPingResponse> message) {
            AbstractPingResponse response = message.get();
            if (!(response instanceof PingResponse)) {
                logger.info("expect ping response but was {}", response.getClass());
                return;
            }
            if (!endpoint.equals(message.getSender()) || pingAt != response.getPingAt()) {
                logger.info("unexpected ping response from ({}, {}), expect ({}, {})",
                        endpoint, pingAt, message.getSender(), response.getPingAt());
                return;
            }
            long latency = System.currentTimeMillis() - pingAt;
            logger.debug("{}, latency {}ms", endpoint, latency);
            latencyRecorder.add(endpoint, pingAt, latency);
            future.cancel(false);
            resetLastPing();
        }

        @Override
        void onTimeout() {
            Set<MemberEndpoint> proxyEndpoints = context.getMemberList().getRandomEndpointsExcept(3, Sets.union(
                    ImmutableSet.of(context.getSelfEndpoint(), endpoint),
                    latencyRecorder.listEndpointWithFailedPing()
            ));
            if (proxyEndpoints.isEmpty()) {
                logger.debug("no proxy endpoint");
                failedToPing(endpoint, pingAt);
                return;
            }
            logger.debug("proxy ping {} by {}", endpoint, proxyEndpoints);
            for (MemberEndpoint proxyEndpoint : proxyEndpoints) {
                context.getTransporter().send(proxyEndpoint, new PingRequestRpc(pingAt, endpoint));
            }
            lastPing = new ProxyPing(endpoint, pingAt, proxyEndpoints);
        }
    }

    private class ProxyPing extends AbstractPing {

        private final Set<MemberEndpoint> proxyEndpoints;

        ProxyPing(MemberEndpoint endpoint, long pingAt, Set<MemberEndpoint> proxyEndpoints) {
            super(endpoint, pingAt, PROXY_PING_TIMEOUT);
            this.proxyEndpoints = proxyEndpoints;
        }

        @Override
        public void onResponse(RemoteMessage<? extends AbstractPingResponse> message) {
            AbstractPingResponse response = message.get();
            if (!(response instanceof ProxyPingDoneResponse)) {
                logger.info("expect proxy ping done response but was {}", response.getClass());
                return;
            }
            MemberEndpoint endpoint = ((ProxyPingDoneResponse) response).getEndpoint();
            if (!proxyEndpoints.contains(message.getSender()) || pingAt != response.getPingAt() ||
                    !this.endpoint.equals(endpoint)) {
                logger.info("unexpected proxy ping done response from ({}, {}, {}), expect ({}, {}, {})",
                        message.getSender(), pingAt, this.endpoint, proxyEndpoints, pingAt, endpoint);
                return;
            }
            long latency = System.currentTimeMillis() - pingAt;
            logger.debug("{}, latency {}ms through proxy {}", this.endpoint, latency, message.getSender());
            latencyRecorder.add(this.endpoint, pingAt, latency);
            future.cancel(false);
            resetLastPing();
        }

        @Override
        void onTimeout() {
            failedToPing(endpoint, pingAt);
        }

    }

}
