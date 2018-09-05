package in.xnnyygn.xgossip;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class MemberListTest {

    @Test
    public void testAddNewMember() {
        MemberList list = new MemberList(new MemberEndpoint("localhost", 5302), System.currentTimeMillis());
        MemberList.UpdateResult result = list.add(new MemberEndpoint("localhost", 5303), System.currentTimeMillis());
        assertTrue(result.isUpdated());
    }

    @Test
    public void testAddNoUpdate() {
        MemberEndpoint endpoint = new MemberEndpoint("localhost", 5302);
        MemberList list = new MemberList(endpoint, System.currentTimeMillis());
        long now = System.currentTimeMillis();
        MemberList.UpdateResult result = list.add(endpoint, now);
        assertFalse(result.isUpdated());
    }

    @Test
    public void testAddAgain() {
        long now = System.currentTimeMillis();
        MemberEndpoint endpoint = new MemberEndpoint("localhost", 5302);
        MemberList list = new MemberList(endpoint, now);
        MemberList.UpdateResult result = list.add(endpoint, now + 1);
        assertTrue(result.isUpdated());
    }

    @Test
    public void testGetRandomEndpointExcept() {
        MemberEndpoint endpoint1 = new MemberEndpoint("localhost", 5302);
        MemberList list = new MemberList(endpoint1, System.currentTimeMillis());
        MemberEndpoint endpoint2 = new MemberEndpoint("localhost", 5303);
        list.add(endpoint2, System.currentTimeMillis());

        MemberEndpoint randomEndpoint = list.getRandomEndpointExceptSelf();
        assertEquals(endpoint2, randomEndpoint);

        randomEndpoint = list.getRandomEndpointExcept(ImmutableSet.of(endpoint1, endpoint2));
        assertNull(randomEndpoint);
    }

    @Test
    public void testGetRandomEndpointsExcept() {
        MemberEndpoint endpoint1 = new MemberEndpoint("localhost", 5302);
        MemberList list = new MemberList(endpoint1, System.currentTimeMillis());
        MemberEndpoint endpoint2 = new MemberEndpoint("localhost", 5303);
        list.add(endpoint2, System.currentTimeMillis());

        Collection<MemberEndpoint> endpoints = list.getRandomEndpointsExcept(2, Collections.singleton(endpoint1));
        assertEquals(1, endpoints.size());
        assertEquals(endpoint2, endpoints.iterator().next());
    }

    @Test
    public void testGetAll() {
        MemberEndpoint endpoint = new MemberEndpoint("localhost", 5302);
        MemberList list = new MemberList(endpoint, System.currentTimeMillis());
        Collection<Member> members = list.getAll();
        assertEquals(1, members.size());
        assertEquals(endpoint, members.iterator().next().getEndpoint());
    }

}