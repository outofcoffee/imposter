package io.gatehill.imposter.script.impl;

import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReadWriteResponseBehaviourImplTest {

    @Test
    public void shouldAddHeaderWithValue() throws Exception {
        ReadWriteResponseBehaviourImpl scriptedResponseBehavior = new ReadWriteResponseBehaviourImpl();
        scriptedResponseBehavior.withHeader("MyHeader", "MyValue");

        assertEquals(1, scriptedResponseBehavior.getResponseHeaders().size());
        assertTrue(scriptedResponseBehavior.getResponseHeaders().containsKey("MyHeader"));
        assertEquals("MyValue", scriptedResponseBehavior.getResponseHeaders().get("MyHeader"));
    }

    @Test
    public void shouldRemoveHeaderWithValueNull() throws Exception {
        ReadWriteResponseBehaviourImpl scriptedResponseBehavior = new ReadWriteResponseBehaviourImpl();
        scriptedResponseBehavior.getResponseHeaders().put("MyHeader", "MyValue");

        scriptedResponseBehavior.withHeader("MyHeader", null);

        assertEquals(0, scriptedResponseBehavior.getResponseHeaders().size());
    }
}