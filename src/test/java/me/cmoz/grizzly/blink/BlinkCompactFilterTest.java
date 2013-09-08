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
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.Test;

import me.cmoz.grizzly.blink.hello_spec.Hello;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link me.cmoz.grizzly.blink.BlinkCompactFilter}.
 */
public class BlinkCompactFilterTest {

    /** The port for the local test. */
    private static final int PORT = 20389;

    @Test
    public void compactWriterTest() throws IOException, BlinkException {
        final Hello helloMessage = new Hello();
        helloMessage.setGreeting("Hello World!");

        final Schema schema = new Schema();
        SchemaReader.readFromString(
                "namespace HelloSpec\n" +
                "Hello/1 ->\n" +
                "  string Greeting", schema);
        schema.finalizeSchema();

        final DefaultObjectModel schemaModel =
                new DefaultObjectModel(schema, "me.cmoz.grizzly.blink");
        final ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();
        final CompactWriter compactWriter = new CompactWriter(schemaModel, outputStream);
        compactWriter.write(helloMessage);
        compactWriter.close();

        ByteBuf byteBuf = new ByteBuf(outputStream.toByteArray());

        assertEquals(
                "8e 00 01 0c 48 65 6c 6c 6f 20 57 6f 72 6c 64 21",
                byteBuf.toHexString());
    }

    @Test
    @SuppressWarnings("cast")
    public void compactReaderTest() throws IOException, BlinkException {
        final Schema schema = new Schema();
        SchemaReader.readFromString(
                "namespace HelloSpec\n" +
                "Hello/1 ->\n" +
                "  string Greeting", schema);
        schema.finalizeSchema();

        final DefaultObjectModel schemaModel =
                new DefaultObjectModel(schema, "me.cmoz.grizzly.blink");
        final CompactReader compactReader = new CompactReader(schemaModel);

        final String helloMessage =
                "8e 00 01 0c 48 65 6c 6c 6f 20 57 6f 72 6c 64 21"
                .replace(" ", "");

        ByteBuf byteBuf = new ByteBuf();
        byteBuf.write(DatatypeConverter.parseHexBinary(helloMessage));
        byteBuf.flip();

        DefaultBlock block = new DefaultBlock();
        compactReader.read(byteBuf, block);

        assertTrue(block.getObjects().get(0) instanceof Hello);
        assertEquals(
                "Hello World!",
                (String) ((Hello) block.getObjects().get(0)).getGreeting());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterMessageTest()
            throws IOException, BlinkException, InterruptedException, ExecutionException {
        final Schema schema = new Schema();
        SchemaReader.readFromString(
                "namespace HelloSpec\n" +
                "Hello/1 ->\n" +
                "  string Greeting", schema);
        schema.finalizeSchema();

        final Hello message = new Hello();
        message.setGreeting("Hello World!");

        final FilterChainBuilder serverFilterBuilder = FilterChainBuilder.stateless()
                .add(new TransportFilter())
                .add(new BlinkCompactFilter(schema, "me.cmoz.grizzly.blink"))
                .add(new BlinkCompactServerFilter(message));

        final NIOTransport transport = TCPNIOTransportBuilder.newInstance()
                .setProcessor(serverFilterBuilder.build())
                .build();

        Connection connection = null;
        try {
            transport.bind(PORT);
            transport.start();

            connection = transport.connect("localhost", PORT).get();

            final BlockingQueue<Object> resultQueue = DataStructures.getLTQInstance(Object.class);

            final FilterChainBuilder clientFilterBuilder = FilterChainBuilder.stateless()
                    .add(new TransportFilter())
                    .add(new BlinkCompactFilter(schema, "me.cmoz.grizzly.blink"))
                    .add(new BlinkCompactClientFilter(resultQueue));

            final FilterChain clientFilter = clientFilterBuilder.build();
            connection.setProcessor(clientFilter);

            connection.write(message).get();

            Object response = resultQueue.poll(10, TimeUnit.SECONDS);

            assertTrue(response instanceof Hello);
            assertEquals(message.getGreeting(), ((Hello) response).getGreeting());
        } finally {
            if (connection != null)
                connection.close();

            transport.stop();
        }
    }

    private static class BlinkCompactServerFilter extends BaseFilter {

        /** The message to send outbound. */
        private final Hello message;

        public BlinkCompactServerFilter(final Hello message) {
            this.message = message;
        }

        public NextAction handleRead(final FilterChainContext context)
                throws IOException {
            final Object object = context.getMessage();

            assertTrue(object instanceof Hello);
            assertEquals(this.message.getGreeting(), ((Hello) object).getGreeting());

            context.write(object);
            return context.getStopAction();
        }

    }

    private static class BlinkCompactClientFilter extends BaseFilter {

        /** A storage queue to send the read messages to. */
        private final BlockingQueue<Object> resultQueue;

        public BlinkCompactClientFilter(final BlockingQueue<Object> resultQueue) {
            this.resultQueue = resultQueue;
        }

        public NextAction handleRead(final FilterChainContext context)
                throws IOException {
            resultQueue.add(context.getMessage());
            return context.getStopAction();
        }

    }

}
