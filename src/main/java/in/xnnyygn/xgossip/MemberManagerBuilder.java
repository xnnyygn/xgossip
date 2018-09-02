package in.xnnyygn.xgossip;

import com.google.common.eventbus.EventBus;

public class MemberManagerBuilder {

    private final MemberEndpoint selfEndpoint;
    private final EventBus eventBus = new EventBus();

    public MemberManagerBuilder(MemberEndpoint selfEndpoint) {
        this.selfEndpoint = selfEndpoint;
    }

    public MemberManager build() {
        MemberListContext context = new MemberListContext();
        context.setEventBus(eventBus);
        context.setSelfEndpoint(selfEndpoint);
        context.setMemberList(new MemberList(new Member(selfEndpoint)));
        context.setTransporter(new DefaultTransporter(eventBus));
        return new MemberManagerImpl(context);
    }

}
