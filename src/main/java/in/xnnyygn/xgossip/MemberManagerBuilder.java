package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.DefaultTransporter;
import in.xnnyygn.xgossip.schedule.DefaultScheduler;
import in.xnnyygn.xgossip.support.MessageDispatcher;

public class MemberManagerBuilder {

    private final MemberEndpoint selfEndpoint;
    private final MessageDispatcher messageDispatcher = new MessageDispatcher();
    private final long timeStarted = System.currentTimeMillis();

    public MemberManagerBuilder(MemberEndpoint selfEndpoint) {
        this.selfEndpoint = selfEndpoint;
    }

    public MemberManager build() {
        MemberListContext context = new MemberListContext();
        context.setSelfEndpoint(selfEndpoint);
        context.setTimeStarted(timeStarted);
        context.setMessageDispatcher(messageDispatcher);
        context.setMemberList(new MemberList(selfEndpoint, timeStarted));
        context.setScheduler(new DefaultScheduler());
        context.setTransporter(new DefaultTransporter(selfEndpoint, messageDispatcher));
        return new MemberManagerImpl(context);
    }

}
