package fr.irit.complex.subgraphs.unary;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.utils.Utils;
import fr.irit.resource.IRI;
import fr.irit.resource.IRITypeUtils;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.HashSet;
import java.util.Set;

public class Triple extends InstantiatedSubgraph {
    public boolean keepObjectType;
    public boolean keepSubjectType;
    private final IRI subject;
    private final IRI predicate;
    private final Resource object;
    private final TripleType type;
    private IRI subjectType;
    private IRI objectType;


    public Triple(String sub, String pred, String obj, TripleType type) {
        subject = new IRI(sub);
        predicate = new IRI(pred);
        Resource r = new Resource(obj);
        if (r.isIRI()) {
            object = new IRI("<" + obj.replaceAll("[<>]", "") + ">");
        } else {
            object = r;
        }
        this.type = type;
        keepObjectType = false;
        keepSubjectType = false;
    }

    public void retrieveIRILabels(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (type!= TripleType.SUBJECT){
            subject.retrieveLabels(targetEndpoint);
        }
        if(type!= TripleType.PREDICATE){
            predicate.retrieveLabels(targetEndpoint);
        }
        if(type != TripleType.OBJECT && object instanceof IRI){
            ((IRI)object).retrieveLabels(targetEndpoint);
        }
    }

    public void retrieveTypes(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException{
        if (type!= TripleType.SUBJECT){
            subject.retrieveTypes(targetEndpoint);
        }
        if(type!= TripleType.PREDICATE){
            predicate.retrieveTypes(targetEndpoint);
        }
        if(type != TripleType.OBJECT && object instanceof IRI){
            ((IRI)object).retrieveTypes(targetEndpoint);
        }
    }

    public int commonPartValue(Triple t) {
        int res = -1;
        if (type == t.type) {
            if (predicate.equals(t.predicate) && !isPredicateTriple()) {
                res = 2;
            }
            if (object.equals(t.object) && !isObjectTriple() && !keepObjectType) {
                res = 3;
            }
            if (subject.equals(t.subject) && !isSubjectTriple() && !keepSubjectType) {
                res = 1;
            }
        }
        return res;
    }

    public boolean hasCommonPart(Triple t) {
        boolean res = false;
        if (type == t.type) {
            if (!isSubjectTriple()) {
                res = subject.equals(t.subject);
            }
            if (!isPredicateTriple()) {
                res = res || predicate.equals(t.predicate);
            }
            if (!isObjectTriple()) {
                res = res || object.equals(t.object);
            }
        }
        return res;
    }

    public SimilarityValues compareLabel(Set<String> targetLabels, double threshold, String targetEndpoint) {
        double subjectSimilarity = 0;
        double predicateSimilarity = 0;
        double objectSimilarity = 0;

        if (type != TripleType.SUBJECT) {
            subjectSimilarity = compareSubjectSimilarity(targetLabels, threshold, targetEndpoint);

        }
        if (type != TripleType.PREDICATE && !predicate.toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
            predicateSimilarity = comparePredicateSimilarity(targetLabels, threshold);
        }
        if (type != TripleType.OBJECT) {
            objectSimilarity = compareObjectSimilarity( targetLabels, threshold, targetEndpoint);
        }

        double similarity = subjectSimilarity + predicateSimilarity + objectSimilarity;


        return new SimilarityValues(similarity, subjectSimilarity, predicateSimilarity, objectSimilarity);

    }


    private double compareSubjectSimilarity(Set<String> targetLabels, double threshold, String targetEndpoint){
        double scoreTypeSubMax = 0;
        double subjectSimilarity;
        subjectType = IRITypeUtils.findMostSimilarType(subject, targetEndpoint, targetLabels, threshold);

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

    private double comparePredicateSimilarity(Set<String> targetLabels, double threshold){
        return Utils.similarity(predicate.getLabels(), targetLabels, threshold);
    }

    public double compareObjectSimilarity(Set<String> targetLabels, double threshold, String targetEndpoint){
        double objectSimilarity = 0;
        if(object instanceof IRI to) {
            objectType = IRITypeUtils.findMostSimilarType(to, targetEndpoint, targetLabels, threshold);
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
