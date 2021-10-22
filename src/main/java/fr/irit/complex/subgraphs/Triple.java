package fr.irit.complex.subgraphs;

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
    private int type;
    private IRI subjectType;
    private IRI objectType;
    private double objectSimilarity;
    private double subjectSimilarity;
    private double predicateSimilarity;

    public Triple() {
        subject = new IRI("");
        object = new Resource("");
        predicate = new IRI("");
    }

    public Triple(String sub, String pred, String obj, int type) {
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
        objectSimilarity = 0;
        subjectSimilarity = 0;
        predicateSimilarity = 0;
    }

    public void retrieveIRILabels(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (type != 1) {
            subject.retrieveLabels(targetEndpoint);
        }
        if (type != 2) {
            predicate.retrieveLabels(targetEndpoint);
        }
        if (type != 3 && object instanceof IRI) {
            ((IRI) object).retrieveLabels(targetEndpoint);
        }
    }


    /**
     * Compares labels and type labels
     */
    public double compareLabel(Set<String> targetLabels, double threshold, String targetEndpoint) {
        if (type != 1) {
            subjectType = IRITypeUtils.findMostSimilarType(subject, targetEndpoint, targetLabels, threshold);
            double scoreTypeSubMax = 0;
            if (subjectType != null) {
                scoreTypeSubMax = Utils.similarity(subjectType.getLabels(), targetLabels, threshold);
            }
            subjectSimilarity = Utils.similarity(subject.getLabels(), targetLabels, threshold);
            if (scoreTypeSubMax > subjectSimilarity) {
                keepSubjectType = true;
                subjectSimilarity = scoreTypeSubMax;
            }
        }
        if (type != 2 && !predicate.toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
            predicateSimilarity = Utils.similarity(predicate.getLabels(), targetLabels, threshold);
        }
        if (type != 3 && object instanceof IRI) {
            objectType = IRITypeUtils.findMostSimilarType((IRI) object, targetEndpoint, targetLabels, threshold);
            if (objectType != null) {
                double scoreTypeObMax = Utils.similarity(objectType.getLabels(), targetLabels, threshold);
                objectSimilarity = Utils.similarity(((IRI) object).getLabels(), targetLabels, threshold);
                if (scoreTypeObMax > objectSimilarity) {
                    keepObjectType = true;
                    objectSimilarity = scoreTypeObMax;
                }
            }

        } else if (type != 3) {
            HashSet<String> hashObj = new HashSet<>();
            hashObj.add(object.toString());
            objectSimilarity = Utils.similarity(hashObj, targetLabels, threshold);
        }
        return subjectSimilarity + predicateSimilarity + objectSimilarity;

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
        return type == 1;
    }

    public boolean isPredicateTriple() {
        return type == 2;
    }

    public boolean isObjectTriple() {
        return type == 3;
    }

    public IRI getObjectType() {
        return objectType;
    }

    public IRI getSubjectType() {
        return subjectType;
    }

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


    public int commonPartValue(Triple t) {
        int res = -1;
        if (getType() == t.getType()) {
            if (getPredicate().equals(t.getPredicate()) && !isPredicateTriple()) {
                res = 2;
            }
            if (getObject().equals(t.getObject()) && !isObjectTriple() && !keepObjectType) {
                res = 3;
            }
            if (getSubject().equals(t.getSubject()) && !isSubjectTriple() && !keepSubjectType) {
                res = 1;
            }
        }
        return res;
    }

    public boolean hasCommonPart(Triple t) {
        boolean res = false;
        if (getType() == t.getType()) {
            if (!isSubjectTriple()) {
                res = getSubject().equals(t.getSubject());
            }
            if (!isPredicateTriple()) {
                res = res || getPredicate().equals(t.getPredicate());
            }
            if (!isObjectTriple()) {
                res = res || getObject().equals(t.getObject());
            }
        }
        return res;
    }


    public int getType() {
        return type;
    }

    public int hashCode() {
        return (subject.toString() + predicate.toString() + object.toString()).hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Triple) {
            return (subject.toString() + predicate.toString() + object.toString())
                    .equals(((Triple) obj).subject.toString() + ((Triple) obj).predicate.toString() + ((Triple) obj).object.toString());
        } else {
            return false;
        }

    }

    public boolean isNullTriple() {
        return subject.toString().equals("") && predicate.toString().equals("") && object.toString().equals("");
    }


    public double getSimilarity() {
        return subjectSimilarity + predicateSimilarity + objectSimilarity;
    }

    public Resource getAnswer() {
        return switch (type) {
            case 1 ->
                    subject;
            case 2 ->
                    predicate;
            case 3 ->
                    object;
            default -> new Resource("null");
        };
    }

    public double getObjectSimilarity() {
        return objectSimilarity;
    }

    public double getSubjectSimilarity() {
        return subjectSimilarity;
    }

    public double getPredicateSimilarity() {
        return predicateSimilarity;
    }

    public int getPartGivingMaxSimilarity() {
        int res = 0;
        if (subjectSimilarity > objectSimilarity && subjectSimilarity > predicateSimilarity) {
            res = 1;
        } else if (objectSimilarity > subjectSimilarity && objectSimilarity > predicateSimilarity) {
            res = 3;
        } else if (predicateSimilarity > subjectSimilarity && predicateSimilarity > objectSimilarity) {
            res = 2;
        }
        return res;

    }
}
