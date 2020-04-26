package io.gatehill.imposter.plugin.openapi.model;

import com.google.common.base.Preconditions;

/**
 * Holds an object of a given content type.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ContentTypedHolder<T> {
    private String contentType;
    private T value;

    public ContentTypedHolder(String contentType, T value) {
        Preconditions.checkNotNull(contentType, "Content type cannot be null");
        this.contentType = contentType;
        this.value = value;
    }

    public String getContentType() {
        return contentType;
    }

    public T getValue() {
        return value;
    }
}
