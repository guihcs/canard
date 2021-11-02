package fr.irit.complex.utils;


import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Set;

public final class Utils {


    public static double similarity(Set<String> labels1, Set<String> labels2, double threshold) {
        double score = 0;
        for (String l1 : labels1) {
            for (String l2 : labels2) {
                score += stringSimilarity(l1, l2, threshold);
            }
        }
        return score;
    }

    private static double stringSimilarity(String s1, String s2, double threshold) {

        double dist = LevenshteinDistance.getDefaultInstance().apply(s1.toLowerCase(), s2.toLowerCase()) / ((double) Math.max(s1.length(), s2.length()));
        double sim = 1 - dist;

        if (sim < threshold) {
            return 0;
        } else {
            return sim;
        }
    }





}
