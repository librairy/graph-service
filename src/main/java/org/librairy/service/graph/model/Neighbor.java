package org.librairy.service.graph.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Neighbor {

    private static final Logger LOG = LoggerFactory.getLogger(Neighbor.class);

    private String node;

    private Double score;

    public Neighbor(String node, Double score) {
        this.node = node;
        this.score = score;
    }

    public Neighbor() {
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "Neighbour{" +
                "node='" + node + '\'' +
                ", score=" + score +
                '}';
    }
}
