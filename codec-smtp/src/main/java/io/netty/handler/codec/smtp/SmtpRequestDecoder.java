package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.UnstableApi;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class SmtpRequestDecoder extends LineBasedFrameDecoder {

    private boolean contentExpected;
    private List<CharSequence> contentLines;

    private static final byte SP = ' ';
    private static final ByteBuf CRLF_BUFFER = Unpooled.unreleasableBuffer(
            Unpooled.directBuffer(2).writeByte('\r').writeByte('\n'));
    private static final ByteBuf DOT_CRLF_BUFFER = Unpooled.unreleasableBuffer(
            Unpooled.directBuffer(3).writeByte('.').writeByte('\r').writeByte('\n'));

    /**
     * {@inheritDoc}
     * @param maxLength
     */
    public SmtpRequestDecoder(int maxLength) {
        super(maxLength, false, false);
    }

    @Override
    protected Object decode(final ChannelHandlerContext ctx, final ByteBuf buffer) throws Exception {
        final ByteBuf frame = (ByteBuf) super.decode(ctx, buffer);
        if (frame == null) {
            return null;
        }

        try {
            if (!contentExpected) {
                return decodeSmtpRequestAndSetContentExpected(frame);
            } else {
                return decodeSmtpContent(frame);
            }
        } finally {
            frame.release();
        }
    }

    /**
     * Decodes the frame to SMTP request. If there is further data expected, sets the data expected processing flag
     * and creates an array for storing the incoming lines.
     *
     * @param frame {@link ByteBuf} which should already be decoded by the {@link LineBasedFrameDecoder}.
     * @return {@link SmtpRequest} containing the {@link SmtpCommand} received and parameters as {@link List} of
     *         {@link CharSequence}.
     * @throws DecoderException when the incoming frame contains less than 4 characters (which is the size of all SMTP
     *                          commands.
     */
    private SmtpRequest decodeSmtpRequestAndSetContentExpected(final ByteBuf frame) throws DecoderException {
        if (frame.readableBytes() < 4) {
            throw newDecoderException(frame, frame.readerIndex(), frame.readableBytes());
        }
        SmtpCommand command = decodeCommand(frame.copy(0, 4));
        List<CharSequence> parameters = decodeParameters(frame.copy(4, frame.readableBytes() - 4));

        if (command.isContentExpected()) {
            contentExpected = true;
            contentLines = new ArrayList<CharSequence>();
        }
        return new DefaultSmtpRequest(command, parameters);
    }

    /**
     * Decodes the SMTP content to a
     *
     * @param frame
     * @return
     */
    private DefaultLastSmtpContent decodeSmtpContent(final ByteBuf frame) {
        // TODO: this comparison can result in incorrect results, as we are only validating the LAST LINE to be .\r\n
        if (3 == frame.readableBytes() && (0 == frame.compareTo(DOT_CRLF_BUFFER))){
            CompositeByteBuf buf = Unpooled.compositeBuffer();
            for (CharSequence cs : this.contentLines) {
                buf.writeCharSequence(cs, CharsetUtil.US_ASCII);
            }
            return new DefaultLastSmtpContent(buf);
        }
        CharSequence cs = frame.getCharSequence(0, frame.readableBytes(), CharsetUtil.US_ASCII);
        this.contentLines.add(AsciiString.of(cs));
        return null;
    }

    private static DecoderException newDecoderException(ByteBuf buffer, int readerIndex, int readable) {
        return new DecoderException("Received invalid line: '"
                + buffer.toString(readerIndex, readable, CharsetUtil.US_ASCII) + "'");
    }

    private static SmtpCommand decodeCommand(final ByteBuf buf) {
        if (buf.isReadable(4)) {
            return SmtpCommand.valueOf(buf.getCharSequence(0, 4, CharsetUtil.US_ASCII));
        }
        throw newDecoderException(buf, buf.readerIndex(), buf.readableBytes());
    }

    private static List<CharSequence> decodeParameters(final ByteBuf buf) {
        List<CharSequence> ret = new ArrayList<CharSequence>(4);

        if (0 == buf.compareTo(CRLF_BUFFER)) {
            return ret;
        }

        int startIndex = 0;
        int index = 0;

        do {
            index = buf.indexOf(startIndex, buf.readableBytes(), SP);
            if (index == startIndex) {
                startIndex++;
            } else {
                if (index == -1) {
                    index = buf.readableBytes() - startIndex;
                }
                final CharSequence cs = buf.getCharSequence(startIndex, index, CharsetUtil.US_ASCII);
                ret.add(AsciiString.of(cs).trim());
                startIndex = index + 1;
            }
        } while (startIndex < buf.readableBytes());

        return ret;
    }
}
