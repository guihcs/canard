package fr.irit.main;

import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.utils.Parameters;
import fr.irit.input.CQAManager;
import fr.irit.input.ParameterException;
import fr.irit.output.EDOALOutput;
import fr.irit.output.Output;
import fr.irit.output.QueryOutput;
import fr.irit.output.SPARQLOutput;
import fr.irit.sparql.client.EmbeddedFuseki;
import fr.irit.sparql.query.select.SparqlSelect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExecutionConfig {

    private final String sourceEndpoint;
    private final String targetEndpoint;
    private int maxMatches;
    private double similarityThreshold;
    private boolean reassess;
    private Parameters params;
    private List<Output> outputs;

    public ExecutionConfig(String[] args) throws IOException, IllegalArgumentException, ParameterException {
        if (args.length < 1) {
            throw new IllegalArgumentException();
        }
        init(args[0]);
        sourceEndpoint = params.getSourceEndpoint();
        targetEndpoint = params.getTargetEndpoint();
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

    private void init(String filePath) throws ParameterException, IOException {
        params = Parameters.load(filePath);
        CQAManager.init(params);
        outputs = new ArrayList<>();
        if (params.isOutputEDOAL()) {
            outputs.add(new EDOALOutput(params));
        }
        if (params.isOutputQUERY()) {
            outputs.add(new QueryOutput(params));

        }
        if (params.isOutputSPARQL()) {
            outputs.add(new SPARQLOutput(params));
        }

        for (Output o : outputs) {
            o.init();
        }
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
        return params.getQueries();
    }


    public void end(){
        if (params.isStartEmbeddedFuseki()) {
            EmbeddedFuseki fusekiServer = EmbeddedFuseki.getFusekiServer();
            fusekiServer.closeConnection();

        }
        for (Output o : outputs) {
            o.end();
        }
    }


    public void addToOutput(SparqlSelect sparqlSelect, List<SubgraphForOutput> subgraphForOutputs){
        for (Output o : outputs) {
            o.addToOutput(subgraphForOutputs, sparqlSelect);
        }
    }
}
