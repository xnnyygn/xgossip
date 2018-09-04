package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.DefaultTransporter;

public class MemberManagerBuilder {

    private final MemberEndpoint selfEndpoint;
    private final MessageDispatcher messageDispatcher = new MessageDispatcher();

    public MemberManagerBuilder(MemberEndpoint selfEndpoint) {
        this.selfEndpoint = selfEndpoint;
    }

    public MemberManager build() {
        MemberListContext context = new MemberListContext();
        context.setSelfEndpoint(selfEndpoint);
        context.setMessageDispatcher(messageDispatcher);
        context.setMemberList(new MemberList(new Member(selfEndpoint)));
        context.setScheduler(new DefaultScheduler());
        context.setTransporter(new DefaultTransporter(selfEndpoint, messageDispatcher));
        return new MemberManagerImpl(context);
    }

}
