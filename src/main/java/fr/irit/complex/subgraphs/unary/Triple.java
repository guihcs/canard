package fr.irit.complex.subgraphs.unary;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.utils.Utils;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.HashSet;
import java.util.Set;

public abstract class Triple extends InstantiatedSubgraph {
    public boolean keepObjectType;
    public boolean keepSubjectType;
    private final IRI subject;
    private final IRI predicate;
    private final Resource object;
    private final TripleType type;
    private IRI subjectType;
    private IRI objectType;


    public Triple(IRI sub, IRI pred, Resource obj, TripleType type) {
        subject = sub;
        predicate = pred;
        object = obj;
        this.type = type;
        keepObjectType = false;
        keepSubjectType = false;
    }

    public void retrieveIRILabels(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException { }

    public void retrieveTypes(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException { }

    public int commonPartValue(Triple t) { return -1; }

    public boolean hasCommonPart(Triple t) {
        return commonPartValue(t) != -1;
    }

    public SimilarityValues compareLabel(Set<String> targetLabels, double threshold) {
        return null;
    }


    protected double compareSubjectSimilarity(Set<String> targetLabels, double threshold){
        double scoreTypeSubMax = 0;
        double subjectSimilarity;


        if (subjectType != null) {
            scoreTypeSubMax = Utils.similarity(subjectType.getLabels(), targetLabels, threshold);
        }
        subjectSimilarity = Utils.similarity(subject.getLabels(), targetLabels, threshold);

        if (scoreTypeSubMax > subjectSimilarity) {
            keepSubjectType = true;
            subjectSimilarity = scoreTypeSubMax;
        }
        return subjectSimilarity;
    }

    protected double comparePredicateSimilarity(Set<String> targetLabels, double threshold){
        if (predicate.isType()) return 0;
        return Utils.similarity(predicate.getLabels(), targetLabels, threshold);
    }

    protected double compareObjectSimilarity(Set<String> targetLabels, double threshold){
        double objectSimilarity = 0;
        if(object instanceof IRI to) {

            if (objectType != null) {
                double scoreTypeObMax = Utils.similarity(objectType.getLabels(), targetLabels, threshold);
                objectSimilarity = Utils.similarity(to.getLabels(), targetLabels, threshold);
                if (scoreTypeObMax > objectSimilarity) {
                    keepObjectType = true;
                    objectSimilarity = scoreTypeObMax;
                }
            }
        } else {
            Set<String> hashObj = new HashSet<>();
            hashObj.add(object.toString());
            objectSimilarity = Utils.similarity(hashObj, targetLabels, threshold);
        }
        return objectSimilarity;
    }



    public Resource getAnswer() {
        return switch (type) {
            case SUBJECT -> subject;
            case PREDICATE -> predicate;
            case OBJECT -> object;
        };
    }

    public IRI getSubject() {
        return subject;
    }


    public IRI getPredicate() {
        return predicate;
    }


    public Resource getObject() {
        return object;
    }


    public boolean isSubjectTriple() {
        return type == TripleType.SUBJECT;
    }

    public boolean isPredicateTriple() {
        return type == TripleType.PREDICATE;
    }

    public boolean isObjectTriple() {
        return type == TripleType.OBJECT;
    }

    public IRI getObjectType() {
        return objectType;
    }

    public IRI getSubjectType() {
        return subjectType;
    }


    public void setSubjectType(IRI subjectType) {
        this.subjectType = subjectType;
    }

    public void setObjectType(IRI objectType) {
        this.objectType = objectType;
    }

    @Override
    public SubgraphForOutput toOutput(SimilarityValues similarity) {
        return new TripleSubgraph(this, similarity);
    }

    @Override
    public int hashCode() {
        return (subject.toString() + predicate.toString() + object.toString()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Triple to) {
            return (subject.toString() + predicate.toString() + object.toString())
                    .equals(to.subject.toString() + to.predicate.toString() + to.object.toString());
        } else {
            return false;
        }

    }

    @Override
    public String toString() {
        String subjStr = subject.toValueString();
        String predStr = predicate.toValueString();
        String objStr = object.toValueString();

        if (isSubjectTriple()) {
            subjStr = "?answer";
        } else if (isPredicateTriple()) {
            predStr = "?answer";
        } else if (isObjectTriple()) {
            objStr = "?answer";
        }


        String result = subjStr + " " + predStr + " " + objStr + ". ";
        if (keepSubjectType && !keepObjectType) {

            result = "?x " + predStr + " " + objStr + ". " +
                    "?x a " + subjectType + ". ";
        } else if (keepObjectType && !keepSubjectType) {
            result = subjStr + " " + predStr + " ?y. " +
                    "?y a " + objectType + ". ";
        } else if (keepObjectType) {

            result = "?x " + predStr + " ?y. " +
                    "?y a " + objectType + ". " +
                    "?x a " + subjectType + ". ";
        }
        return result;
    }
}
