package fr.irit.resource;

import fr.irit.complex.utils.Utils;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class IRITypeUtils {

    public static IRI findMostSimilarType(IRI iri, String endpointUrl, Set<String> targetLabels, double threshold) {
        double scoreTypeMax = -1;
        IRI finalType = null;

        try {
            if (iri.getTypes().isEmpty()) {
                retrieveTypes(iri, endpointUrl);
            }

            for (IRI type : iri.getTypes()) {
                double scoreType;
                type.retrieveLabels(endpointUrl);
                scoreType = Utils.similarity(type.getLabels(), targetLabels, threshold);
                if (scoreTypeMax < scoreType) {
                    scoreTypeMax = scoreType;
                    finalType = type;
                }
            }
        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }

        return finalType;
    }


    private static void retrieveTypes(IRI iri, String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        String query = SparqlSelect.buildTypesSelect(iri.getValue());
        SparqlSelect sq = new SparqlSelect(query);

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpointUrl);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(sq.getMainQueryWithPrefixes());

        for (Map<String, SelectResponse.Results.Binding> jsonNode : ret) {
            String s = jsonNode.get("type").getValue().replaceAll("\"", "");
            iri.getTypes().add(new IRI("<" + s + ">"));
        }

        for (IRI type : iri.getTypes()) {
            type.retrieveLabels(endpointUrl);
        }
    }

}
