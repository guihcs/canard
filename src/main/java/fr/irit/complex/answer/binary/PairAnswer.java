package fr.irit.complex.answer.binary;

import fr.irit.complex.answer.Answer;
import fr.irit.complex.answer.SubgraphResult;
import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.binary.Path;
import fr.irit.complex.subgraphs.similarity.PathSimilarity;
import fr.irit.main.ExecutionConfig;
import fr.irit.resource.IRI;
import fr.irit.resource.IRIUtils;
import fr.irit.resource.Resource;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;

public class PairAnswer extends Answer {
    private final Resource r1;
    private final Resource r2;
    private boolean similarLooked;

    public PairAnswer(Resource r1, Resource r2) {
        this.r1 = r1;
        this.r2 = r2;
        similarLooked = false;
    }

    @Override
    public void retrieveIRILabels(String endpointURL) {
        if (r1 instanceof IRI ri) {
            IRIUtils.retrieveLabels(ri, endpointURL);
        }
        if (r2 instanceof IRI ri) {
            IRIUtils.retrieveLabels(ri, endpointURL);
        }

    }
    @Override
    public void getSimilarIRIs(String targetEndpoint) {
        if (r1 instanceof IRI ri) {
            ri.findSimilarResource(targetEndpoint);
        }
        if (r2 instanceof IRI ri) {
            ri.findSimilarResource(targetEndpoint);
        }
    }
    @Override
    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        if (r1 instanceof IRI ri) {
            ri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }
        if (r2 instanceof IRI ri) {
            ri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }

    }


    private Set<SubgraphResult> findCorresponding(Set<String> queryLabels, SparqlSelect query, ExecutionConfig executionConfig, int currentPath, int maxPath){
        Set<SubgraphResult> paths = new HashSet<>();
        if( currentPath > maxPath) return paths;
        Set<InstantiatedSubgraph> paths1 = getPaths(executionConfig, queryLabels);


        if (paths1.isEmpty() && !similarLooked) {
            getSimilarIRIs(executionConfig.getTargetEndpoint());
            System.out.println("No path found, similar answers : " + printMatchedEquivalents());
            paths = findCorresponding(queryLabels, query, executionConfig, currentPath + 1, maxPath);
            similarLooked = true;
        }

        if (!paths1.isEmpty()) {
            for (InstantiatedSubgraph p : paths1) {
                if (p instanceof Path pa) {

                    double similarity =  IRIUtils.comparePathLabel(pa, queryLabels, executionConfig.getSimilarityThreshold(), executionConfig.getTargetEndpoint(), 0.5);

                    PathSimilarity pathSimilarity = new PathSimilarity();
                    pa.setSimilarity(similarity);
                    paths.add(new SubgraphResult(p, pathSimilarity));
                } else {
                    System.err.println("problem in Pair answer: instantiated subgraph is not a path...");
                }
            }
        }

        return paths;

    }

    @Override
    public Set<SubgraphResult> findCorrespondingSubGraph(Set<String> queryLabels, SparqlSelect query, ExecutionConfig executionConfig) {

        return findCorresponding(queryLabels, query, executionConfig, 0, 5);
    }


    private Set<InstantiatedSubgraph> getPaths(ExecutionConfig executionConfig, Set<String> queryLabels){
        Set<InstantiatedSubgraph> paths = new HashSet<>();
        if (hasTotalMatch()) {
            for (Resource x : r1.getSimilarIRIs()) {
                for (Resource y : r2.getSimilarIRIs()) {
                    paths.addAll(findMostSimilarPaths(executionConfig, queryLabels, x, y));
                }
            }
        }

        if (paths.isEmpty() && hasR1Match()) {
            if (!r2.isIRI()) {
                for (IRI x : r1.getSimilarIRIs()) {
                    paths.addAll(findMostSimilarPaths(executionConfig, queryLabels, x, r2));
                }

            }
        }

        if (paths.isEmpty() && hasR2Match()) {
            if (!r1.isIRI()) {
                for (IRI y : r2.getSimilarIRIs()) {
                    paths.addAll(findMostSimilarPaths(executionConfig, queryLabels, r1, y));
                }

            }

        }
        return paths;
    }

    private Set<InstantiatedSubgraph> findMostSimilarPaths(ExecutionConfig executionConfig, Set<String> queryLabels, Resource x, Resource y) {
        Set<InstantiatedSubgraph> paths = new HashSet<>();
        int length = 1;
        boolean found = false;
        while (length < 4 && !found) {
            List<List<Boolean>> allInv = allInversePossibilities(length);
            for (List<Boolean> invArray : allInv) {
                Path p = new Path(x, y, executionConfig.getTargetEndpoint(), length, invArray);
                if (p.pathFound()) {
                    p.getMostSimilarTypes(executionConfig.getTargetEndpoint(), queryLabels, 0.0);
                    paths.add(p);
                    found = true;
                }
            }
            length++;
        }
        return paths;
    }

    @Override
    public boolean hasMatch() {
        boolean match = !r1.isIRI() || hasR1Match();
        if (r2.isIRI() && !hasR2Match()) {
            match = false;
        }
        return match;
    }

    private boolean hasR1Match() {
        return !r1.getSimilarIRIs().isEmpty();
    }

    private boolean hasR2Match() {
        return !r2.getSimilarIRIs().isEmpty();
    }

    private boolean hasTotalMatch() {
        return hasR1Match() && hasR2Match();
    }
    @Override
    public String toString() {
        return r1.toString() + " " + r2.toString();
    }

    private List<List<Boolean>> allInversePossibilities(int length) {
        List<List<Boolean>> result = new ArrayList<>();
        for (int i = 0; i < Math.pow(2, length); i++) {
            List<Boolean> invArray = new ArrayList<>();
            StringBuilder invStr = new StringBuilder(Integer.toBinaryString(i));
            while (invStr.length() < length) {
                invStr.insert(0, "0");
            }
            for (char invCh : invStr.toString().toCharArray()) {
                if (invCh == '0') {
                    invArray.add(false);
                } else if (invCh == '1') {
                    invArray.add(true);
                }
            }
            result.add(invArray);
        }


        return result;

    }
    @Override
    public String printMatchedEquivalents() {
        return r1.getSimilarIRIs().toString() + " <--> " + r2.getSimilarIRIs().toString();
    }

}
