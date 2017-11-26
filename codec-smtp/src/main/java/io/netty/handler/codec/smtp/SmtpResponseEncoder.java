package io.netty.handler.codec.smtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class SmtpResponseEncoder extends MessageToMessageEncoder<Object> {

    private static final byte SP = ' ';
    private static final int CRLF_SHORT = ('\r' << 8) | '\n';
    private static final byte DASH = '-';

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return msg instanceof SmtpResponse;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if(!(msg instanceof SmtpResponse)) {
            return;
        }

        final SmtpResponse smtpResponse = (SmtpResponse) msg;
        final ByteBuf buffer = ctx.alloc().buffer();
        boolean release = true;
        try {
            CharSequence code = Integer.toString(smtpResponse.code(), 10);

            if (smtpResponse.details().size() <= 1) {
                final CharSequence detailsString;
                if(smtpResponse.details().isEmpty()) {
                    // TODO: This should throw an exception, as RFC requires this
                    detailsString = "";
                } else {
                    detailsString = smtpResponse.details().get(0);
                }
                writeSingleLineResponse(code, detailsString, buffer);
            } else {
                writeMultilineResponse(code, smtpResponse.details(), buffer);
            }
            out.add(buffer);
            release = false;
        } finally {
            if (release) {
                buffer.release();
            }
        }
    }

    private static void writeSingleLineResponse(final CharSequence code, final CharSequence details, final ByteBuf out) {
        ByteBufUtil.writeAscii(out, code);
        out.writeByte(SP);
        ByteBufUtil.writeAscii(out, details);
        ByteBufUtil.writeShortBE(out, CRLF_SHORT);
    }

    private static void writeMultilineResponse(
            final CharSequence code,
            final List<CharSequence> details,
            final ByteBuf out)
    {
        for (int i = 0; i < details.size() - 1; i++) {
            ByteBufUtil.writeAscii(out, code);
            out.writeByte(DASH);
            ByteBufUtil.writeAscii(out, details.get(i));
            ByteBufUtil.writeShortBE(out, CRLF_SHORT);
        }
        ByteBufUtil.writeAscii(out, code);
        out.writeByte(SP);
        ByteBufUtil.writeAscii(out, details.get(details.size() - 1));
        ByteBufUtil.writeShortBE(out, CRLF_SHORT);
    }
}
