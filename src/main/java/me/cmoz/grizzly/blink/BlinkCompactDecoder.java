/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.cmoz.grizzly.blink;

import com.pantor.blink.BlinkException;
import com.pantor.blink.CompactReader;
import com.pantor.blink.DefaultBlock;
import com.pantor.blink.ObjectModel;
import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import static org.glassfish.grizzly.TransformationResult.createErrorResult;

/**
 * Decodes {@code Blink Compact} messages from the input stream.
 */
@Slf4j
public class BlinkCompactDecoder extends AbstractTransformer<Buffer, Object> {

    /** The error code for a failure decoding the message header. */
    public static final int IO_BLINK_COMPACT_DECODE_HEADER = 0;
    /** The error code for a failed blink compact parse of a message. */
    public static final int IO_BLINK_COMPACT_PARSE_ERROR = 1;
    /** The name of the decoder attribute for the size of the message. */
    public static final String MESSAGE_LENGTH_ATTR =
            "grizzly-blink-message-length";

    /** The set of mapping rules to encode POJOs to messages from the schema. */
    private final ObjectModel objectModel;
    /** The attribute for the length of the message. */
    private final Attribute<Integer> messageLengthAttr;

    /**
     * A Blink Compact decoder that uses the supplied {@code objectModel} to
     * decode messages.
     *
     * @param objectModel The set of mapping rules to encode POJOs to messages
     *                    from the schema.
     */
    public BlinkCompactDecoder(final ObjectModel objectModel) {
        this.objectModel = objectModel;
        messageLengthAttr = attributeBuilder.createAttribute(MESSAGE_LENGTH_ATTR);
    }

    /** {@inheritDoc} */
    @Override
    protected TransformationResult<Buffer, Object> transformImpl(
            final AttributeStorage storage, final @NonNull Buffer input)
            throws TransformationException {
        log.debug("inputRemaining={}", input.remaining());

        Integer messageLength = messageLengthAttr.get(storage);
        if (messageLength == null) {
            log.debug("inputRemaining={}", input.remaining());
            input.mark();

            // decode varint32 message length header
            final int b = input.get();
            if ((b & 0x80) == 0) {
                messageLength = b + 1;  // retain the message header byte
            } else if ((b & 0x40) == 0) {
                messageLength = (input.get() << 6) | (b & 0x3f);
                messageLength += 2;     // retain the message header byte
            } else {
                int w = b & 0x3f;
                if (w > 4) {
                    final String msg = "Message length header is larger than 'u32'.";
                    log.warn(msg);
                    return createErrorResult(IO_BLINK_COMPACT_DECODE_HEADER, msg);
                }

                for (int i = 0; i < w; ++i) {
                    messageLength |= input.get(i + 1) << (i << 3);
                }
                messageLength += w + 1; // retain the message header byte
            }
            log.debug("inputRemaining={}", input.remaining());
            input.reset();
            log.debug("inputRemaining={}", input.remaining());

            log.debug("messageLength={}", messageLength);
            messageLengthAttr.set(storage, messageLength);
        }

        if (input.remaining() < messageLength) {
            return TransformationResult.createIncompletedResult(input);
        }

        final Object message;
        try {
            final byte[] buf = input.array();
            final int pos = input.position();
            log.debug("pos={}", pos);

            final DefaultBlock block = new DefaultBlock();
            final CompactReader reader = new CompactReader(objectModel);
            reader.read(buf, pos, messageLength, block);

            message = block.getObjects().get(0);
        } catch (final BlinkException e) {
            final String msg = "Error decoding blink compact message from input stream.";
            log.warn(msg, e);
            return createErrorResult(IO_BLINK_COMPACT_PARSE_ERROR, msg);
        }

        return TransformationResult.createCompletedResult(message, input);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return BlinkCompactDecoder.class.getName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasInputRemaining(
            final AttributeStorage storage, final @NonNull Buffer input) {
        return (input != null) && input.hasRemaining();
    }

}
