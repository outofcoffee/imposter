package com.gatehill.imposter;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author pcornish
 */
public class BaseMockConfig {
    private String responseFile;
    private String viewName;

    public String getResponseFile() {
        return responseFile;
    }

    public String getViewName() {
        return viewName;
    }
}
