package io.gatehill.imposter.store.inmem;

import io.gatehill.imposter.store.model.Store;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for in-memory store implementation.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InMemoryStoreTest {
    private InMemoryStoreFactoryImpl factory;

    @Before
    public void setUp() {
        factory = new InMemoryStoreFactoryImpl();
    }

    @Test
    public void testBuildNewStore() {
        final Store store = factory.buildNewStore("test");
        assertEquals("inmem", store.getTypeDescription());
    }

    @Test
    public void testSaveLoadItem() {
        final Store store = factory.buildNewStore("sli");

        store.save("foo", "bar");
        assertEquals("bar", store.load("foo"));

        final Map<String, Object> allItems = store.loadAll();
        assertEquals(1, allItems.size());
        assertEquals("bar", allItems.get("foo"));
        assertTrue("Item should exist", store.hasItemWithKey("foo"));
        assertEquals(1, store.count());
    }

    @Test
    public void testDeleteItem() {
        final Store store = factory.buildNewStore("di");

        assertFalse("Item should not exist", store.hasItemWithKey("foo"));

        store.save("foo", "bar");
        assertTrue("Item should exist", store.hasItemWithKey("foo"));

        store.delete("foo");
        assertFalse("Item should not exist", store.hasItemWithKey("foo"));
    }
}
