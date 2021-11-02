package fr.irit.complex.subgraphs.binary;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.subgraphs.unary.SimilarityValues;

import java.util.ArrayList;

public class PathSubgraph extends SubgraphForOutput {
    final ArrayList<Path> paths;

    public PathSubgraph(Path p, double sim) {
        paths = new ArrayList<>();
        paths.add(p);
        similarity = sim;
    }

    public double getAverageSimilarity() {
        return similarity;
    }

    @Override
    public boolean addSubgraph(InstantiatedSubgraph p, SimilarityValues sim) {
        boolean added = false;
        if (((Path) p).toSubGraphString().equals(paths.get(0).toSubGraphString())) {
            addSimilarity(sim.similarity());
            paths.add((Path) p);
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
