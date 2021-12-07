package fr.irit.complex.answer;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.similarity.SimilarityValues;

public record SubgraphResult(InstantiatedSubgraph subgraph,
                             SimilarityValues similarity) {

    public SimilarityValues getSimilarity() {
        return similarity;
    }

    public InstantiatedSubgraph getSubgraph() {
        return subgraph;
    }
}
