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

import com.pantor.blink.*;
import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.BufferOutputStream;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static org.glassfish.grizzly.TransformationResult.createCompletedResult;
import static org.glassfish.grizzly.TransformationResult.createErrorResult;

/**
 * Encodes {@code Blink Compact} messages to the output stream.
 */
@Slf4j
public class BlinkCompactEncoder extends AbstractTransformer<Object, Buffer> {

    /** The error code for a failed write to the output stream. */
    public static final int IO_WRITE_ERROR = 0;

    /** The set of mapping rules to encode POJOs to messages from the schema. */
    private final ObjectModel objectModel;

    /**
     * A Blink Compact encoder that uses the supplied {@code schema} and
     * (optional) {@code packageName} to encode messages.
     *
     * @param objectModel The set of mapping rules to encode POJOs to messages
     *                    from the schema.
     */
    public BlinkCompactEncoder(final ObjectModel objectModel) {
        this.objectModel = objectModel;
    }

    /** {@inheritDoc} */
    @Override
    protected TransformationResult<Object, Buffer> transformImpl(
            final AttributeStorage storage, final @NonNull Object input)
            throws TransformationException {
        final MemoryManager memoryManager = obtainMemoryManager(storage);
        final BufferOutputStream outputStream = new BufferOutputStream(memoryManager);

        final CompactWriter writer = new CompactWriter(objectModel, outputStream);
        try {
            writer.write(input);
            writer.close();
        } catch (final BlinkException e) {
            final String msg = "Error encoding blink compact message to output stream.";
            log.warn(msg, e);
            return createErrorResult(IO_WRITE_ERROR, msg);
        } catch (final IOException e) {
            final String msg = "Error writing blink compact message to output stream.";
            log.warn(msg, e);
            return createErrorResult(IO_WRITE_ERROR, msg);
        }

        return createCompletedResult(outputStream.getBuffer().flip(), null);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return BlinkCompactEncoder.class.getName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasInputRemaining(
            final AttributeStorage storage, final Object input) {
        return (input != null);
    }

}
