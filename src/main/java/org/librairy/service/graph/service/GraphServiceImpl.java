package org.librairy.service.graph.service;

import org.apache.avro.AvroRemoteException;
import org.apache.commons.io.FileUtils;
import org.librairy.service.graph.facade.model.GraphInfo;
import org.librairy.service.graph.facade.model.GraphRequest;
import org.librairy.service.graph.facade.model.GraphSummary;
import org.librairy.service.graph.model.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class GraphServiceImpl implements org.librairy.service.graph.facade.model.GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);
    private Path outputPath;

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    String resourceFolder;

    ParallelExecutor executor = new ParallelExecutor();

    @PostConstruct
    public void setup() {
        this.outputPath = Paths.get(resourceFolder);
    }

    @Override
    public String newGraph(GraphRequest request) throws AvroRemoteException {
        String msg = "accepted";

        executor.submit(() -> {
            Graph graph = new Graph(request.getId());
            graph.setDescription(request.getDescription());
            graph.setQuery(request.getNodes().getFilter());
            // TODO Read nodes from Solr
            //graph.addNode();
            LOG.info("Discovering relations in graph ..");
            graph.build(Integer.valueOf(request.getEdges().getFilter()));
            LOG.info("Saving ..");
            graph.saveTo(outputPath);
            LOG.info("Graph '"+graph+"' created and saved at: " + outputPath);
        });

        return msg;
    }

    @Override
    public GraphInfo getGraph(String s) throws AvroRemoteException {
        GraphInfo graphInfo = Graph.fromPath(Paths.get(resourceFolder, s)).toGraphInfo();
        graphInfo.setUrl(resourceFolder + File.pathSeparator + s);
        return graphInfo;
    }

    @Override
    public List<GraphSummary> listGraphs() throws AvroRemoteException {
        try {
            return Files.walk(Paths.get(resourceFolder)).map(path -> Graph.fromPath(path).toGraphSummary()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public String removeGraph(String s) throws AvroRemoteException {
        try {
            FileUtils.deleteDirectory(Paths.get(resourceFolder, s).toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    @Override
    public String removeGraphs() throws AvroRemoteException {
        try {
            FileUtils.deleteDirectory(Paths.get(resourceFolder).toFile());
            FileUtils.forceMkdir(Paths.get(resourceFolder).toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }
}
