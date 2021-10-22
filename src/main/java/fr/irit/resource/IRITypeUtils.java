package fr.irit.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.complex.utils.Utils;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.ArrayList;
import java.util.Set;

public class IRITypeUtils {

    public static IRI findMostSimilarType(IRI iri, String endpointUrl, Set<String> targetLabels, double threshold) {
        if (iri.getTypes().isEmpty()) {
            try {
                retrieveTypes(iri, endpointUrl);
            } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
        }
        double scoreTypeMax = -1;
        IRI finalType = null;
        for (IRI type : iri.getTypes()) {
            double scoreType;
            try {
                type.retrieveLabels(endpointUrl);
            } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
            scoreType = Utils.similarity(type.getLabels(), targetLabels, threshold);
            if (scoreTypeMax < scoreType) {
                scoreTypeMax = scoreType;
                finalType = type;
            }
        }
        return finalType;
    }


    private static void retrieveTypes(IRI iri, String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        String query = "SELECT DISTINCT ?type WHERE {" +
                iri.getValue() + " a ?type."
                + "filter(isIRI(?type))}";
        SparqlSelect sq = new SparqlSelect(query);

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpointUrl);

        ArrayList<JsonNode> ret = spIn.getResponse(sq.getMainQueryWithPrefixes());

        for (JsonNode jsonNode : ret) {
            String s = jsonNode.get("type").get("value").toString().replaceAll("\"", "");
            iri.getTypes().add(new IRI("<" + s + ">"));
        }
        for (IRI type : iri.getTypes()) {
            type.retrieveLabels(endpointUrl);
        }
    }

}
