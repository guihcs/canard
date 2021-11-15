package fr.irit.complex;

import fr.irit.complex.answer.*;
import fr.irit.complex.answer.binary.PairAnswer;
import fr.irit.complex.answer.unary.SingleAnswer;
import fr.irit.complex.subgraphs.*;
import fr.irit.complex.subgraphs.unary.SimilarityValues;
import fr.irit.complex.subgraphs.unary.Triple;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
import fr.irit.resource.IRIUtils;
import fr.irit.resource.Resource;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;
import java.util.stream.Collectors;


public record ComplexAlignmentGeneration(ExecutionConfig executionConfig) {


    public void run() throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, QueryArityException {
        System.out.println("Number of CQAs: " + executionConfig.getQueries().size());
        for (SparqlSelect sparqlSelect : executionConfig.getQueries()) {
            System.out.println();
            executeQuery(sparqlSelect);
        }
    }


    private void executeQuery(SparqlSelect sparqlSelect) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, QueryArityException {
        if (sparqlSelect.getFocusLength() == 1) {
            System.out.println("Unary query : " + sparqlSelect.toSubgraphForm());
        } else if (sparqlSelect.getFocusLength() == 2) {
            System.out.println("Binary query : " + sparqlSelect.toSubgraphForm());
        }



        System.out.println("step 3 ==============================");
        List<Map<String, SelectResponse.Results.Binding>> ret = SparqlProxy.getAnswers(sparqlSelect, executionConfig.getSourceEndpoint());
        List<Answer> answers = getByArity(sparqlSelect.getFocusLength(), sparqlSelect, ret);
        System.out.println("answer size: " + answers.size());


        System.out.println("step 4 ==============================");
        Set<Answer> matchedAnswers = matchAnswers(answers);

        System.out.println("step 2 =============================");
        Set<String> queryLabels = new HashSet<>();

        for (Map.Entry<String, IRI> m : sparqlSelect.getIRIMap().entrySet()) {
            IRIUtils.retrieveLabels(m.getValue(), executionConfig.getSourceEndpoint());
            queryLabels.addAll(m.getValue().getLabels());
        }
        System.out.println(queryLabels);

        System.out.println("step 5, 6, 7 ======================================");
        Set<SubgraphResult> goodGraphs = new HashSet<>();

        for (Answer ans : matchedAnswers) {
            if (ans instanceof SingleAnswer singleAnswer) {

                Set<Triple> triples = IRIUtils.getAllTriples(singleAnswer.getRes(), queryLabels, executionConfig.getTargetEndpoint(), executionConfig.getSimilarityThreshold());

                Set<SubgraphResult> goodTriples = getGoodTriples(queryLabels, triples);

                goodGraphs.addAll(goodTriples);

            } else if(ans instanceof  PairAnswer pairAnswer){
                Set<SubgraphResult> localGraphs = pairAnswer.findCorrespondingSubGraph(queryLabels, sparqlSelect, executionConfig);
                goodGraphs.addAll(localGraphs);
            }

        }


        System.out.println("step 8 ? =============================");
        goodGraphs = goodGraphs.stream()
                .filter(subgraphResult -> subgraphResult.getSimilarity().similarity() > 0)
                .collect(Collectors.toSet());




        List<SubgraphForOutput> output = buildSubgraphForOutput(goodGraphs);

        Set<String> queries = new HashSet<>();

        output = output.stream().filter(subgraphForOutput -> {
            if (queries.contains(subgraphForOutput.toString())) return false;
            queries.add(subgraphForOutput.toString());
            return true;
        }).collect(Collectors.toList());


        System.out.println("Number of correspondences found: " + output.size());
        output.sort(SubgraphForOutput::compareTo);
        output.forEach(System.out::println);
        System.out.println("step 9 =======================================");
        if (executionConfig.isReassess()) {
            System.out.println("Reassessing similarity");
            for (SubgraphForOutput s : output) {
                System.out.println(s);
                s.reassessSimilarityWithCounterExamples(executionConfig.getSourceEndpoint(), executionConfig.getTargetEndpoint(), sparqlSelect);
            }
        }

        System.out.println("step 10 ======================================");
        Collections.sort(output);
        List<SubgraphForOutput> singleOutput = filterSingleOutput(output);


        System.out.println("step 11 =======================================");
        if (!singleOutput.isEmpty()) {
            executionConfig.addToOutput(sparqlSelect, singleOutput);
        }
        System.out.println("=======================================");
    }

    private Set<SubgraphResult> getGoodTriples(Set<String> queryLabels, Set<Triple> triples) {
        double maxSim = Double.NEGATIVE_INFINITY;
        Triple bestTriple = null;
        Set<SubgraphResult> goodTriples = new HashSet<>();
        SimilarityValues maxSimilarityValues = null;

        double localMaxSim = Double.NEGATIVE_INFINITY;

        for (Triple t : triples) {

            SimilarityValues similarityValues = IRIUtils.compareLabel(t, queryLabels, executionConfig.getSimilarityThreshold());
            double similarity = similarityValues.similarity();

            if (similarity > maxSim) {
                maxSim = similarity;
                maxSimilarityValues = similarityValues;
                bestTriple = t;
            }

            if (similarity > localMaxSim) {
                localMaxSim = similarity;
            }

            if (similarity >= 0.6) {
                goodTriples.add(new SubgraphResult(t, similarityValues));
            }
        }

        if (goodTriples.isEmpty() && bestTriple != null) {
            goodTriples.add(new SubgraphResult(bestTriple, maxSimilarityValues));
        }
        return goodTriples;
    }


