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

import com.pantor.blink.DefaultObjectModel;
import com.pantor.blink.ObjectModel;
import com.pantor.blink.Schema;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

import lombok.NonNull;

/**
 * A filter for the Blink Compact serialization format.
 *
 * See <a href="http://blinkprotocol.org/s/BlinkSpec-beta4.pdf">Blink Protocol Specification (PDF)</a>
 */
public final class BlinkCompactFilter extends AbstractCodecFilter<Buffer, Object> {

    /**
     * A Blink Compact filter that uses the supplied {@code schema} for
     * (de)serialization.
     *
     * @param schema The schema for the blink protocol definitions.
     */
    public BlinkCompactFilter(final @NonNull Schema schema) {
        this(new DefaultObjectModel(schema));
    }

    /**
     * A Blink Compact filter that uses the supplied {@code schema} and fully
     * qualified {@code packageName} for (de)serialization.
     *
     * @param schema The schema for the blink protocol definitions.
     * @param packageName The fully qualified package name for the generated
     *                    message classes.
     */
    public BlinkCompactFilter(
            final @NonNull Schema schema, final @NonNull String packageName) {
        this(new DefaultObjectModel(schema, packageName));
    }

    /**
     * A Blink Compact filter that uses the supplied {@code objectModel} for
     * (de)serialization.
     *
     * @param objectModel The set of mapping rules to (de)serialize POJOs as
     *                    messages.
     */
    public BlinkCompactFilter(final ObjectModel objectModel) {
        super(new BlinkCompactDecoder(objectModel), new BlinkCompactEncoder(objectModel));
    }

}
