package org.librairy.service.graph.indexer;

import org.librairy.service.graph.metrics.JensenShannon;
import org.librairy.service.graph.model.Neighbor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class MemoryIndexer implements Indexer {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryIndexer.class);

    private final Map<String,List<Double>> nodeMap;
    private final Double minScore;

    public MemoryIndexer(Double minScore) {
        this.nodeMap = new ConcurrentHashMap<>();
        this.minScore = minScore;
    }

    @Override
    public void add(String label, List<Double> vector) {
        this.nodeMap.put(label,vector);
    }

    @Override
    public List<Neighbor> getNeighbours(String label, Integer num) {

        if (!nodeMap.containsKey(label)) return Collections.emptyList();

        List<Double> vector = nodeMap.get(label);

        return nodeMap.entrySet().parallelStream().filter(entry -> !entry.getKey().equalsIgnoreCase(label)).map(entry -> new Neighbor(entry.getKey(), JensenShannon.similarity(vector, entry.getValue()))).filter(rel -> rel.getScore()>this.minScore).sorted((a,b) -> -a.getScore().compareTo(b.getScore())).limit(num).collect(Collectors.toList());
    }
}
