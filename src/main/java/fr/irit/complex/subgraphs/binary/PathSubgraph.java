package fr.irit.complex.subgraphs.binary;

import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.subgraphs.similarity.PathSimilarity;

import java.util.ArrayList;
import java.util.List;

public class PathSubgraph extends SubgraphForOutput<Path, PathSimilarity> {
    final List<Path> paths;

    public PathSubgraph(Path p, double sim) {
        paths = new ArrayList<>();
        paths.add(p);
        similarity = sim;
    }

    public double getAverageSimilarity() {
        return similarity;
    }

    @Override
    public boolean addSubgraph(Path p, PathSimilarity sim) {
        boolean added = false;
        if (p.toSubGraphString().equals(paths.get(0).toSubGraphString())) {
            addSimilarity(sim.getSimilarity());
            paths.add(p);
            added = true;
        }
        return added;
    }

    public void addSimilarity(double s) {
        similarity = ((similarity * paths.size()) + s) / (paths.size() + 1);
    }

    public String toExtensionString() {
        return paths.get(0).toSubGraphString();
    }

    public String toSPARQLForm() {
        return "SELECT distinct ?answer0 ?answer1 WHERE {\n" +
                paths.get(0).toSubGraphString() + "}";
    }

    public String toIntensionString() {
        return paths.get(0).toSubGraphString();
    }

    public Path getMainPath() {
        return paths.get(0);
    }


}
