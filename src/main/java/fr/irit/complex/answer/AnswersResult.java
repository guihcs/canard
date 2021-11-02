package fr.irit.complex.answer;

import fr.irit.main.ExecutionConfig;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.List;
import java.util.Set;

public record AnswersResult(Set<Answer> matchedAnswers) {



    public Set<Answer> getMatchedAnswers() {
        return matchedAnswers;
    }

    public void fillSimilarAnswers(ExecutionConfig executionConfig, List<Answer> answers) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {

    }
}
