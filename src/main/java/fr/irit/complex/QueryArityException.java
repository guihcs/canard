package fr.irit.complex;

import fr.irit.sparql.query.select.SparqlSelect;

public class QueryArityException extends Exception {
    final SparqlSelect sparqlSelect;

    public QueryArityException(SparqlSelect sparqlSelect) {
        super("ERROR for query : " + sparqlSelect.toUnchangedString() + "\nProblem detected: too many variables in SELECT: can only deal with 1 or 2");
        this.sparqlSelect = sparqlSelect;
    }
}
