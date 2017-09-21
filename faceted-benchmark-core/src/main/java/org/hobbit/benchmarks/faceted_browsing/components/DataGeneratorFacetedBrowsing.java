package org.hobbit.benchmarks.faceted_browsing.components;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.aksw.commons.collections.utils.StreamUtils;
import org.aksw.jena_sparql_api.utils.GraphUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.hobbit.core.Commands;
import org.hobbit.interfaces.DataGenerator;
import org.hobbit.interfaces.TripleStreamSupplier;
import org.hobbit.transfer.ChunkedProtocolWriter;
import org.hobbit.transfer.ChunkedProtocolWriterSimple;
import org.hobbit.transfer.OutputStreamChunkedTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Streams;

@Component
public class DataGeneratorFacetedBrowsing
    extends ComponentBase
    implements DataGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(DataGeneratorFacetedBrowsing.class);

    protected int batchSize = 10000;

    @Resource(name="commandChannel")
    protected WritableByteChannel commandChannel;

    @Resource
    protected TripleStreamSupplier tripleStreamSupplier;

    @Resource(name="dg2tg")
    protected WritableByteChannel toTaskGenerator;

    @Resource(name="dg2sa")
    protected WritableByteChannel toSystemAdatper;



    @Override
    public void init() throws Exception {
        logger.debug("Data generator init");

        commandPublisher.subscribe((buffer) -> {
            if(buffer.hasRemaining()) {
                byte cmd = buffer.get(0);
                if(cmd == Commands.DATA_GENERATOR_START_SIGNAL) {
                    try {
                        generateData();

                        try {
                            commandChannel.write(ByteBuffer.wrap(new byte[]{Commands.DATA_GENERATION_FINISHED}));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    } catch(Exception e) {
                         throw new RuntimeException(e);
                    }
                }
            }
        });
    }

//    @Override
//    public void receiveCommand(byte command, byte[] data) {
//        logger.debug("Seen command: " + command);
//        if(command == Commands.DATA_GENERATOR_START_SIGNAL) {
//            try {
//                generateData();
//            } catch(Exception e) {
//                 throw new RuntimeException(e);
//            }
//        }
//
////        if (command == (byte) 150 ) {
////            byte[] emptyByte = {};
////            try {
////                // tell the task generator to start
////                sendDataToTaskGenerator(emptyByte);
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////            generateTasks.release();
////        }
//    }


//        streamManager.handleIncomingData(ByteBuffer.wrap(data));


    // TODO Separate actual data generation from sending out platform specific events
    @Override
    public void generateData() throws Exception {
        logger.info("Data generator started.");

        // Generate the data once and store it in a temp file
        // TODO Add a flag whether to cache in a file
        File datasetFile = File.createTempFile("hobbit-data-generator-faceted-browsing", ".nt");
        datasetFile.deleteOnExit();
        Stream<Triple> triples = tripleStreamSupplier.get();
        RDFDataMgr.writeTriples(new FileOutputStream(datasetFile), triples.iterator());

        Supplier<Stream<Triple>> triplesFromCache = () -> {
            try {
                return Streams.stream(RDFDataMgr.createIteratorTriples(new FileInputStream(datasetFile), Lang.NTRIPLES, "http://example.org/"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };

        sendTriples(triplesFromCache.get(), batchSize, toTaskGenerator);
        sendTriples(triplesFromCache.get(), batchSize, toSystemAdatper);

        datasetFile.delete();
    }

    public static Entry<Long, Long> sendTriples(Stream<Triple> stream, int batchSize, WritableByteChannel channel) throws IOException {

        AtomicLong recordCount = new AtomicLong();
        AtomicLong batchCount = new AtomicLong();

        //ChunkedProtocolWriter protocol = new ChunkedProtocolWriterSimple(666);

        //Stream<Triple> stream = Streams.stream(it);
        try(OutputStream out = OutputStreamChunkedTransfer.newInstanceForByteChannel(channel, null)) {
            StreamUtils
                .mapToBatch(stream, batchSize)
                .peek(x -> batchCount.incrementAndGet())
                .map(GraphUtils::toMemGraph)
                .peek(graph -> recordCount.addAndGet(graph.size()))
                .map(ModelFactory::createModelForGraph)
                .map(batchModel -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    RDFDataMgr.write(baos, batchModel, RDFFormat.TURTLE);
                    byte[] r = baos.toByteArray();
                    return r;
                })
                .forEach(data -> {
                    //logger.info("CONVERTED MODEL TO BYTE. SENDING TO TASK");
                    //logger.info("byte array: " + data);

                    String graphURI = "http://www.example.com/graph";

                    try {
                        out.write(data);
                        //channel.write(ByteBuffer.wrap(data));
                        //sink.accept(RabbitMQUtils.writeByteArrays(null, new byte[][]{RabbitMQUtils.writeString(graphURI)}, data));
                        //sendDataToSystemAdapter(RabbitMQUtils.writeByteArrays(null, new byte[][]{RabbitMQUtils.writeString(graphURI)}, data));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            out.flush();
        }

        Entry<Long, Long> result = new SimpleEntry<>(recordCount.get(), batchCount.get());
        return result;
    }

    @Override
    public void close() throws IOException {
    }

}