package fr.irit.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.irit.input.CQAManager;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRI extends Resource {
    private final HashSet<String> labels;
    private final HashSet<IRI> types;
    private boolean labelsGot;
    private final Pattern pattern = Pattern.compile("<([^>]+)[#/]([A-Za-z0-9_-]+)>");

    public IRI(String iri) {
        super(iri);
        labels = new HashSet<>();
        types = new HashSet<>();
        labelsGot = false;
    }

    public void retrieveLabels(String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (labelsGot) return;

        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            addLabel(matcher.group(2));
        } else {
            addLabel(value);
        }


        Map<String, String> substitution = new HashMap<>();
        substitution.put("uri", value);
        String literalQuery = CQAManager.getInstance().getLabelQuery(endpointUrl, substitution);
        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpointUrl);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(literalQuery);

        for (Map<String, SelectResponse.Results.Binding> jsonNode : ret) {
            String s = jsonNode.get("x").getValue().replaceAll("\"", "");
            Resource res = new Resource(s);
            if (!res.isIRI()) {
                addLabel(s);
            }

        }
        labelsGot = true;
    }

    public void retrieveTypes(String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException{
        String query = "SELECT DISTINCT ?type WHERE {" +
                value +" a ?type."
                + "filter(isIRI(?type))}";
        SparqlSelect sq = new SparqlSelect(query);

        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpointUrl);

        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(sq.getMainQueryWithPrefixes());

        for (Map<String, SelectResponse.Results.Binding> stringBindingMap : ret) {
            String s = stringBindingMap.get("type").getValue().replaceAll("\"", "");
            types.add(new IRI("<" + s + ">"));
        }
        for (IRI type: types){
            type.retrieveLabels(endpointUrl);
        }
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

    private static List<IRI> parseMatches(String endpoint, Map<String, String> substitution) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        List<IRI> allMatches = new ArrayList<>();
        String query = CQAManager.getInstance().getMatchedURIs(endpoint, substitution);
        SparqlProxy spIn = SparqlProxy.getSparqlProxy(endpoint);
        List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(query);
        for (Map<String, SelectResponse.Results.Binding> node : ret) {
            String s = node.get("x").getValue().replaceAll("\"", "");
            Resource res = new Resource(s);
            if (res.isIRI()) {
                allMatches.add(new IRI("<" + s + ">"));
            }
        }
        return allMatches;
    }


    public boolean existsInTarget(IRI match, String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        String queryMatch = SparqlSelect.buildExactMatchExistsAsk(match.toString());
        SparqlProxy spTarg = SparqlProxy.getSparqlProxy(targetEndpoint);
        return spTarg.sendAskQuery(queryMatch);
    }


    @Override
    public void findSimilarResource(String targetEndpoint) {
        try {
            if (labels.isEmpty()) {
                retrieveLabels(targetEndpoint);
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
                    SparqlProxy spIn = SparqlProxy.getSparqlProxy(targetEndpoint);
                    List<Map<String, SelectResponse.Results.Binding>> ret = spIn.getResponse(litteralQuery);

                    for (Map<String, SelectResponse.Results.Binding> jsonNode : ret) {
                        String s = jsonNode.get("x").getValue().replaceAll("\"", "");
                        similarIRIs.add(new IRI("<" + s + ">"));
                    }
                }
            }
        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }


    }


    public HashSet<String> getLabels() {
        return labels;
    }


    public Set<IRI> getTypes() {
        return types;
    }
}
