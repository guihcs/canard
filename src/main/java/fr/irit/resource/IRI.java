package fr.irit.resource;

import fr.irit.input.CQAManager;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;
import java.util.regex.Pattern;

public class IRI extends Resource {
    private final Set<String> labels;
    private final Set<IRI> types;
    private boolean labelsGot;
    private final Pattern pattern = Pattern.compile("<([^>]+)[#/]([A-Za-z0-9_-]+)>");

    public IRI(String iri) {
        super(iri);
        labels = new HashSet<>();
        types = new HashSet<>();
        labelsGot = false;
    }





    public void addLabel(String label) {
        labels.add(label.trim().replaceAll("\\\\", "").toLowerCase());
    }

    public void findExistingMatches(String sourceEndpoint, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        List<IRI> allMatches = new ArrayList<>();
        allMatches.add(this);

        Map<String, String> substitution = new HashMap<>();
        substitution.put("uri", value.replaceAll("\\$", ""));

        allMatches.addAll(IRI.parseMatches(sourceEndpoint, substitution));
        allMatches.addAll(IRI.parseMatches(targetEndpoint, substitution));

        for (IRI match : allMatches) {
            if (existsInTarget(match, targetEndpoint)) {
                similarIRIs.add(match);
            }
        }

    }

    private static List<IRI> parseMatches(String endpoint, Map<String, String> substitution) {
        List<IRI> allMatches = new ArrayList<>();
        String query = CQAManager.getInstance().getMatchedURIs(endpoint, substitution);
        List<Map<String, String>> ret = SparqlProxy.getResponse(endpoint, query);
        for (Map<String, String> node : ret) {
            String s = node.get("x");
            Resource res = new Resource(s);
            if (res.isIRI()) {
                allMatches.add(new IRI("<" + s + ">"));
            }
        }
        return allMatches;
    }


    public boolean existsInTarget(IRI match, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        String queryMatch = SparqlSelect.buildExactMatchExistsAsk(match.toString());
        return SparqlProxy.sendAskQuery(targetEndpoint, queryMatch);
    }


    @Override
    public void findSimilarResource(String targetEndpoint) {
        if (labels.isEmpty()) {
            IRIUtils.retrieveLabels(this, targetEndpoint);
        }

        for (String rawLab : labels) {

            String label = rawLab.replaceAll("[\\^+{}.?]", "");
            Map<String, String> substitution = new HashMap<>();
            substitution.put("labelValue", label.toLowerCase());

            substitution.put("LabelValue", label.length() > 1 ?
                    label.substring(0, 1).toUpperCase() + label.substring(1) :
                    label.toUpperCase());

            String litteralQuery = CQAManager.getInstance().getSimilarQuery(targetEndpoint, substitution);

            if (litteralQuery.length() < 2000) {
                List<Map<String, String>> ret = SparqlProxy.getResponse(targetEndpoint, litteralQuery);

                for (Map<String, String> jsonNode : ret) {
                    String s = jsonNode.get("x");
                    similarIRIs.add(new IRI("<" + s + ">"));
                }
            }
        }


    }


    public Set<String> getLabels() {
        return labels;
    }


    public Set<IRI> getTypes() {
        return types;
    }


    public boolean isLabelsGot() {
        return labelsGot;
    }

    public void setLabelsGot(boolean labelsGot) {
        this.labelsGot = labelsGot;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
