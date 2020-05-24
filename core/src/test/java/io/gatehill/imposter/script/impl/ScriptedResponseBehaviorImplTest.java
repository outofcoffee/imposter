package io.gatehill.imposter.script.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScriptedResponseBehaviorImplTest {

    @Test
    public void shouldAddHeaderWithValue() throws Exception {
        ScriptedResponseBehaviorImpl scriptedResponseBehavior = new ScriptedResponseBehaviorImpl();
        scriptedResponseBehavior.withHeader("MyHeader", "MyValue");

        assertEquals(1, scriptedResponseBehavior.getResponseHeaders().size());
        assertTrue(scriptedResponseBehavior.getResponseHeaders().containsKey("MyHeader"));
        assertEquals("MyValue", scriptedResponseBehavior.getResponseHeaders().get("MyHeader"));
    }

    @Test
    public void shouldRemoveHeaderWithValueNull() throws Exception {
        ScriptedResponseBehaviorImpl scriptedResponseBehavior = new ScriptedResponseBehaviorImpl();
        scriptedResponseBehavior.getResponseHeaders().put("MyHeader", "MyValue");

        scriptedResponseBehavior.withHeader("MyHeader", null);

        assertEquals(0, scriptedResponseBehavior.getResponseHeaders().size());
    }
}