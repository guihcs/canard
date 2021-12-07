package fr.irit.complex.subgraphs.similarity;

public class PathSimilarity extends SimilarityValues {


    private double similarity;


    @Override
    public double getSimilarity() {
        return similarity;
    }


    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
