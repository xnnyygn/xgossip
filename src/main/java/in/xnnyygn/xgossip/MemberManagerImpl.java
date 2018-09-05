package in.xnnyygn.xgossip;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import in.xnnyygn.xgossip.rpc.messages.*;
import in.xnnyygn.xgossip.support.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

class MemberManagerImpl implements MemberManager {

    private static final long INTERVAL = 3000;
    private static final Logger logger = LoggerFactory.getLogger(MemberManagerImpl.class);
    private final MemberListContext context;
    private final MemberListExchanger memberListExchanger;
    private final FailureDetector failureDetector;

    MemberManagerImpl(MemberListContext context) {
        this.context = context;
        this.memberListExchanger = new MemberListExchanger(context);
        this.failureDetector = new FailureDetector(context);
    }

    @Override
    public void initialize() {
        MessageDispatcher dispatcher = context.getMessageDispatcher();
        dispatcher.register(MemberUpdatesRpc.class, this::onReceiveMemberUpdatesRpc);
        dispatcher.register(MemberJoinRpc.class, this::onReceiveMemberJoinRpc);
        dispatcher.register(MemberJoinResponse.class, this::onReceiveMemberJoinResponse);
        dispatcher.register(MemberLeavedRpc.class, this::onReceiveMemberLeavedRpc);

        memberListExchanger.initialize();
        failureDetector.initialize();
        context.getTransporter().initialize();
        context.getScheduler().scheduleWithFixedDelay(this::spreadUpdates, INTERVAL, INTERVAL);
    }

    private void spreadUpdates() {
        spreadUpdatesTo(context.getMemberList().getRandomEndpointExcept(Sets.union(
                Collections.singleton(context.getSelfEndpoint()),
                failureDetector.listSuspectedMembers()
        )));
    }

    private void spreadUpdatesExcept(MemberEndpoint excluding) {
        spreadUpdatesTo(context.getMemberList().getRandomEndpointExcept(Sets.union(
                ImmutableSet.of(context.getSelfEndpoint(), excluding),
                failureDetector.listSuspectedMembers()
        )));
    }

    private void spreadUpdatesTo(MemberEndpoint endpoint) {
        spreadUpdatesTo(endpoint == null ? Collections.emptyList() : Collections.singletonList(endpoint));
    }

    private void spreadUpdatesTo(Collection<MemberEndpoint> endpoints) {
        if (endpoints.isEmpty()) {
            return;
        }
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(
                context.getUpdateList().take(1),
                context.getNotificationList().take(1),
                context.getMemberList().getDigest()
        );
        for (MemberEndpoint endpoint : endpoints) {
            context.getTransporter().send(endpoint, rpc);
        }
    }

    // subscriber
    private void onReceiveMemberUpdatesRpc(RemoteMessage<MemberUpdatesRpc> message) {
        failureDetector.processNotifications(message.get().getNotifications());
        memberListExchanger.onReceiveMemberUpdatesRpc(message);
    }

    @Override
    public void join(Collection<MemberEndpoint> seedEndpoints) {
        // maybe the first member in cluster
        if (seedEndpoints.isEmpty()) {
            return;
        }
        logger.info("join with seeds {}", seedEndpoints);
        MemberJoinRpc rpc = new MemberJoinRpc(context.getSelfEndpoint(), context.getTimeStarted());
        context.getMemberList().addAll(seedEndpoints, 0);
        for (MemberEndpoint endpoint : seedEndpoints) {
            context.getTransporter().send(endpoint, rpc);
        }
    }

    // subscriber
    void onReceiveMemberJoinRpc(RemoteMessage<MemberJoinRpc> message) {
        MemberJoinRpc rpc = message.get();
        MemberList.UpdateResult result = context.getMemberList().add(rpc.getEndpoint(), rpc.getTimeJoined());

        context.getTransporter().reply(message, new MemberJoinResponse(context.getMemberList().getAll()));

        failureDetector.trustMember(rpc.getEndpoint());

        if (!result.isUpdated()) {
            return;
        }

        logger.info("member {} joined", rpc.getEndpoint());
        context.getUpdateList().memberJoined(rpc.getEndpoint(), rpc.getTimeJoined());
        spreadUpdatesExcept(rpc.getEndpoint());
    }

    // subscriber
    void onReceiveMemberJoinResponse(RemoteMessage<MemberJoinResponse> message) {
        MemberJoinResponse response = message.get();
        context.getMemberList().mergeAll(response.getMembers());
    }

    @Override
    public Set<MemberEndpoint> listAvailableEndpoints() {
        Set<MemberEndpoint> suspected = failureDetector.listSuspectedMembers();
        return context.getMemberList().getAll().stream()
                .filter(m -> m.doesExist() && !suspected.contains(m.getEndpoint()))
                .map(Member::getEndpoint)
                .collect(Collectors.toSet());
    }

    @Override
    public void leave() {
        Set<MemberEndpoint> endpoints = context.getMemberList().getRandomEndpointsExcept(3, Sets.union(
                Collections.singleton(context.getSelfEndpoint()),
                failureDetector.listSuspectedMembers()
        ));
        if (endpoints.isEmpty()) {
            logger.info("leave without telling anyone");
            return;
        }
        MemberLeavedRpc rpc = new MemberLeavedRpc(context.getSelfEndpoint(), System.currentTimeMillis());
        for (MemberEndpoint endpoint : endpoints) {
            context.getTransporter().send(endpoint, rpc);
        }
    }

    private void onReceiveMemberLeavedRpc(RemoteMessage<MemberLeavedRpc> message) {
        MemberLeavedRpc rpc = message.get();
        MemberList.UpdateResult result = context.getMemberList().remove(rpc.getEndpoint(), rpc.getTimeLeaved());
        if (!result.isUpdated()) {
            return;
        }
        logger.info("member {} leaved", rpc.getEndpoint());
        context.getUpdateList().memberLeaved(rpc.getEndpoint(), rpc.getTimeLeaved());
        spreadUpdatesExcept(rpc.getEndpoint());
    }

    @Override
    public void shutdown() {
        context.getScheduler().shutdown();
        context.getTransporter().close();
    }

}
