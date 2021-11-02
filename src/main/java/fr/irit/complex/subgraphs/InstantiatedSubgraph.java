package fr.irit.complex.subgraphs;

import fr.irit.complex.subgraphs.unary.SimilarityValues;

public abstract class InstantiatedSubgraph implements Comparable<InstantiatedSubgraph> {

    public double getSimilarity() {
        return 0;
    }

    public SubgraphForOutput toOutput(SimilarityValues sim){
        return null;
    }


    @Override
    public int compareTo(InstantiatedSubgraph o) {
        return 0;
    }
}

	
