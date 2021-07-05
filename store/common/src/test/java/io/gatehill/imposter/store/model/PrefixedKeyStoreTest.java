package io.gatehill.imposter.store.model;

import io.gatehill.imposter.store.inmem.InMemoryStore;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PrefixedKeyStore}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class PrefixedKeyStoreTest {
    private InMemoryStore delegateStore;
    private PrefixedKeyStore store;

    @Before
    public void setUp() throws Exception {
        delegateStore = new InMemoryStore("test");
        store = new PrefixedKeyStore("pref.", delegateStore);
    }

    @Test
    public void testPrefixedKeys() {
        store.save("foo", "bar");
        assertTrue(delegateStore.hasItemWithKey("pref.foo"));
        assertEquals(store.load("foo"), "bar");

        // prefix should not be present in keys
        final Set<String> allKeys = store.loadAll().keySet();
        assertThat(allKeys, not(hasItem("pref.foo")));
        assertThat(allKeys, hasItem("foo"));
    }
}
