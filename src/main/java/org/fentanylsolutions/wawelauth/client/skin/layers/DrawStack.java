package org.fentanylsolutions.wawelauth.client.skin.layers;

import java.util.ArrayDeque;

/** Open draws, innermost on top. Not thread-safe; client thread only. */
final class DrawStack<T> {

    /** A host that forgets to close its draws would otherwise grow this forever. */
    static final int MAX_DEPTH = 16;

    private final ArrayDeque<T> stack = new ArrayDeque<>();

    /** Returns false when the stack overflowed and was reset first, which means a host leaked draws. */
    boolean push(T draw) {
        boolean healthy = stack.size() < MAX_DEPTH;
        if (!healthy) stack.clear();
        stack.push(draw);
        return healthy;
    }

    T peek() {
        return stack.peek();
    }

    /**
     * Removes a draw. If draws opened after it are still open, they are dropped with it and false is returned, so a
     * host that closes out of order cannot leave stale draws behind.
     */
    boolean remove(T draw) {
        if (stack.peek() == draw) {
            stack.pop();
            return true;
        }
        if (!stack.contains(draw)) return false;
        while (stack.pop() != draw) {
            // Drop the draws opened inside it.
        }
        return false;
    }

    int size() {
        return stack.size();
    }
}
