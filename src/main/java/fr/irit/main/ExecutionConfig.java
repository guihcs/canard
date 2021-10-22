package fr.irit.main;

import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.input.ParameterException;
import fr.irit.complex.utils.Utils;
import fr.irit.sparql.query.select.SparqlSelect;

import java.io.IOException;
import java.util.List;

public class ExecutionConfig {

    private final String sourceEndpoint;
    private final String targetEndpoint;
    private int maxMatches;
    private double similarityThreshold;
    private boolean reassess;
    private final Utils utils;

    public ExecutionConfig(String[] args) throws IOException, IllegalArgumentException, ParameterException {
        if (args.length < 1) {
            throw new IllegalArgumentException();
        }
        utils = Utils.getInstance();
        utils.init(args[0]);
        sourceEndpoint = utils.getSourceEndpoint();
        targetEndpoint = utils.getTargetEndpoint();
        maxMatches = 10;
        similarityThreshold = 0.4;
        reassess = false;
        if (args.length == 3) {
            maxMatches = Integer.parseInt(args[1]);
            similarityThreshold = Double.parseDouble(args[2]);
        } else if (args.length == 4) {
            maxMatches = Integer.parseInt(args[1]);
            similarityThreshold = Double.parseDouble(args[2]);
            if (args[3].equals("reassess")) {
                reassess = true;
            }
        }
    }


    public boolean isSystemCanRun() {
        return true;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public int getMaxMatches() {
        return maxMatches;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public boolean isReassess() {
        return reassess;
    }

    public List<SparqlSelect> getQueries(){
        return utils.getQueries();
    }


    public void end(){
        utils.end();
    }


    public void addToOutput(SparqlSelect sparqlSelect, List<SubgraphForOutput> subgraphForOutputs){
        utils.addToOutput(sparqlSelect, subgraphForOutputs);
    }
}
