package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class SmtpRequestDecoderTest {
    @Test
    public void testEncodeEhlo() {
        testEncode("EHLO localhost\r\n", SmtpRequests.ehlo(AsciiString.of("localhost")));
    }

    @Test
    public void testEncodeHelo() {
        testEncode("HELO localhost\r\n", SmtpRequests.helo(AsciiString.of("localhost")));
    }

    @Test
    public void testEncodeMail() {
        testEncode("MAIL FROM:<me@netty.io>\r\n", SmtpRequests.mail(AsciiString.of("me@netty.io")));
    }

    @Test
    public void testEncodeMailNullSender() {
        testEncode("MAIL FROM:<>\r\n", SmtpRequests.mail(null));
    }

    @Test
    public void testEncodeRcpt() {
        testEncode("RCPT TO:<me@netty.io>\r\n", SmtpRequests.rcpt(AsciiString.of("me@netty.io")));
    }

    @Test
    public void testEncodeNoop() {
        testEncode("NOOP\r\n", SmtpRequests.noop());
    }

    @Test
    public void testEncodeRset() {
        testEncode("RSET\r\n", SmtpRequests.rset());
    }

    @Test
    public void testEncodeHelp() {
        testEncode("HELP\r\n", SmtpRequests.help(null));
    }

    @Test
    public void testEncodeHelpWithArg() {
        testEncode("HELP MAIL\r\n", SmtpRequests.help("MAIL"));
    }

    @Test
    public void testEncodeData() {
        testEncode("DATA\r\n", SmtpRequests.data());
    }

    @Test
    public void testEncodeDataAndContent() {
        EmbeddedChannel channel = new EmbeddedChannel(new SmtpRequestDecoder(Integer.MAX_VALUE));
        assertTrue(channel.writeInbound(Unpooled.copiedBuffer("DATA\r\n", CharsetUtil.US_ASCII)));
        assertTrue(channel.writeInbound(Unpooled.copiedBuffer("Subject: Test\r\n\r\n", CharsetUtil.US_ASCII)));
        assertTrue(channel.writeInbound(Unpooled.copiedBuffer("Test\r\n.\r\n", CharsetUtil.US_ASCII)));
        assertTrue(channel.finish());

        SmtpRequest request = channel.readInbound();
        LastSmtpContent content = channel.readInbound();

        assertEquals(SmtpCommand.DATA, request.command());
        final String cs = content.content().toString(0, content.content().readableBytes(), CharsetUtil.US_ASCII);
        assertEquals("Subject: Test\r\n\r\nTest\r\n", cs);
    }

    private static String getWrittenString(EmbeddedChannel channel) {
        ByteBuf written = Unpooled.buffer();

        for (;;) {
            Object buffer = channel.readInbound();
            if (buffer == null) {
                break;
            }
            //written.writeBytes(buffer);
            ReferenceCountUtil.release(buffer);
        }

        String writtenString = written.toString(CharsetUtil.US_ASCII);
        written.release();

        return writtenString;
    }

    private static void testEncode(String request, SmtpRequest expected) {
        final EmbeddedChannel channel = new EmbeddedChannel(new SmtpRequestDecoder(Integer.MAX_VALUE));
        assertTrue(channel.writeInbound(newBuffer(request)));
        assertTrue(channel.finish());

        SmtpRequest buffer = channel.readInbound();
        assertEquals(expected, buffer);
        ReferenceCountUtil.release(buffer);
        assertNull(channel.readInbound());
    }

    private static ByteBuf newBuffer(CharSequence seq) {
        return Unpooled.copiedBuffer(seq, CharsetUtil.US_ASCII);
    }

}
