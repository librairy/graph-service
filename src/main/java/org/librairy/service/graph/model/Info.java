package org.librairy.service.graph.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Info {

    private static final Logger LOG = LoggerFactory.getLogger(Info.class);

    private static final String FILE_NAME = "graph.info";

    static ObjectMapper jsonMapper = new ObjectMapper();

    private String id;

    private String description;

    private String query;

    private Integer numNodes;

    private Integer numEdges;

    public Info() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(Integer numNodes) {
        this.numNodes = numNodes;
    }

    public Integer getNumEdges() {
        return numEdges;
    }

    public void setNumEdges(Integer numEdges) {
        this.numEdges = numEdges;
    }

    public void saveTo(Path directory){
        try {
            jsonMapper.writeValue(Paths.get(directory.toFile().getAbsolutePath(), FILE_NAME).toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Info fromPath(Path directory){

        try {
            return jsonMapper.readValue(Paths.get(directory.toFile().getAbsolutePath(), FILE_NAME).toFile(), Info.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
