package in.xnnyygn.xgossip;

import in.xnnyygn.xgossip.rpc.Transporter;
import in.xnnyygn.xgossip.schedule.Scheduler;
import in.xnnyygn.xgossip.support.MessageDispatcher;

import java.util.ArrayList;
import java.util.List;

class MemberListContext {

    private final UpdateList updateList = new UpdateList(5);
    private final NotificationList notificationList = new NotificationList(5);
    private final List<MemberEventListener> memberEventListeners = new ArrayList<>();
    private MemberList memberList;
    private Scheduler scheduler;
    private MessageDispatcher messageDispatcher;
    private Transporter transporter;
    private MemberEndpoint selfEndpoint;
    private long timeStarted;

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

    NotificationList getNotificationList() {
        return notificationList;
    }

    void setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
    }

    long getTimeStarted() {
        return timeStarted;
    }

    void addListener(MemberEventListener listener) {
        memberEventListeners.add(listener);
    }

    void notifyChangeToListeners(MemberEvent event) {
        for (MemberEventListener listener : memberEventListeners) {
            listener.onChanged(event);
        }
    }

    void notifyMergeToListeners() {
        for (MemberEventListener listener : memberEventListeners) {
            listener.onMerged();
        }
    }

}
