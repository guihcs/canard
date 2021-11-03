package fr.irit.complex.subgraphs.binary;

import fr.irit.complex.subgraphs.InstantiatedSubgraph;
import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.subgraphs.unary.SimilarityValues;
import fr.irit.resource.IRI;
import fr.irit.resource.IRITypeUtils;
import fr.irit.resource.Resource;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;

import java.util.*;

public class Path extends InstantiatedSubgraph {
    final List<Resource> entities;
    final List<IRI> types;
    final List<Boolean> inverse;
    List<IRI> properties;
    double similarity;
    double typeSimilarity;

    public Path(Resource x, Resource y, String sparqlEndpoint, int length, List<Boolean> inverse) {
        properties = new ArrayList<>();
        entities = new ArrayList<>();
        types = new ArrayList<>();
        similarity = 0;
        this.inverse = inverse;
        findPathWithLength(x, y, sparqlEndpoint, length);

    }

    private void findPathWithLength(Resource x, Resource y, String sparqlEndpoint, int length) {
        String query;
        StringBuilder queryBody = new StringBuilder();
        List<String> variables = new ArrayList<>();

        variables.add(!x.isIRI() ? "?x" : x.toString());


        for (int i = 1; i < length; i++) {
            variables.add("?v" + i);
        }

        variables.add(!y.isIRI() ? "?y" : y.toString());


        for (int i = 1; i <= length; i++) {
            int v1 = i - 1;
            int v2 = i;

            if (inverse.get(i - 1)) {
                v1 = i;
                v2 = i - 1;
            }

            queryBody.append(variables.get(v1))
                    .append(" ?p")
                    .append(i)
                    .append(" ")
                    .append(variables.get(v2))
                    .append(". \n");

        }

        if (!x.isIRI()) {
            queryBody.append("   filter (regex(?x, \"^").append(x).append("$\",\"i\"))\n");
        }
        if (!y.isIRI()) {
            queryBody.append("   filter (regex(?y, \"^").append(y).append("$\",\"i\"))\n");
        }

        query = "SELECT DISTINCT * WHERE { " + queryBody + " }  LIMIT 20";

        SparqlProxy spProx = SparqlProxy.getSparqlProxy(sparqlEndpoint);
        List<Map<String, SelectResponse.Results.Binding>> ret;

        try {
            ret = spProx.getResponse(query);
            Iterator<Map<String, SelectResponse.Results.Binding>> retIteratorTarg = ret.iterator();
            if (retIteratorTarg.hasNext()) {
                Map<String, SelectResponse.Results.Binding> next = retIteratorTarg.next();
                if (next.containsKey("x")) {
                    entities.add(new Resource(next.get("x").getValue().replaceAll("\"", "")));
                } else {
                    entities.add(x);
                }
                int i = 1;
                boolean stop = false;
                while (i <= length && !stop) {
                    String p = next.get("p" + i).getValue();
                    Resource res = new Resource(p);
                    if (p.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        stop = true;
                    }
                    if (p.equals("http://www.w3.org/2002/07/owl#sameAs")) {
                        stop = true;
                    }
                    if (p.equals("http://www.w3.org/2004/02/skos/core#exactMatch")) {
                        stop = true;
                    }
                    if (p.equals("http://www.w3.org/2004/02/skos/core#closeMatch")) {
                        stop = true;
                    }
                    if (p.equals("http://dbpedia.org/ontology/wikiPageWikiLink")) {
                        stop = true;
                    }
                    if (res.isIRI()) {
                        properties.add(new IRI("<" + p + ">"));
                    }
                    i++;
                }
                if (stop) {
                    properties = new ArrayList<>();
                }
                if (length >= 2 && !stop) {
                    for (int j = 1; j <= length - 1; j++) {
                        String v = next.get("v" + j).getValue();
                        Resource res = new Resource(v);
                        if (res.isIRI()) {
                            entities.add(new IRI("<" + v + ">"));
                        } else {
                            entities.add(res);
                        }
                    }
                }
                if (next.containsKey("y")) {
                    entities.add(new Resource(next.get("y").getValue().replaceAll("\"", "")));
                } else {
                    entities.add(y);
                }

            }


        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }
    }



    public boolean pathFound() {
        return !properties.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            ret.append(entities.get(i)).append(" ").append(properties.get(i)).append(" ").append(entities.get(i + 1)).append(".  ");
        }
        return ret.toString();
    }

    public String toSubGraphString() {
        StringBuilder ret = new StringBuilder();
        ArrayList<String> variables = new ArrayList<>();
        variables.add("?answer0");
        for (int i = 1; i <= properties.size() - 1; i++) {
            variables.add("?v" + i);
        }
        variables.add("?answer1");

        for (int i = 0; i < properties.size(); i++) {
            String xStr = variables.get(i);
            String yStr = variables.get(i + 1);

            if (types.get(i) != null) {
                ret.append(xStr).append(" a ").append(types.get(i)).append(".  ");
            }
            if (inverse.get(i)) {
                ret.append(yStr).append(" ").append(properties.get(i)).append(" ").append(xStr).append(".  ");
            } else {
                ret.append(xStr).append(" ").append(properties.get(i)).append(" ").append(yStr).append(".  ");
            }
        }
        if (types.get(properties.size()) != null) {
            ret.append(variables.get(properties.size())).append(" a ").append(types.get(properties.size())).append(".  ");
        }
        return ret.toString();
    }

    public void getMostSimilarTypes(String endpointUrl, Set<String> targetLabels, double threshold) {
        for (Resource r : entities) {
            if (r instanceof IRI ri) {
                IRI type = IRITypeUtils.findMostSimilarType(ri, endpointUrl, targetLabels, threshold);
                types.add(type);
            } else {
                types.add(null);
            }
        }
    }

    public List<IRI> getProperties() {
        return properties;
    }

    public List<IRI> getTypes() {
        return types;
    }

    public List<Boolean> getInverse() {
        return inverse;
    }


    @Override
    public SubgraphForOutput toOutput(SimilarityValues sim) {

        return new PathSubgraph(this, sim.similarity());
    }


    public List<Resource> getEntities() {
        return entities;
    }


    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public double getTypeSimilarity() {
        return typeSimilarity;
    }

    public void setTypeSimilarity(double typeSimilarity) {
        this.typeSimilarity = typeSimilarity;
    }
}
