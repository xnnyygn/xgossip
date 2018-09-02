package in.xnnyygn.xgossip;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xgossip.updates.UpdateList;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

class MemberListContext {

    private final UpdateList updateList = new UpdateList(10);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final LatencyRecorder latencyRecorder = new LatencyRecorder();
    private MemberList memberList;
    private EventBus eventBus;
    private MemberEndpoint selfEndpoint;
    private Transporter transporter;

    MemberList getMemberList() {
        return memberList;
    }

    public void setMemberList(MemberList memberList) {
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

    ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    EventBus getEventBus() {
        return eventBus;
    }

    public LatencyRecorder getLatencyRecorder() {
        return latencyRecorder;
    }

}
