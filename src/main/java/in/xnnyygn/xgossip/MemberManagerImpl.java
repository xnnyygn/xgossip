package in.xnnyygn.xgossip;

import com.google.common.eventbus.Subscribe;
import in.xnnyygn.xgossip.messages.MemberJoinResponse;
import in.xnnyygn.xgossip.messages.MemberJoinRpc;
import in.xnnyygn.xgossip.messages.RemoteMessage;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

class MemberManagerImpl implements MemberManager {

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
        context.getEventBus().register(this);
        memberListExchanger.initialize();
        failureDetector.initialize();
    }

    @Override
    public void join(Collection<MemberEndpoint> seeds) {
        logger.info("join with seeds {}", seeds);
        // maybe the first member in cluster
        if (seeds.isEmpty()) {
            return;
        }
        // send message to seed members
        MemberJoinRpc rpc = new MemberJoinRpc(context.getSelfEndpoint());
        for (MemberEndpoint endpoint : seeds) {
            context.getTransporter().send(endpoint, rpc);
        }
    }

    @Subscribe
    public void onReceiveMemberJoinRpc(RemoteMessage<MemberJoinRpc> message) {
        MemberJoinRpc rpc = message.get();
        // lamport clock
        long timeAdded = Math.max(rpc.getTimestamp(), System.currentTimeMillis());
        Member member = new Member(rpc.getEndpoint(), timeAdded);
        MemberList.UpdateResult result = context.getMemberList().add(member);

        // reply to sender
        context.getTransporter().reply(message, new MemberJoinResponse(context.getMemberList().getAll()));

        if (!result.isUpdated()) {
            return;
        }

        // add to update list
        context.getUpdateList().prepend(new MemberJoinedUpdate(member));

        memberListExchanger.spreadUpdatesExcept(member.getEndpoint());
    }

    @Subscribe
    public void onReceiveMemberJoinResponse(MemberJoinResponse response) {
        context.getMemberList().mergeAll(response.getMembers());
    }

    @Override
    public void shutdown() {
        context.getScheduler().shutdown();
    }

}