    private Set<Answer> matchAnswers(List<Answer> answers) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {

        int offsetMatch = 0;
        Set<Answer> matchedAnswers = new HashSet<>();
        while (matchedAnswers.size() < executionConfig.getMaxMatches() && offsetMatch < answers.size()) {

            Answer ans = answers.get(offsetMatch);
            ans.getExistingMatches(executionConfig.getSourceEndpoint(), executionConfig.getTargetEndpoint());
            if (ans.hasMatch()) {
                matchedAnswers.add(ans);
            }
            offsetMatch++;
        }

        System.out.println("Number of matched answers :" + matchedAnswers.size());

        if (matchedAnswers.isEmpty()) {
            System.out.println("Looking for similar answers...");
            for (Answer ans : answers) {
                if (matchedAnswers.size() >= executionConfig.getMaxMatches()) break;
                ans.retrieveIRILabels(executionConfig.getSourceEndpoint());
                ans.getSimilarIRIs(executionConfig.getTargetEndpoint());

                if (ans.hasMatch()) {
                    System.out.println(ans.printMatchedEquivalents());
                    matchedAnswers.add(ans);
                }
            }

            System.out.println("Number of similar answers :" + matchedAnswers.size());
        }

        return matchedAnswers;
    }


    private List<Answer> getByArity(int arity, SparqlSelect sparqlSelect, List<Map<String, SelectResponse.Results.Binding>> ret) throws QueryArityException {
        if (arity == 1) {
            return getUnaryAnswers(sparqlSelect, ret);

        } else if (arity == 2) {
            return getBinaryAnswers(sparqlSelect, ret);
        } else {
            throw new QueryArityException(sparqlSelect);
        }

    }


    private List<Answer> getUnaryAnswers(SparqlSelect sparqlSelect, List<Map<String, SelectResponse.Results.Binding>> ret) {
        List<Answer> answers = new ArrayList<>();
        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            String s = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).getValue();
            String type = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).getType().replaceAll("\"", "");
            if (!type.equals("bnode")) {
                Resource r = new Resource(s);
                Resource res;
                if (r.isIRI()) {
                    res = new IRI("<" + r + ">");
                } else {
                    res = r;
                }
                SingleAnswer singleton = new SingleAnswer(res);
                answers.add(singleton);
            }

        }
        return answers;
    }

    private List<Answer> getBinaryAnswers(SparqlSelect sparqlSelect, List<Map<String, SelectResponse.Results.Binding>> ret) {
        List<Answer> answers = new ArrayList<>();
        for (Map<String, SelectResponse.Results.Binding> response : ret) {
            String s1 = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).getValue();
            String s2 = response.get(sparqlSelect.getSelectFocus().get(1).replaceFirst("\\?", "")).getValue();
            String type1 = response.get(sparqlSelect.getSelectFocus().get(0).replaceFirst("\\?", "")).getType().replaceAll("\"", "");
            String type2 = response.get(sparqlSelect.getSelectFocus().get(1).replaceFirst("\\?", "")).getType().replaceAll("\"", "");
            if (!type1.equals("bnode") && !type2.equals("bnode")) {
                if (!s1.equals("") && !s2.equals("")) {
                    Resource r1 = new Resource(s1);
                    Resource r2 = new Resource(s2);
                    if (r1.isIRI()) {
                        r1 = new IRI("<" + r1 + ">");
                    }
                    if (r2.isIRI()) {
                        r2 = new IRI("<" + r2 + ">");
                    }
                    PairAnswer pair = new PairAnswer(r1, r2);
                    answers.add(pair);
                }
            }
        }
        return answers;
    }

    private List<SubgraphForOutput> buildSubgraphForOutput(Set<SubgraphResult> goodSubgraphs) {
        List<SubgraphForOutput> output = new ArrayList<>();

        for (SubgraphResult result : goodSubgraphs) {
            boolean added = false;

            for (SubgraphForOutput subG : output) {
                added = subG.addSubgraph(result.getSubgraph(), result.getSimilarity());
                if (!added) break;
            }

            if (!added) {
                output.add(result.getSubgraph().toOutput(result.getSimilarity()));
            }

        }

        return output;
    }


    private List<SubgraphForOutput> filterSingleOutput(List<SubgraphForOutput> output) {
        List<SubgraphForOutput> singleOutput = new ArrayList<>();

        if (output.size() > 0 && output.get(output.size() - 1).getSimilarity() < 0.6 && output.get(output.size() - 1).getSimilarity() > 0.01) {
            double sim = output.get(output.size() - 1).getSimilarity();
            boolean moreCorrespondences = true;

            for (int i = output.size() - 1; i >= 0 && moreCorrespondences; i--) {
                if (output.get(i).getSimilarity() == sim) {
                    singleOutput.add(output.get(i));
                    System.out.println(output.get(i));
                } else {
                    moreCorrespondences = false;
                }
            }

        } else {
            output.stream()
                    .filter(subgraphForOutput -> subgraphForOutput.getSimilarity() >= 0.6)
                    .forEach(singleOutput::add);
        }
        return singleOutput;
    }


}

