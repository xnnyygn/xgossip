package in.xnnyygn.xgossip;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import in.xnnyygn.xgossip.messages.*;
import in.xnnyygn.xgossip.updates.AbstractUpdate;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

class MemberListExchanger {

    private static final long INTERVAL = 1000;
    private static final int MAX_UPDATES = 1;
    private static final int MAX_MEMBER_UPDATES_RESPONSE_TURN = 10;
    private static final Logger logger = LoggerFactory.getLogger(MemberListExchanger.class);
    private final MemberListContext context;

    MemberListExchanger(MemberListContext context) {
        this.context = context;
    }

    void initialize() {
        context.getEventBus().register(this);
        context.getScheduler().scheduleWithFixedDelay(this::spreadUpdates, INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);
    }

    // run periodically
    void spreadUpdates() {
        spreadUpdatesTo(context.getMemberList().getRandomEndpointExceptSelf());
    }

    void spreadUpdatesExcept(MemberEndpoint excluding) {
        spreadUpdatesTo(context.getMemberList().getRandomEndpointExcept(ImmutableSet.of(context.getSelfEndpoint(), excluding)));
    }

    private void spreadUpdatesTo(MemberEndpoint endpoint) {
        if (endpoint == null) {
            return;
        }
        MemberUpdatesRpc rpc = new MemberUpdatesRpc(
                context.getUpdateList().take(MAX_UPDATES),
                context.getMemberList().getDigest()
        );
        logger.info("start exchange with {}, {}", endpoint, rpc);
        context.getTransporter().send(endpoint, rpc);
    }

    @Subscribe
    public void onReceiveMemberUpdatesRpc(RemoteMessage<MemberUpdatesRpc> message) {
        MemberUpdatesRpc rpc = message.get();
        logger.debug("exchange with {}, {}", message.getSender(), rpc);
        context.getTransporter().reply(message, processMemberUpdatesRpc(rpc));
    }

    private AbstractMessage processMemberUpdatesRpc(MemberUpdatesRpc rpc) {
        // 1. same digest, agree
        if (Arrays.equals(rpc.getMembersDigest(), context.getMemberList().getDigest())) {
            return new MemberUpdatesAgreedResponse(rpc.getExchangeAt(), rpc.makeUpdateIdMap());
        }
        if (rpc.hasUpdate()) {
            MultiUpdateResult result = processUpdates(rpc.getUpdates());
            if (Arrays.equals(rpc.getMembersDigest(), result.getLastDigest())) {

                // 2. apply remote updates and agree
                return new MemberUpdatesAgreedResponse(rpc.getExchangeAt(), result.getUpdatedMap());
            }
            List<AbstractUpdate> updates = context.getUpdateList().takeExcept(MAX_UPDATES, result.getLocalUpdateIds());
            if (updates.isEmpty()) {

                // 3. try to merge members
                MemberList.Snapshot snapshot = context.getMemberList().getSnapshot();
                return new MembersMergeResponse(rpc.getExchangeAt(), result.getUpdatedMap(), snapshot.getMembers(), snapshot.getDigest());
            }

            // 4. send updates and retry by remote
            return new MemberUpdatesResponse(rpc.getExchangeAt(), result.getUpdatedMap(), updates, result.getLastDigest());
        }

        // no update
        List<AbstractUpdate> updates = context.getUpdateList().take(MAX_UPDATES);
        MemberList.Snapshot snapshot = context.getMemberList().getSnapshot();
        if (updates.isEmpty()) {

            // maybe both local and remote members deleted recent updates
            // 5. try to merge members
            return new MembersMergeResponse(rpc.getExchangeAt(), snapshot.getMembers(), snapshot.getDigest());
        }

        // 6. send updates and retry by remote
        return new MemberUpdatesResponse(rpc.getExchangeAt(), Collections.emptyMap(), updates, snapshot.getDigest());
    }

    private MultiUpdateResult processUpdates(List<AbstractUpdate> updates) {
        assert !updates.isEmpty();
        Map<Long, Boolean> updatedMap = new HashMap<>();
        Set<Long> localUpdateIds = new HashSet<>();
        byte[] lastDigest = null;
        for (AbstractUpdate update : updates) {
            MemberList.UpdateResult result = processUpdate(update);
            updatedMap.put(update.getId(), result.isUpdated());
            if (result.isUpdated()) {
                localUpdateIds.add(context.getUpdateList().prepend(update));
            }
            lastDigest = result.getDigest();
        }
        return new MultiUpdateResult(updatedMap, localUpdateIds, lastDigest);
    }

    private MemberList.UpdateResult processUpdate(AbstractUpdate update) {
        logger.debug("apply update {}", update);
        if (update instanceof MemberJoinedUpdate) {
            MemberJoinedUpdate memberJoinedUpdate = (MemberJoinedUpdate) update;
            return context.getMemberList().add(
                    new Member(memberJoinedUpdate.getEndpoint(), memberJoinedUpdate.getTimeJoined())
            );
        }
        throw new IllegalArgumentException("unsupported update " + update);
    }

