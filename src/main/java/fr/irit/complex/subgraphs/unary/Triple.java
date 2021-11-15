package fr.irit.complex.subgraphs.unary;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;

public class Triple extends InstantiatedSubgraph {
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
        }

        return false;

    }


    @Override
    public String toString() {

        String subjStr = getSubject().toValueString();
        String predStr = getPredicate().toValueString();
        String objStr = getObject().toValueString();

        if (type == TripleType.SUBJECT) {
            subjStr = "?answer";
        } else if (type == TripleType.PREDICATE) {
            predStr = "?answer";
        } else {
            objStr = "?answer";
        }

        return toStringTemplate(subjStr, predStr, objStr);
    }

    protected String toStringTemplate(String subjStr, String predStr, String objStr) {
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


    public void setKeepObjectType(boolean keepObjectType) {
        this.keepObjectType = keepObjectType;
    }

    public void setKeepSubjectType(boolean keepSubjectType) {
        this.keepSubjectType = keepSubjectType;
    }

    public TripleType getType() {
        return type;
    }
}
