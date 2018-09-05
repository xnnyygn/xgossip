package in.xnnyygn.xgossip;

import com.google.common.collect.ImmutableSet;
import in.xnnyygn.xgossip.updates.AbstractUpdate;
import in.xnnyygn.xgossip.updates.MemberJoinedUpdate;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateListTest {

    @Test
    public void testTake() {
        UpdateList list = new UpdateList(10);
        long updateId = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5302), System.currentTimeMillis()));
        List<AbstractUpdate> updates = list.take(2);
        assertEquals(1, updates.size());
        assertEquals(updateId, updates.get(0).getId());
        assertTrue(updates.get(0) instanceof MemberJoinedUpdate);
    }

    @Test
    public void testTakeSorted() {
        UpdateList list = new UpdateList(10);
        long updateId1 = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5302), System.currentTimeMillis()));
        long updateId2 = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5303), System.currentTimeMillis()));
        long updateId3 = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5304), System.currentTimeMillis()));
        list.decreaseUsefulness(updateId2);
        list.decreaseUsefulness(updateId3);
        list.decreaseUsefulness(updateId3);
        List<AbstractUpdate> updates = list.take(3);
        assertEquals(3, updates.size());
        assertEquals(updateId1, updates.get(0).getId());
        assertEquals(updateId2, updates.get(1).getId());
        assertEquals(updateId3, updates.get(2).getId());
    }

    @Test
    public void takeExcept() {
        UpdateList list = new UpdateList(10);
        long updateId1 = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5302), System.currentTimeMillis()));
        long updateId2 = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5303), System.currentTimeMillis()));
        long updateId3 = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5304), System.currentTimeMillis()));
        List<AbstractUpdate> updates = list.takeExcept(2, ImmutableSet.of(updateId1, updateId3));
        assertEquals(1, updates.size());
        assertEquals(updateId2, updates.get(0).getId());
    }

    @Test
    public void testIncrease() {
        UpdateList list = new UpdateList(1);
        long updateId = list.prepend(new MemberJoinedUpdate(new MemberEndpoint("localhost", 5302), System.currentTimeMillis()));
        List<AbstractUpdate> updates = list.take(1);
        assertEquals(1, updates.size());
        assertEquals(updateId, updates.get(0).getId());

        list.decreaseUsefulness(updateId);

        updates = list.take(1);
        assertEquals(0, updates.size());
    }

}