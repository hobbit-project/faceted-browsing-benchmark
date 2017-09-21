package org.hobbit.benchmarks.faceted_browsing.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hobbit.transfer.Publisher;

public class ByteChannelUtils {
    public static CompletableFuture<ByteBuffer> sendMessageAndAwaitResponse(WritableByteChannel dataChannel, ByteBuffer msg, Collection<Publisher<ByteBuffer>> publishers, Predicate<ByteBuffer> responseCondition) throws IOException {
        CompletableFuture<ByteBuffer> result = PublisherUtils.awaitMessage(publishers, responseCondition);

        // TODO By awaiting the message first, we may mistake a message for a response despite not
        // having sent the request

        try {
            dataChannel.write(msg);
        } catch(Exception e) {
            result.completeExceptionally(e);
            throw new RuntimeException(e);
        }

        return result;
    }

    public static WritableByteChannel wrapConsumer(Consumer<? super ByteBuffer> consumer) {
        return new WritableByteChannel() {
            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                int r = src.remaining();
                consumer.accept(src);
                return r;
            }
        };
    }

}