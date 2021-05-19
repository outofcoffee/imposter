package io.gatehill.imposter.script.impl;

import io.gatehill.imposter.script.MutableResponseBehaviourImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MutableResponseBehaviourImplTest {

    @Test
    public void shouldAddHeaderWithValue() throws Exception {
        MutableResponseBehaviourImpl scriptedResponseBehavior = new MutableResponseBehaviourImpl();
        scriptedResponseBehavior.withHeader("MyHeader", "MyValue");

        assertEquals(1, scriptedResponseBehavior.getResponseHeaders().size());
        assertTrue(scriptedResponseBehavior.getResponseHeaders().containsKey("MyHeader"));
        assertEquals("MyValue", scriptedResponseBehavior.getResponseHeaders().get("MyHeader"));
    }

    @Test
    public void shouldRemoveHeaderWithValueNull() throws Exception {
        MutableResponseBehaviourImpl scriptedResponseBehavior = new MutableResponseBehaviourImpl();
        scriptedResponseBehavior.getResponseHeaders().put("MyHeader", "MyValue");

        scriptedResponseBehavior.withHeader("MyHeader", null);

        assertEquals(0, scriptedResponseBehavior.getResponseHeaders().size());
    }
}