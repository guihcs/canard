package fr.irit.complex.subgraphs.unary;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;

public class Triple extends InstantiatedSubgraph {
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

    public TripleType getType() {
        return type;
    }

    public boolean haveTypePredicate() {
        return predicate.toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
    }
}
