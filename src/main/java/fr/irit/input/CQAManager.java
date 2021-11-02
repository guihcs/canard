package fr.irit.input;

import fr.irit.complex.utils.Parameters;
import fr.irit.sparql.exceptions.IncompleteSubstitutionException;

import java.util.Map;

public class CQAManager {

    private static CQAManager instance;

    private final Parameters params;

    private CQAManager(Parameters params) {
        this.params = params;
    }

    public static void init(Parameters params) {
        instance = new CQAManager(params);
    }

    public static CQAManager getInstance() {
        return instance;
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
}
