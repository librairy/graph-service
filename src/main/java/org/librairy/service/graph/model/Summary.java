package org.librairy.service.graph.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.librairy.service.graph.facade.model.GraphSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Summary {

    private static final Logger LOG = LoggerFactory.getLogger(Summary.class);

    private static final String FILE_NAME = "graph.summary";

    static ObjectMapper jsonMapper = new ObjectMapper();

    private String id;

    private String description;

    private String creationTime;

    private String size;

    public Summary() {
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

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void saveTo(Path directory){
        try {
            jsonMapper.writeValue(Paths.get(directory.toFile().getAbsolutePath(), FILE_NAME).toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Summary fromPath(Path directory){

        try {
            return jsonMapper.readValue(Paths.get(directory.toFile().getAbsolutePath(), FILE_NAME).toFile(), Summary.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
