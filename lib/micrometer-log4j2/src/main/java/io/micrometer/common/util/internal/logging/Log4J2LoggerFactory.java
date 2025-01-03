/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.micrometer.common.util.internal.logging;

import org.apache.logging.log4j.LogManager;

/**
 * NOTE: This file has been copied and slightly modified from
 * {io.netty.util.internal.logging}.
 * <p>
 * Logger factory which creates a <a href="https://logging.apache.org/log4j/2.x/">Log4J2</a> logger.
 */
public final class Log4J2LoggerFactory extends InternalLoggerFactory {

    public static final InternalLoggerFactory INSTANCE = new Log4J2LoggerFactory();

    @Override
    public InternalLogger newInstance(String name) {
        return new Log4J2Logger(LogManager.getLogger(name));
    }
}
