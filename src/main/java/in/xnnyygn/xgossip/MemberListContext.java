package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.Transporter;
import in.xnnyygn.xgossip.updates.UpdateList;

class MemberListContext {

    private final UpdateList updateList = new UpdateList(5);
    private Scheduler scheduler;
    private MemberList memberList;
    private MessageDispatcher messageDispatcher;
    private MemberEndpoint selfEndpoint;
    private Transporter transporter;

    MemberList getMemberList() {
        return memberList;
    }

    void setMemberList(MemberList memberList) {
        this.memberList = memberList;
    }

    MemberEndpoint getSelfEndpoint() {
        return selfEndpoint;
    }

    void setSelfEndpoint(MemberEndpoint selfEndpoint) {
        this.selfEndpoint = selfEndpoint;
    }

    Transporter getTransporter() {
        return transporter;
    }

    void setTransporter(Transporter transporter) {
        this.transporter = transporter;
    }

    UpdateList getUpdateList() {
        return updateList;
    }

    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    Scheduler getScheduler() {
        return scheduler;
    }

    MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    void setMessageDispatcher(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

}
