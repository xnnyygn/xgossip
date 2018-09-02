package in.xnnyygn.xgossip;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import in.xnnyygn.xgossip.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class FailureDetector {

    private static final Logger logger = LoggerFactory.getLogger(FailureDetector.class);
    private static final long INTERVAL = 3000;
    private static final long PING_TIMEOUT = 1000;
    private static final long PROXY_PING_TIMEOUT = 2000;
    private final MemberListContext context;
    private ScheduledFuture<?> pingTimeout;
    private ScheduledFuture<?> proxyPingTimeout;

    FailureDetector(@Nonnull MemberListContext context) {
        this.context = context;
    }

    void initialize() {
        context.getEventBus().register(this);
        schedulePing();
    }

    private void schedulePing() {
        context.getScheduler().schedule(this::ping, INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void ping() {
        MemberEndpoint endpoint = context.getMemberList().getRandomEndpointExceptSelf();
        if (endpoint != null) {
            context.getTransporter().send(endpoint, new PingRpc());
            pingTimeout = context.getScheduler().schedule(() -> pingTimeout(endpoint), PING_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    public void pingTimeout(@Nonnull MemberEndpoint endpoint) {
        Collection<MemberEndpoint> proxyEndpoints = context.getMemberList().getRandomEndpointsExcept(3, ImmutableSet.of(context.getSelfEndpoint(), endpoint));
        if (proxyEndpoints.isEmpty()) {
            // no proxy to ping
            context.getLatencyRecorder().add(endpoint, System.currentTimeMillis(), -1);
            return;
        }
        for (MemberEndpoint proxyEndpoint : proxyEndpoints) {
            context.getTransporter().send(endpoint, new PingRequestRpc(endpoint));
        }
        proxyPingTimeout = context.getScheduler().schedule(() -> proxyPingTimeout(endpoint), PROXY_PING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void onReceivePingRpc(RemoteMessage<PingRpc> message) {
        context.getTransporter().reply(message, new PongResponse());
    }

    @Subscribe
    public void onReceivePongResponse(RemoteMessage<PongResponse> message) {
        cancelTimeoutAndSchedulePing(pingTimeout);
    }

    private void cancelTimeoutAndSchedulePing(ScheduledFuture<?> timeout) {
        if (timeout == null) {
            logger.warn("receive ping response when not ping");
            return;
        }
        if (timeout.isDone()) {
            logger.debug("receive ping response after timeout");
            return;
        }
        timeout.cancel(false);
        schedulePing();
    }

    @Subscribe
    public void onReceivePingRequestRpc(RemoteMessage<PingRequestRpc> message) {
        PingRequestRpc rpc = message.get();
        context.getTransporter().send(rpc.getEndpoint(), new ProxyPingRpc(message.getSender()));
    }

    public void proxyPingTimeout(@Nonnull MemberEndpoint endpoint) {
        context.getLatencyRecorder().add(endpoint, System.currentTimeMillis(), -1);
    }

    @Subscribe
    public void onReceiveProxyPingRpc(RemoteMessage<ProxyPingRpc> message) {
        ProxyPingRpc rpc = message.get();
        context.getTransporter().reply(message, new ProxyPingResponse(rpc.getEndpoint()));
    }

    @Subscribe
    public void onReceiveProxyPingResponse(RemoteMessage<ProxyPingResponse> message) {
        context.getTransporter().send(message.get().getEndpoint(), new PingRequestDoneResponse(message.getSender()));
    }

    @Subscribe
    public void onReceivePingRequestDoneResponse(PingRequestDoneResponse response) {
        cancelTimeoutAndSchedulePing(proxyPingTimeout);
    }



}
