package org.librairy.service.graph.indexer;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.DelimitedTermFrequencyTokenFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.librairy.service.graph.metrics.JensenShannon;
import org.librairy.service.graph.model.Neighbor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class LuceneIndexer implements Indexer {

    private static final Logger LOG = LoggerFactory.getLogger(LuceneIndexer.class);

    private static FSDirectory directory;
    private final IndexWriter writer;
    private final Double threshold;
    private IndexReader reader;
    private final AtomicInteger counter;
    private Double epsylon;
    private Double multiplicationFactor;
    private Integer dimensions;

    public LuceneIndexer(Double threshold) {

        File indexFile = null;
        try {
            indexFile = File.createTempFile("graph-edges-" + System.currentTimeMillis(), "csv.gz");
            if (indexFile.exists()) indexFile.delete();
            indexFile.getParentFile().mkdirs();
            IndexWriterConfig writerConfig = new IndexWriterConfig(new DocTopicAnalyzer());
            writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            writerConfig.setRAMBufferSizeMB(500.0);
            this.threshold = threshold;
            this.directory = FSDirectory.open(indexFile.toPath());
            this.writer = new IndexWriter(directory, writerConfig);
            this.counter = new AtomicInteger();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String vector2string(List<Double> vector){
        if (dimensions == null){
            dimensions = vector.size();
            epsylon = 1.0 / dimensions;
            multiplicationFactor = Double.valueOf(1*Math.pow(10,String.valueOf(dimensions).length()+1));
        }

        String result = "";
        for(int i=0; i<vector.size();i++){
            int freq = (int) (vector.get(i) * multiplicationFactor);
            if(freq > (epsylon*multiplicationFactor)){
                result += i + "|" + freq + " ";
            }
        }
        return result;
    }

    private List<Double> string2vector(String vectorAsString){
        if (Strings.isNullOrEmpty(vectorAsString)) return Collections.emptyList();
        String[] topics = vectorAsString.split(" ");

        Double[] vector = new Double[dimensions];
        Arrays.fill(vector,(double)epsylon);
        for(int i=0; i<topics.length;i++){
            int id      = Integer.valueOf(StringUtils.substringBefore(topics[i],"|"));
            int freq    = Integer.valueOf(StringUtils.substringAfter(topics[i],"|"));
            Double score = Double.valueOf(freq) / Double.valueOf(multiplicationFactor);
            vector[id] = score;
        }
        return Arrays.asList(vector);
    }

    private Document newDocument(String id, String topics){
        Document luceneDoc = new Document();
        // id
        luceneDoc.add(new TextField("id", id, Field.Store.YES));

        // doc-topic
        FieldType fieldType = new FieldType(TextField.TYPE_STORED);//TYPE_NOT_STORED
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setStoreTermVectorPositions(false);
        fieldType.setStoreTermVectorOffsets(false);
        fieldType.setStoreTermVectors(true);

        Field textField = new Field("vector", topics, fieldType);
        luceneDoc.add(textField);


        return luceneDoc;
    }

    @Override
    public void add(String label, List<Double> vector) {

            try {
                String stringTopics = vector2string(vector);
                Document luceneDoc = newDocument(label, stringTopics);
                writer.addDocument(luceneDoc);


                if (this.counter.incrementAndGet() % 100 == 0 ) {
                        this.writer.commit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

    }

    @Override
    public List<Neighbor> getNeighbours(String label, Integer num) {
            try {
                if (reader == null){
                    this.writer.commit();
                    this.writer.close();

                    reader= DirectoryReader.open(directory);
                }

                IndexSearcher searcher = new IndexSearcher(reader);

                // Search reference vector
                QueryParser parser = new QueryParser("id", new DocTopicAnalyzer());
                Query query = parser.parse(label);
                TopDocs res1 = searcher.search(query, 1);
                if (res1.totalHits<1) return Collections.emptyList();

                Document idoc = reader.document(res1.scoreDocs[0].doc);
                String queryVector = String.format(idoc.get("vector"));

                // More Like This
                MoreLikeThis mlt = new MoreLikeThis(reader);
                mlt.setMinTermFreq(1);
                mlt.setMinDocFreq(1);
                mlt.setAnalyzer(new DocTopicAnalyzer());

                Reader stringReader = new StringReader(queryVector);

                final List<Double> v1 = string2vector(queryVector);
                Query mltQuery = mlt.like("vector", stringReader);

                TopDocs results = searcher.search(mltQuery, reader.numDocs());
                LOG.debug("Total Hits: " + results.totalHits);
                List<Neighbor> topDocs = Arrays.stream(results.scoreDocs).parallel().map(scoreDoc -> {
                    try {
                        org.apache.lucene.document.Document docIndexed = reader.document(scoreDoc.doc);
                        String vectorString = String.format(docIndexed.get("vector"));
                        if (Strings.isNullOrEmpty(vectorString)) return new Neighbor("",0.0);

                        String id = String.format(docIndexed.get("id"));
                        if (id.equalsIgnoreCase(label)) return new Neighbor("",0.0);

                        List<Double> v2 = string2vector(vectorString);
                        return new Neighbor(id, JensenShannon.similarity(v1, v2));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new Neighbor("",0.0);
                    }
                }).filter(s -> s.getScore() > threshold).sorted((a, b) -> -a.getScore().compareTo(b.getScore())).limit(num).collect(Collectors.toList());

                return topDocs;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    public class DocTopicAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String s) {
            Tokenizer tokenizer = new WhitespaceTokenizer();
            TokenFilter filters = new DelimitedTermFrequencyTokenFilter(tokenizer);
            return new TokenStreamComponents(tokenizer, filters);
        }

    }
}
