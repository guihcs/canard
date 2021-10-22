package fr.irit.complex.answer;

import fr.irit.main.ExecutionConfig;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AnswersResult {

    private final List<Answer> answers;
    private final Set<Answer> matchedAnswers;


    public AnswersResult(List<Answer> answers, Set<Answer> matchedAnswers) {
        this.answers = answers;
        this.matchedAnswers = matchedAnswers;
    }


    public List<Answer> getAnswers() {
        return answers;
    }

    public Set<Answer> getMatchedAnswers() {
        return matchedAnswers;
    }

    public void fillSimilarAnswers(ExecutionConfig executionConfig) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        if (getMatchedAnswers().isEmpty()) {
            System.out.println("Looking for similar answers");
            Iterator<Answer> ansIt = getAnswers().iterator();
            while (getMatchedAnswers().size() < executionConfig.getMaxMatches() && ansIt.hasNext()) {
                Answer ans = ansIt.next();
                ans.retrieveIRILabels(executionConfig.getSourceEndpoint());
                ans.getSimilarIRIs(executionConfig.getTargetEndpoint());
                if (ans.hasMatch()) {
                    System.out.println(ans.printMatchedEquivalents());
                    getMatchedAnswers().add(ans);
                }
            }

            System.out.println("Number of similar answers :" + getMatchedAnswers().size());
        }
    }
}
