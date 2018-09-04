package in.xnnyygn.xgossip;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import in.xnnyygn.xgossip.messages.MemberJoinResponse;
import in.xnnyygn.xgossip.messages.MemberJoinRpc;
import in.xnnyygn.xgossip.messages.MemberUpdatesRpc;
import in.xnnyygn.xgossip.messages.RemoteMessage;
import in.xnnyygn.xgossip.updates.AbstractUpdate;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import in.xnnyygn.xgossip.updates.MemberSuspectedNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

        memberListExchanger.initialize();
//        failureDetector.initialize();
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
            logger.debug("no member to exchange updates, skip");
            return;
        }
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(
                context.getUpdateList().take(1),
                context.getMemberList().getDigest()
        );
        for (MemberEndpoint endpoint : endpoints) {
            logger.info("start exchange with {}", endpoint);
            context.getTransporter().send(endpoint, rpc);
        }
    }

    // subscriber
    private void onReceiveMemberUpdatesRpc(RemoteMessage<MemberUpdatesRpc> message) {
        for (AbstractUpdate notification : message.get().getNotifications()) {
            processNotification(notification);
        }
        memberListExchanger.onReceiveMemberUpdatesRpc(message);
    }

    private void processNotification(AbstractUpdate notification) {
        if (notification instanceof MemberSuspectedNotification) {
            failureDetector.addSuspectedMember(((MemberSuspectedNotification) notification).getEndpoint());
        } else {
            logger.warn("unknown notification {}", notification);
        }
    }

    @Override
    public void join(Collection<MemberEndpoint> seedEndpoints) {
        // maybe the first member in cluster
        if (seedEndpoints.isEmpty()) {
            return;
        }
        logger.info("join with seeds {}", seedEndpoints);
        Member self = context.getMemberList().getSelf();
        if (self == null) {
            logger.warn("not in member list");
            return;
        }
        MemberJoinRpc rpc = new MemberJoinRpc(self.getEndpoint(), self.getTimeAdded());
        List<Member> seedMembers = seedEndpoints.stream()
                .map(e -> new Member(e, 0))
                .collect(Collectors.toList());
        context.getMemberList().addAll(seedMembers);
        seedMembers.forEach(m -> context.getTransporter().send(m.getEndpoint(), rpc));
    }

    // subscriber
    void onReceiveMemberJoinRpc(RemoteMessage<MemberJoinRpc> message) {
        MemberJoinRpc rpc = message.get();
        Member member = new Member(rpc.getEndpoint(), rpc.getTimeJoined());
        MemberList.UpdateResult result = context.getMemberList().add(member);

        // reply to sender
        context.getTransporter().reply(message, new MemberJoinResponse(context.getMemberList().getAll()));

        if (!result.isUpdated()) {
            return;
        }

        // add to update list
        context.getUpdateList().prepend(new MemberJoinedUpdate(member));

        spreadUpdatesExcept(member.getEndpoint());
    }

    // subscriber
    void onReceiveMemberJoinResponse(RemoteMessage<MemberJoinResponse> message) {
        MemberJoinResponse response = message.get();
        context.getMemberList().mergeAll(response.getMembers());
    }

    @Override
    public void shutdown() {
        context.getScheduler().shutdown();
        context.getTransporter().close();
    }

}
