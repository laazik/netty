/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SmtpResponseEncoderTest {

    @Test
    public void testDecodeOneLineResponse() {
        final SmtpResponse resp = new DefaultSmtpResponse(200, "Ok");
        testEncode(resp, "200 Ok\r\n");
    }

    @Test
    public void testDecodeOneLineResponseNoDetails() {
        final SmtpResponse resp = new DefaultSmtpResponse(250);
        testEncode(resp, "250 \r\n");
    }

    @Test
    public void testDecodeTwoLineResponse() {
        final List<CharSequence> details = new ArrayList<CharSequence>();
        details.add("Hello");
        details.add("Ok");
        final SmtpResponse resp = new DefaultSmtpResponse(200, details);
        testEncode(resp, "200-Hello\r\n200 Ok\r\n");
    }

    private static void testEncode(final SmtpResponse response, final String expected) {
        final EmbeddedChannel channel = new EmbeddedChannel(new SmtpResponseEncoder());
        assertTrue(channel.writeOutbound(response));
        assertTrue(channel.finish());
        final ByteBuf buffer = channel.readOutbound();
        final String res =  buffer.toString(CharsetUtil.US_ASCII);
        assertEquals(expected, res);
        buffer.release();
        assertNull(channel.readOutbound());
    }
}