    @Subscribe
    public void onReceiveMemberUpdatesAgreedResponse(RemoteMessage<MemberUpdatesAgreedResponse> message) {
        MemberUpdatesAgreedResponse response = message.get();
        logger.info("exchange done with {}, {}", message.getSender(), response);
        feedback(response.getUpdatedMap());
    }

    private void feedback(Map<Long, Boolean> updatedMap) {
        if (updatedMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Boolean> updateResult : updatedMap.entrySet()) {
            // update is not applied
            if (!updateResult.getValue()) {
                context.getUpdateList().decreaseUsefulness(updateResult.getKey());
            }
        }
    }

    @Subscribe
    public void onReceiveMemberUpdatesResponse(RemoteMessage<MemberUpdatesResponse> message) {
        MemberUpdatesResponse response = message.get();
        logger.debug("exchange with {}, {}", message.getSender(), response);
        try {
            context.getTransporter().reply(message, processMemberUpdatesResponse(response));
        } catch (ExchangeTurnExceedException e) {
            logger.warn("exchange with {} at {}, member list cannot agree within max turns, wait for next period",
                    message.getSender(), response.getExchangeAt());
        }
    }

    // case 4, 6
    private AbstractMessage processMemberUpdatesResponse(MemberUpdatesResponse response) {
        feedback(response.getUpdatedMap());
        MultiUpdateResult result = processUpdates(response.getUpdates());
        if (Arrays.equals(response.getMembersDigest(), result.getLastDigest())) {

            // 1. apply remote updates and agree
            return new MemberUpdatesAgreedResponse(response.getExchangeAt(), result.getUpdatedMap());
        }

        List<AbstractUpdate> updates = context.getUpdateList().takeExcept(
                MAX_UPDATES,
                Sets.union(response.getUpdateIds(), result.getLocalUpdateIds())
        );
        MemberList.Snapshot snapshot = context.getMemberList().getSnapshot();
        if (updates.isEmpty()) {

            // 2. try to merge members
            return new MembersMergeResponse(response.getExchangeAt(), snapshot.getMembers(), snapshot.getDigest());
        }

        if (response.getHopCount() > MAX_MEMBER_UPDATES_RESPONSE_TURN) {
            throw new ExchangeTurnExceedException();
        }

        // 3. send updates and retry by remote
        return new MemberUpdatesResponse(
                response.getExchangeAt(),
                result.getUpdatedMap(),
                updates,
                snapshot.getDigest(),
                response.getHopCount() + 1
        );
    }

    // case 3, 5
    @Subscribe
    public void onReceiveMembersMergeResponse(RemoteMessage<MembersMergeResponse> message) {
        MembersMergeResponse response = message.get();
        logger.debug("exchange with {}, {}", message.getSender(), response);
        feedback(response.getUpdatedMap());
        MemberList.UpdateResult result = context.getMemberList().mergeAll(response.getMembers());
        if (Arrays.equals(result.getDigest(), response.getMembersDigest())) {

            // 1. merged and agree
            context.getTransporter().reply(message, new MembersMergedResponse(response.getExchangeAt()));
        } else {
            if (response.getHopCount() > 1) {

                // 2. local or remote changed when try to merge members
                context.getTransporter().reply(message, new MemberUpdatesRpc(
                        context.getUpdateList().take(MAX_UPDATES),
                        result.getDigest()
                ));
            } else {

                // 3. try to merge members
                MemberList.Snapshot snapshot = context.getMemberList().getSnapshot();
                context.getTransporter().reply(message, new MembersMergeResponse(
                        response.getExchangeAt(),
                        snapshot.getMembers(),
                        snapshot.getDigest(),
                        response.getHopCount() + 1
                ));
            }
        }
    }

    @Subscribe
    public void onReceiveMembersMergedResponse(RemoteMessage<MembersMergedResponse> message) {
        logger.info("done exchange with {}, {}", message.getSender(), message.get());
    }

    private static class MultiUpdateResult {

        private final Map<Long, Boolean> updatedMap;
        private final Set<Long> localUpdateIds;
        private final byte[] lastDigest;

        MultiUpdateResult(Map<Long, Boolean> updatedMap, Set<Long> localUpdateIds, byte[] lastDigest) {
            this.updatedMap = updatedMap;
            this.localUpdateIds = localUpdateIds;
            this.lastDigest = lastDigest;
        }

        Map<Long, Boolean> getUpdatedMap() {
            return updatedMap;
        }

        Set<Long> getLocalUpdateIds() {
            return localUpdateIds;
        }

        byte[] getLastDigest() {
            return lastDigest;
        }

    }

}
