package com.gatehill.imposter.script.impl;

import org.junit.Test;

import static org.junit.Assert.*;

public class InternalResponseBehaviorImplTest {

    @Test
    public void shouldAddHeaderWithValue() throws Exception {
        InternalResponseBehaviorImpl internalResponseBehavior = new InternalResponseBehaviorImpl();
        internalResponseBehavior.withHeader("MyHeader","MyValue");

        assertEquals(1,internalResponseBehavior.getResponseHeaders().size());
        assertTrue(internalResponseBehavior.getResponseHeaders().containsKey("MyHeader"));
        assertEquals("MyValue",internalResponseBehavior.getResponseHeaders().get("MyHeader"));
    }

    @Test
    public void shouldRemoveHeaderWithValueNull() throws Exception {
        InternalResponseBehaviorImpl internalResponseBehavior = new InternalResponseBehaviorImpl();
        internalResponseBehavior.getResponseHeaders().put("MyHeader","MyValue");

        internalResponseBehavior.withHeader("MyHeader",null);

        assertEquals(0,internalResponseBehavior.getResponseHeaders().size());
    }
}