package io.gatehill.imposter.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link HttpUtil}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HttpUtilTest {
    @Test
    public void readAcceptedContentTypes() throws Exception {
        // note: provided out of order, but should be sorted by 'q' weight
        final String acceptHeader = "text/html; q=1.0, text/*; q=0.8, image/jpeg; q=0.5, image/gif; q=0.7, image/*; q=0.4, */*; q=0.1";

        final List<String> actual = HttpUtil.readAcceptedContentTypes(acceptHeader);
        assertNotNull(actual);
        assertEquals(6, actual.size());

        // check order
        assertEquals("text/html", actual.get(0));
        assertEquals("text/*", actual.get(1));
        assertEquals("image/gif", actual.get(2));
        assertEquals("image/jpeg", actual.get(3));
        assertEquals("image/*", actual.get(4));
        assertEquals("*/*", actual.get(5));
    }
}
