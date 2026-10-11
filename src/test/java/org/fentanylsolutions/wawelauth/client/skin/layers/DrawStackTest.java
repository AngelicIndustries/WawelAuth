package org.fentanylsolutions.wawelauth.client.skin.layers;

import org.junit.Assert;
import org.junit.Test;

public class DrawStackTest {

    @Test
    public void innermostDrawIsCurrent() {
        DrawStack<String> stack = new DrawStack<>();
        Assert.assertNull(stack.peek());
        stack.push("world");
        stack.push("first person");
        Assert.assertEquals("first person", stack.peek());
        Assert.assertTrue(stack.remove("first person"));
        Assert.assertEquals("world", stack.peek());
        Assert.assertTrue(stack.remove("world"));
        Assert.assertNull(stack.peek());
    }

    @Test
    public void closingAnOuterDrawDropsUnclosedInnerOnes() {
        DrawStack<String> stack = new DrawStack<>();
        stack.push("outer");
        stack.push("inner");
        Assert.assertFalse(stack.remove("outer"));
        Assert.assertEquals(0, stack.size());
        Assert.assertFalse(stack.remove("inner"));
    }

    @Test
    public void leakedDrawsAreDroppedAtTheLimit() {
        DrawStack<Integer> stack = new DrawStack<>();
        for (int i = 0; i < DrawStack.MAX_DEPTH; i++) Assert.assertTrue(stack.push(i));
        Assert.assertFalse(stack.push(99));
        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(Integer.valueOf(99), stack.peek());
    }
}
