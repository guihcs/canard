package fr.irit.complex.utils;

import fr.irit.input.ParameterException;
import fr.irit.output.EDOALOutput;
import fr.irit.output.Output;
import fr.irit.output.QueryOutput;
import fr.irit.output.SPARQLOutput;
import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.sparql.client.EmbeddedFuseki;
import fr.irit.sparql.exceptions.IncompleteSubstitutionException;
import fr.irit.sparql.query.select.SparqlSelect;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.util.*;

public final class Utils {
    public static final Utils instance = new Utils();

    private Parameters params;
    private List<Output> outputs;


    private Utils() {
    }

    public static Utils getInstance() {
        return instance;
    }


    public static double similarity(Set<String> labels1, Set<String> labels2, double threshold) {
        double score = 0;
        for (String l1 : labels1) {
            for (String l2 : labels2) {
                score += stringSimilarity(l1, l2, threshold);
            }
        }
        return score;
    }

    public static double stringSimilarity(String s1, String s2, double threshold) {

        double dist = LevenshteinDistance.getDefaultInstance().apply(s1.toLowerCase(), s2.toLowerCase()) / ((double) Math.max(s1.length(), s2.length()));
        double sim = 1 - dist;

        if (sim < threshold) {
            return 0;
        } else {
            return sim;
        }
    }


    public void init(String filePath) throws IOException, ParameterException {
        params = Parameters.load(filePath);
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


    public String getLabelQuery(String endpoint, Map<String, String> substitution) {
        String query = "";
        try {
            query = params.getQueryTemplates().get(endpoint).get("labels").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    public String getSimilarQuery(String endpoint, Map<String, String> substitution) {
        String query = "";
        try {
            query = params.getQueryTemplates().get(endpoint).get("similar").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    public String getMatchedURIs(String endpoint, Map<String, String> substitution) {
        String query = "";
        try {
            query = params.getQueryTemplates().get(endpoint).get("matched").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }


    public void addToOutput(SparqlSelect sq, List<SubgraphForOutput> subGraph) {
        for (Output o : outputs) {
            o.addToOutput(subGraph, sq);
        }

    }


    public void end() {
        if (params.isStartEmbeddedFuseki()) {
            EmbeddedFuseki fusekiServer = EmbeddedFuseki.getFusekiServer();
            fusekiServer.closeConnection();

        }


        for (Output o : outputs) {
            o.end();
        }
    }


    public String getSourceEndpoint() {
        return params.getSourceEndpoint();
    }

    public String getTargetEndpoint() {
        return params.getTargetEndpoint();
    }

    public List<SparqlSelect> getQueries() {
        return params.getQueries();
    }


}
