package in.xnnyygn.xgossip;

import org.junit.Before;

import static org.junit.Assert.*;

public class FailureDetectorTest {


    private MemberListContext context;
    private FailureDetector detector;

    @Before
    public void setUp() {
        MemberEndpoint selfEndpoint = new MemberEndpoint("localhost", 5302);

        context = new MemberListContext();
        context.setTransporter(new MockTransporter());
        context.setSelfEndpoint(selfEndpoint);
        context.setMemberList(new MemberList(new Member(selfEndpoint)));

        detector = new FailureDetector(context);
    }

}