package org.librairy.service.graph.model;

import org.apache.commons.io.FileUtils;
import org.librairy.service.graph.utils.WriterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Data {

    private static final Logger LOG = LoggerFactory.getLogger(Data.class);

    private static final String NODES_FILE = "nodes.csv.gz";
    private static final String EDGES_FILE = "edges.csv.gz";

    private  BufferedWriter nodeWriter;
    private  BufferedWriter edgeWriter;
    private File tmpNodeFile;
    private File tmpEdgeFile;


    public Data(){
        try {
            this.tmpNodeFile = File.createTempFile("graph-nodes-"+System.currentTimeMillis(),"csv.gz");
            this.nodeWriter = WriterUtils.to(tmpNodeFile.getAbsolutePath());

            this.tmpEdgeFile = File.createTempFile("graph-edges-"+System.currentTimeMillis(),"csv.gz");
            this.edgeWriter = WriterUtils.to(tmpEdgeFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setDirectory(Path dir){
        try {
            this.nodeWriter.close();
            FileUtils.forceDelete(tmpNodeFile);
            this.tmpNodeFile = Paths.get(dir.toFile().getAbsolutePath(),NODES_FILE).toFile();
            this.nodeWriter = WriterUtils.to(Paths.get(tmpNodeFile.getAbsolutePath(),NODES_FILE).toFile().getAbsolutePath());


            this.edgeWriter.close();
            FileUtils.forceDelete(tmpEdgeFile);
            this.tmpEdgeFile = Paths.get(dir.toFile().getAbsolutePath(),EDGES_FILE).toFile();
            this.edgeWriter = WriterUtils.to(Paths.get(tmpEdgeFile.getAbsolutePath(),EDGES_FILE).toFile().getAbsolutePath());


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(String n1, String n2){
        try {
            this.edgeWriter.write(n1+","+n2+"\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void add(String node){
        try {
            this.edgeWriter.write(node+"\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveTo(Path directory){
        try {
            this.nodeWriter.close();
            FileUtils.moveFile(tmpNodeFile, Paths.get(directory.toFile().getAbsolutePath(), NODES_FILE).toFile());

            this.edgeWriter.close();
            FileUtils.moveFile(tmpEdgeFile, Paths.get(directory.toFile().getAbsolutePath(), EDGES_FILE).toFile());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Data fromPath(Path directory){
        Data data = new Data();
        data.setDirectory(directory);
        return data;
    }

}
