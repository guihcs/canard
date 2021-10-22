package fr.irit.complex;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.complex.answer.*;
import fr.irit.complex.subgraphs.*;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;


public class ComplexAlignmentGeneration {

    private final ExecutionConfig executionConfig;


    public ComplexAlignmentGeneration(ExecutionConfig executionConfig) {
        this.executionConfig = executionConfig;
    }


    public void run() throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        System.out.println("Number of CQAs: " + executionConfig.getQueries().size());
        for (SparqlSelect sparqlSelect : executionConfig.getQueries()) {
            System.out.println();
            executeQuery(sparqlSelect);
        }
    }


    private void executeQuery(SparqlSelect sparqlSelect) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        if (sparqlSelect.getFocusLength() == 1) {
            System.out.println("Unary query : " + sparqlSelect.toSubgraphForm());
        } else if (sparqlSelect.getFocusLength() == 2) {
            System.out.println("Binary query : " + sparqlSelect.toSubgraphForm());
        }



        for (Map.Entry<String, IRI> m : sparqlSelect.getIRIList().entrySet()) {
            m.getValue().retrieveLabels(executionConfig.getSourceEndpoint());
            System.out.println(m.getValue().getLabels());
        }

        AnswersResult answersResult = getAnswers(sparqlSelect);


        Set<InstantiatedSubgraph> goodGraphs = new HashSet<>();


        for (Answer ans : answersResult.getMatchedAnswers()) {
            Set<InstantiatedSubgraph> localGraphs = ans.findCorrespondingSubGraph(sparqlSelect, executionConfig);
            goodGraphs.addAll(localGraphs);
        }

        System.out.println(goodGraphs);

        List<SubgraphForOutput> output = buildSubgraphForOutput(goodGraphs);


        System.out.println("Number of correspondences found: " + output.size());

        if (executionConfig.isReassess()) {
            System.out.println("Reassessing similarity");
            for (SubgraphForOutput s : output) {
                System.out.println(s);
                s.reassessSimilarityWithCounterExamples(executionConfig.getSourceEndpoint(), executionConfig.getTargetEndpoint(), sparqlSelect);
            }
        }

        Collections.sort(output);
        List<SubgraphForOutput> singleOutput = filterSingleOutput(output);


        if (!singleOutput.isEmpty()) {
            executionConfig.addToOutput(sparqlSelect, singleOutput);
        }
    }

    private AnswersResult getAnswers(SparqlSelect sparqlSelect) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        List<Answer> answers = new ArrayList<>();
        Set<Answer> matchedAnswers = new HashSet<>();
        int offsetMatch = 0;

        boolean noMoreSourceAnswers = false;
        int offset = 0;
        int limit = 2000;


        while (!noMoreSourceAnswers && matchedAnswers.size() < executionConfig.getMaxMatches()) {

            try {
                List<Answer> answerList = getSourceAnswers(sparqlSelect, limit, offset);
                noMoreSourceAnswers = answerList.size() < limit;
                System.out.println("answer size: " + answerList.size());
                answers.addAll(answerList);
            } catch (QueryArityException e) {
                System.out.println("ERROR for query : " + sparqlSelect.toUnchangedString());
                System.err.println("Problem detected: too many variables in SELECT: can only deal with 1 or 2");
                noMoreSourceAnswers = true;
            } catch (SparqlQueryMalFormedException e) {
                System.out.println("Error: malformed sparql");
                System.out.println(e.getMessage());
                break;
            } catch (SparqlEndpointUnreachableException e) {
                System.out.println("Endpoint unreachable.");
                break;
            }

            if (!noMoreSourceAnswers) {
                offset += limit;
            }

            while (matchedAnswers.size() < executionConfig.getMaxMatches() && offsetMatch < answers.size()) {

                Answer ans = answers.get(offsetMatch);
                ans.getExistingMatches(executionConfig.getSourceEndpoint(), executionConfig.getTargetEndpoint());
                if (ans.hasMatch()) {
                    matchedAnswers.add(ans);
                    System.out.println(ans.printMatchedEquivalents());
                }
                offsetMatch++;
            }
        }
        System.out.println("Number of matched answers :" + matchedAnswers.size());

        AnswersResult answersResult = new AnswersResult(answers, matchedAnswers);

        answersResult.fillSimilarAnswers(executionConfig);

        return answersResult;
    }


    private List<Answer> getSourceAnswers(SparqlSelect sparqlSelect, int limit, int offset) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, QueryArityException {
        SparqlProxy sparqlProxy = SparqlProxy.getSparqlProxy(executionConfig.getSourceEndpoint());
        String queryLimit = " LIMIT " + limit + "\n OFFSET " + offset;
        List<JsonNode> ret = sparqlProxy.getResponse(sparqlSelect.toUnchangedString() + queryLimit);
        return getByArity(sparqlSelect.getFocusLength(), sparqlSelect, ret);
    }


    private List<Answer> getByArity(int arity, SparqlSelect sparqlSelect, List<JsonNode> ret) throws QueryArityException {
        if (arity == 1) {
            return getUnaryAnswers(sparqlSelect, ret);

        } else if (arity == 2) {
            return getBinaryAnswers(sparqlSelect, ret);
        } else {
            throw new QueryArityException();
        }

    }


    private List<Answer> getUnaryAnswers(SparqlSelect sparqlSelect, List<JsonNode> ret) {
        List<Answer> answers = new ArrayList<>();
        for (JsonNode response : ret) {
            String s = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).get("value").toString().replaceAll("\"", "");
            String type = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).get("type").toString().replaceAll("\"", "");
            if (!type.equals("bnode")) {
                SingleAnswer singleton = new SingleAnswer(new Resource(s));
                answers.add(singleton);
            }

        }
        return answers;
    }

    private List<Answer> getBinaryAnswers(SparqlSelect sparqlSelect, List<JsonNode> ret) {
        List<Answer> answers = new ArrayList<>();
        for (JsonNode response : ret) {
            String s1 = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).get("value").toString().replaceAll("\"", "");
            String s2 = response.get(sparqlSelect.getSelectFocus().get(1).replaceFirst("\\?", "")).get("value").toString().replaceAll("\"", "");
            String type1 = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).get("type").toString().replaceAll("\"", "");
            String type2 = response.get(sparqlSelect.getSelectFocus().get(1).replaceFirst("\\?", "")).get("type").toString().replaceAll("\"", "");
            if (!type1.equals("bnode") && !type2.equals("bnode")) {
                if (!s1.equals("") && !s2.equals("")) {
                    PairAnswer pair = new PairAnswer(new Resource(s1), new Resource(s2));
                    answers.add(pair);
                }
            }
        }
        return answers;
    }

    private List<SubgraphForOutput> buildSubgraphForOutput(Set<InstantiatedSubgraph> goodSubgraphs) {
        List<SubgraphForOutput> output = new ArrayList<>();
        for (InstantiatedSubgraph t : goodSubgraphs) {
            boolean added = false;
            Iterator<SubgraphForOutput> it = output.iterator();
            while (it.hasNext() && !added) {
                SubgraphForOutput subG = it.next();
                if (t instanceof Triple && subG instanceof TripleSubgraph) {
                    added = ((TripleSubgraph) subG).addSubgraph((Triple) t);
                }
                if (t instanceof Path && subG instanceof PathSubgraph) {
                    added = ((PathSubgraph) subG).addSubgraph((Path) t);
                }
            }
            if (!added) {
                if (t instanceof Triple) {
                    output.add(new TripleSubgraph((Triple) t));
                }
                if (t instanceof Path) {
                    output.add(new PathSubgraph((Path) t));
                }
            }
        }
        return output;
    }


    private List<SubgraphForOutput> filterSingleOutput(List<SubgraphForOutput> output) {
        List<SubgraphForOutput> singleOutput = new ArrayList<>();

        if (output.size() > 0 && output.get(output.size() - 1).getSimilarity() < 0.6 && output.get(output.size() - 1).getSimilarity() > 0.01) {
            double sim = output.get(output.size() - 1).getSimilarity();
            boolean moreCorrespondences = true;
            int i = output.size() - 1;
            while (i >= 0 && moreCorrespondences) {
                if (output.get(i).getSimilarity() == sim) {
                    singleOutput.add(output.get(i));
                    System.out.println(output.get(i));
                } else {
                    moreCorrespondences = false;
                }
                i--;
            }
        } else {
            for (SubgraphForOutput s : output) {
                if (s.getSimilarity() >= 0.6) {
                    singleOutput.add(s);
                }
                System.out.println(s);
            }
        }
        return singleOutput;
    }


}

