package in.xnnyygn.xgossip;

import org.junit.Test;

import static org.junit.Assert.*;

public class MemberTest {

    @Test
    public void testDoesExist() {
        long now = System.currentTimeMillis();
        Member member = new Member(new MemberEndpoint("localhost", 5302), now);
        assertTrue(member.doesExist());
    }

}