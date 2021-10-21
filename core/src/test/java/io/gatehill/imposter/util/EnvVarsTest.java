package io.gatehill.imposter.util;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

public class EnvVarsTest {
    @BeforeClass
    public static void beforeClass() {
        EnvVars.populate(new HashMap<String, String>() {{
            put("foo", "bar");
        }});
    }

    @Test
    public void testGetEnvSingle() {
        assertThat(EnvVars.getEnv("foo"), equalTo("bar"));
    }

    @Test
    public void testGetEnvAll() {
        final Map<String, String> entries = EnvVars.getEnv();
        assertThat(entries.entrySet(), hasSize(1));
        assertThat(entries, hasEntry("foo", "bar"));
    }
}