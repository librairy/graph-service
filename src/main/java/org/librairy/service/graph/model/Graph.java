package org.librairy.service.graph.model;

import org.apache.commons.io.FileUtils;
import org.librairy.service.graph.facade.model.*;
import org.librairy.service.graph.indexer.LuceneIndexer;
import org.librairy.service.graph.indexer.MemoryIndexer;
import org.librairy.service.graph.indexer.Indexer;
import org.librairy.service.graph.utils.ReaderUtils;
import org.librairy.service.graph.utils.WriterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Graph {

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);

    private static final String NODES_FILE = "nodes.csv.gz";
    private static final String EDGES_FILE = "edges.csv.gz";
    private String creationTime;
    private BufferedWriter nodeWriter;
    private BufferedWriter edgeWriter;


    private String id;
    private Indexer index;
    private String description = "";
    private String query = "";
    private Double threshold = 0.5;

    private File tmpNodeFile;
    private File tmpEdgeFile;

    private static final String SEPARATOR = ",";


    AtomicInteger nodeCounter = new AtomicInteger();
    AtomicInteger edgeCounter = new AtomicInteger();

    public Graph() {
        initialize("unknown", new LuceneIndexer(0.5));
    }

    public Graph(String id) {
        initialize(id, new LuceneIndexer(0.5));
    }

    public Graph(String id, Indexer indexer) {
        initialize(id, indexer);
    }

    private void initialize(String id, Indexer indexer){
        try {
            this.id = id;
            this.index = indexer;

            this.tmpNodeFile = File.createTempFile("graph-nodes-"+System.currentTimeMillis(),"csv.gz");
            this.nodeWriter = WriterUtils.to(tmpNodeFile.getAbsolutePath());

            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            this.creationTime = df.format(new Date());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addNode(String id, List<Double> vector){
        try {
            this.index.add(id,vector);
            this.nodeCounter.incrementAndGet();
            this.nodeWriter.write(id+"\n");
            if (nodeCounter.get() % 100 == 0 ) LOG.info(nodeCounter.get() + " nodes added");
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void build(Integer max){
        try {
            // save node file
            this.nodeWriter.close();

            // create an edge file
            this.tmpEdgeFile = File.createTempFile("graph-edges-"+System.currentTimeMillis(),"csv.gz");
            this.edgeWriter = WriterUtils.to(tmpEdgeFile.getAbsolutePath());


            BufferedReader nodeReader = ReaderUtils.from(tmpNodeFile.getAbsolutePath());
            String node;
            int counter = 0;
            int interval = nodeCounter.get() > 100? nodeCounter.get()/100 : 1;
            while((node = nodeReader.readLine()) != null){
                final String refNode = node;
                List<Neighbor> neighbours = index.getNeighbours(node, max);
                neighbours.forEach(neighbour -> {
                    try {
                        edgeCounter.incrementAndGet();
                        this.edgeWriter.write(refNode+ SEPARATOR + neighbour.getNode() + SEPARATOR + neighbour.getScore() + "\n");
                    } catch (IOException e) {
                        LOG.warn("Error writing relationships",e);
                    }
                });
                counter ++;
                if (counter % interval == 0) LOG.info("Calculated similarities for " + counter + " nodes of " + nodeCounter.get());

            }
            this.edgeWriter.close();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public Summary getSummary() {
        Summary summary = new Summary();
        summary.setDescription(this.description);
        summary.setId(this.id);
        summary.setSize(nodeCounter.get()+"x"+edgeCounter.get());
        summary.setCreationTime(this.creationTime);
        return summary;
    }


    public Info getInfo() {
        Info info = new Info();
        info.setId(this.id);
        info.setDescription(this.description);
        info.setNumNodes(nodeCounter.get());
        info.setNumEdges(edgeCounter.get());
        info.setQuery(this.query);
        return info;
    }

    public void setNumNodes(Integer num){
        this.nodeCounter.set(num);
    }

    public void setNumEdges(Integer num){
        this.edgeCounter.set(num);
    }

    public void saveTo(Path dir){

        File graphDir = Paths.get(dir.toFile().getAbsolutePath(), id).toFile();
        graphDir.mkdir();
        Path path = graphDir.toPath();

        try {
            // Save Node File
            File out1 = Paths.get(path.toFile().getAbsolutePath(), NODES_FILE).toFile();
            if (out1.exists()) out1.delete();
            FileUtils.moveFile(tmpNodeFile, out1);

            // Save Edge File
            File out2 = Paths.get(path.toFile().getAbsolutePath(), EDGES_FILE).toFile();
            if (out2.exists()) out2.delete();
            FileUtils.moveFile(tmpEdgeFile, out2);

            // Save Summary
            getSummary().saveTo(path);

            // Save Info
            getInfo().saveTo(path);

            LOG.info("Graph saved successfully at " + graphDir.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public String getDescription() {
        return description;
    }

    public String getQuery() {
        return query;
    }

    public AtomicInteger getNodeCounter() {
        return nodeCounter;
    }

    public AtomicInteger getEdgeCounter() {
        return edgeCounter;
    }

    public List<Neighbor> getNeighbours(String nodeId, Integer num){
        return index.getNeighbours(nodeId, num);
    }

    public String getId() {
        return id;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public static Graph fromPath(Path path){
        Info info = Info.fromPath(path);
        Summary summary = Summary.fromPath(path);
        Graph graph = new Graph();
        graph.setId(info.getId());
        graph.setQuery(info.getQuery());
        graph.setDescription(info.getDescription());
        graph.setCreationTime(summary.getCreationTime());
        graph.setNumEdges(info.getNumEdges());
        graph.setNumNodes(info.getNumNodes());
        return graph;
    }

    public GraphSummary toGraphSummary(){
        return GraphSummary.newBuilder().setId(getId()).setDescription(getDescription()).setDate(getCreationTime()).setSize(getSummary().getSize()).build();
    }

    public GraphInfo toGraphInfo(){
        return GraphInfo.newBuilder().setId(getId()).setDate(getCreationTime()).setUrl(getId()).setDescription(getDescription()).setFormat(Format.CSV).setNodes(Selection.newBuilder().setQuery(Query.newBuilder().setFilter(query).build()).setCount(getNodeCounter().get()).build()).setEdges(Selection.newBuilder().setCount(getEdgeCounter().get()).build()).build();
    }

    @Override
    public String toString() {
        return "Graph{" +
                "id='" + id + '\'' +
                ", creationTime='" + creationTime + '\'' +
                ", description='" + description + '\'' +
                ", query='" + query + '\'' +
                ", threshold=" + threshold +
                ", nodeCounter=" + nodeCounter +
                ", edgeCounter=" + edgeCounter +
                '}';
    }
}
