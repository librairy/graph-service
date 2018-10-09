package org.librairy.service.graph.indexer;

import org.librairy.service.graph.model.Neighbor;

import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface Indexer {

    void add(String label, List<Double> vector);

    List<Neighbor> getNeighbours(String label, Integer num);
}
