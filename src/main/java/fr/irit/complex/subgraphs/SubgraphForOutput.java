package fr.irit.complex.subgraphs;

import fr.irit.complex.subgraphs.unary.SimilarityValues;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SelectResponse;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SubgraphForOutput implements Comparable<SubgraphForOutput> {
    protected double similarity;

    public boolean addSubgraph(InstantiatedSubgraph t, SimilarityValues sim){
        return false;
    }

    public String toExtensionString() {
        return "";
    }

    public String toIntensionString() {
        return "";
    }

    public String toString() {
        return getAverageSimilarity() + " <-> " + toIntensionString();
    }

    public double getSimilarity() {
        return similarity;
    }

    public double getAverageSimilarity() {
        return similarity;
    }

    public void reassessSimilarityWithCounterExamples(String sourceEndpoint, String targetEndpoint, SparqlSelect sq) {
        SparqlProxy spTarg = SparqlProxy.getSparqlProxy(targetEndpoint);
        SparqlProxy spSource = SparqlProxy.getSparqlProxy(sourceEndpoint);
        double nbTrueExamples = 0;
        double nbCounterExamples = 0;
        double nbRetSource;
        try {
            List<Map<String, SelectResponse.Results.Binding>> retSource = spSource.getResponse(sq.toString());
            nbRetSource = retSource.size();
            int offset = 0;
            int limit = 10000;
            boolean end = false;

            while (!end) {
                String newQuery = toSPARQLForm();
                newQuery += "\n LIMIT " + limit;
                newQuery += "\n OFFSET " + offset;

                List<Map<String, SelectResponse.Results.Binding>> ret = spTarg.getResponse(newQuery);

                for(Map<String, SelectResponse.Results.Binding> response : ret) {
                    if(nbCounterExamples > 10 * nbRetSource) break;

                    if (response.containsKey("answer")) {
                        IRI iriResponse = new IRI("<" + response.get("answer").getValue().replaceAll("\"", "") + ">");
                        iriResponse.findExistingMatches(targetEndpoint, sourceEndpoint);
                        for (IRI sourceRes : iriResponse.getSimilarIRIs()) {
                            if (spSource.sendAskQuery("ASK{" + sq.toSubgraphForm().replaceAll("\\?answer", sourceRes.toString()) + "}")) {
                                nbTrueExamples += 1;
                            } else {
                                nbCounterExamples += 1;
                            }
                        }

                    }

                    if (response.containsKey("answer1")) {
                        Resource r1 = new Resource(response.get("answer0").getValue().replaceAll("\"", ""));
                        Resource r2 = new Resource(response.get("answer1").getValue().replaceAll("\"", ""));
                        List<Resource> valuesr1Source = new ArrayList<>();
                        List<Resource> valuesr2Source = new ArrayList<>();

                        addSimilarIfIRI(sourceEndpoint, targetEndpoint, r1, valuesr1Source);

                        addSimilarIfIRI(sourceEndpoint, targetEndpoint, r2, valuesr2Source);

                        for (Resource sourceRes1 : valuesr1Source) {
                            for (Resource sourceRes2 : valuesr2Source) {
                                String query = sq.toSubgraphForm();

                                if (sourceRes1.isIRI()) {
                                    query = query.replaceAll("\\?answer0", sourceRes1.toString());
                                } else {
                                    query += " Filter(str(?answer0)=" + sourceRes1.toValueString() + ")";
                                }

                                if (sourceRes2.isIRI()) {
                                    query = query.replaceAll("\\?answer1", sourceRes2.toString());
                                } else {
                                    query += " Filter(str(?answer1)=" + sourceRes2.toValueString() + ")";
                                }

                                query = "ASK{" + query + "}";

                                if (spSource.sendAskQuery(query)) {
                                    nbTrueExamples += 1;
                                } else {
                                    nbCounterExamples += 1;
                                }
                            }
                        }
                    }
                }


                if (ret.size() < limit) {
                    end = true;
                } else {
                    offset += limit;
                }
                if (nbCounterExamples >= 10 * nbRetSource) {
                    end = true;
                }
                if (offset > 600000) {
                    end = true;
                }
            }

        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }


        if (nbTrueExamples + nbCounterExamples == 0) {
            similarity = 0;
        } else {
            similarity *= nbTrueExamples / (nbTrueExamples + nbCounterExamples);
        }

    }

    private void addSimilarIfIRI(String sourceEndpoint, String targetEndpoint, Resource r1, List<Resource> valuesr1Source) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (r1.isIRI()) {
            r1 = new IRI("<" + r1 + ">");
            ((IRI) r1).findExistingMatches(targetEndpoint, sourceEndpoint);
            valuesr1Source.addAll(r1.getSimilarIRIs());
        } else {
            valuesr1Source.add(r1);
        }
    }

    public String toSPARQLForm() {
        return "";
    }

    @Override
    public int compareTo(SubgraphForOutput o) {
        return Double.compare(getSimilarity(), o.getSimilarity());
    }

}
