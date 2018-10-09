package org.librairy.service.graph.model;

import org.junit.Ignore;
import org.junit.Test;
import org.librairy.service.graph.indexer.LuceneIndexer;
import org.librairy.service.graph.indexer.MemoryIndexer;
import org.librairy.service.graph.utils.ReaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class GraphTest {

    private static final Logger LOG = LoggerFactory.getLogger(GraphTest.class);


    @Test
    public void inMemory() throws IOException {

        Graph graph = new Graph("t1-lucene", new MemoryIndexer(0.5));
        test(graph);
    }

    @Test
    @Ignore
    public void inLucene() throws IOException {

        Graph graph = new Graph("t1-memory", new LuceneIndexer(0.5));
        test(graph);
    }


    private void test(Graph graph) throws IOException {
        BufferedReader reader = ReaderUtils.from("src/test/resources/doctopics.csv.gz");
        String line;
        int counter = 0;
        while((line = reader.readLine()) != null){

            String[] values = line.split(",");
            String id = values[0];
            List<Double> vector = IntStream.range(1, values.length).mapToDouble(i -> Double.valueOf(values[i])).boxed().collect(Collectors.toList());
            graph.addNode(id,vector);
            counter++;
            if (counter == 100000) break;
        }
        graph.build(10);
        graph.saveTo(Paths.get("graphs"));
    }
}
