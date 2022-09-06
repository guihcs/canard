package fr.irit.complex.subgraphs.similarity;


import fr.irit.complex.subgraphs.unary.Triple;
import fr.irit.complex.subgraphs.unary.TripleType;
import fr.irit.complex.utils.Utils;
import fr.irit.resource.IRI;
import org.apache.jena.sparql.function.library.print;

import java.util.HashSet;
import java.util.Set;

public class TripleSimilarity extends SimilarityValues {

    private double subjectSimilarity;
    private double predicateSimilarity;
    private double objectSimilarity;
    private double subjectTypeSimilarity;
    private double objectTypeSimilarity;


    public static TripleSimilarity compareTripleWithLabels(Triple triple, Set<String> labels, double threshold){
        TripleSimilarity tripleSimilarity = new TripleSimilarity();


        if (triple.getType() != TripleType.SUBJECT) {

            double scoreTypeSubMax = 0;
            double subjectSimilarity;


            if (triple.getSubjectType() != null) {
                scoreTypeSubMax = Utils.similarity(triple.getSubjectType().getLabels(), labels, threshold);
            }
            subjectSimilarity = Utils.similarity(triple.getSubject().getLabels(), labels, threshold);
            tripleSimilarity.setSubjectSimilarity(subjectSimilarity);
            tripleSimilarity.setSubjectTypeSimilarity(scoreTypeSubMax);
        }

        if (triple.getType() != TripleType.PREDICATE && !triple.getPredicate().isType()) {
            double predicateSimilarity = Utils.similarity(triple.getPredicate().getLabels(), labels, threshold);
            tripleSimilarity.setPredicateSimilarity(predicateSimilarity);
        }

        if (triple.getType() != TripleType.OBJECT) {
            double scoreTypeObMax = 0;
            double objectSimilarity;
            if (triple.getObject() instanceof IRI to && triple.getObjectType() != null) {
                scoreTypeObMax = Utils.similarity(triple.getObjectType().getLabels(), labels, threshold);
                objectSimilarity = Utils.similarity(to.getLabels(), labels, threshold);
            } else {
                Set<String> hashObj = Set.of(triple.getObject().toString());

                hashObj = parseLabels(hashObj);

                objectSimilarity = Utils.similarity(hashObj, labels, threshold);
            }
            tripleSimilarity.setObjectSimilarity(objectSimilarity);
            tripleSimilarity.setObjectTypeSimilarity(scoreTypeObMax);
        }


        return tripleSimilarity;
    }


    private static Set<String> parseLabels(Set<String> values){
        Set<String> result = new HashSet<>();
        for (String value : values) {
            String[] split = value.split("#");
            value = split.length > 1 ? split[split.length - 1] : split[0];
            value = value.replaceAll("\\W", "");
            result.add(value);
        }
        return result;
    }


    @Override
    public double getSimilarity() {

        return Math.max(subjectSimilarity, subjectTypeSimilarity) + predicateSimilarity + Math.max(objectSimilarity, objectTypeSimilarity);
    }


    public double getSubjectSimilarity() {
        return subjectSimilarity;
    }

    public void setSubjectSimilarity(double subjectSimilarity) {
        this.subjectSimilarity = subjectSimilarity;
    }

    public double getPredicateSimilarity() {
        return predicateSimilarity;
    }

    public void setPredicateSimilarity(double predicateSimilarity) {
        this.predicateSimilarity = predicateSimilarity;
    }

    public double getObjectSimilarity() {
        return objectSimilarity;
    }

    public void setObjectSimilarity(double objectSimilarity) {
        this.objectSimilarity = objectSimilarity;
    }

    public void setSubjectTypeSimilarity(double subjectTypeSimilarity) {
        this.subjectTypeSimilarity = subjectTypeSimilarity;
    }

    public void setObjectTypeSimilarity(double objectTypeSimilarity) {
        this.objectTypeSimilarity = objectTypeSimilarity;
    }


    @Override
    public String toString() {
        return "TripleSimilarity{" +
                "subjectSimilarity=" + subjectSimilarity +
                ", predicateSimilarity=" + predicateSimilarity +
                ", objectSimilarity=" + objectSimilarity +
                ", subjectTypeSimilarity=" + subjectTypeSimilarity +
                ", objectTypeSimilarity=" + objectTypeSimilarity +
                '}';
    }

    public boolean canKeepSubjectType(Triple triple){
        return triple.getType() != TripleType.SUBJECT && subjectTypeSimilarity > subjectSimilarity;
    }

    public boolean canKeepObjectType(Triple triple){
        return triple.getType() != TripleType.OBJECT && triple.getObject() instanceof IRI && triple.getObjectType() != null && objectTypeSimilarity > objectSimilarity;

    }


    public TripleType getPartGivingMaxSimilarity() {
        if (getSubjectSimilarity() > getObjectSimilarity() && getSubjectSimilarity() > getPredicateSimilarity()) {
            return TripleType.SUBJECT;
        } else if (getObjectSimilarity() > getSubjectSimilarity() && getObjectSimilarity() > getPredicateSimilarity()) {
            return TripleType.OBJECT;
        } else if (getPredicateSimilarity() > getSubjectSimilarity() && getPredicateSimilarity() > getObjectSimilarity()) {
            return TripleType.PREDICATE;
        } else {
            return TripleType.NONE;
        }
    }
}
