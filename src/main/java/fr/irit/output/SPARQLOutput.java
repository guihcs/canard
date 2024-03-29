package fr.irit.output;

import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.subgraphs.similarity.TripleSimilarity;
import fr.irit.complex.subgraphs.unary.Triple;
import fr.irit.complex.subgraphs.unary.TripleSubgraph;
import fr.irit.complex.utils.Parameters;
import fr.irit.resource.IRI;
import fr.irit.resource.Resource;
import fr.irit.sparql.exceptions.IncompleteSubstitutionException;
import fr.irit.sparql.files.QueryTemplate;
import fr.irit.sparql.proxy.SparqlProxy;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.*;

public class SPARQLOutput extends Output {
    private final String outputEndpoint;
    private final Map<String, QueryTemplate> templates;
    private String outputAlignmentIRI;

    public SPARQLOutput(Parameters params) {
        super(params);
        outputEndpoint = params.getOutputEndpoint();
        templates = params.getQueryTemplates().get(outputEndpoint);

    }

    @Override
    public void init() {
        String outputIRI = "http://alignment-output#";
        String alignmentHashCode = sourceEndpoint.hashCode() + "-" + targetEndpoint.hashCode();

        Map<String, String> substitution = new HashMap<>();
        outputAlignmentIRI = "<" + outputIRI + "alignment_" + alignmentHashCode + ">";
        substitution.put("alignment", outputAlignmentIRI);
        substitution.put("sourceOntology", "<" + sourceEndpoint + ">");
        substitution.put("targetOntology", "<" + targetEndpoint + ">");
        substitution.put("date", "\"" + new Date() + "\"^^xsd:dateTime");
        String createAlignment = createAlignment(substitution);
        try {
            SparqlProxy.postSparqlUpdateQuery(outputEndpoint, createAlignment);
        } catch (SparqlQueryMalFormedException
                | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }

    }


    public String createAlignment(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_alignment").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }


    public String createCell(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_cell").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }


    public String createSourceSubgraph(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_sourcesubgraph").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    public String createTargetSubgraph(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_targetsubgraph").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }


    public String createTriple(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_triple").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }

    public void pushLabels(Set<String> labels, String uri) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        Map<String, String> substitution = new HashMap<>();
        for (String l : labels) {
            if (!l.equals("")) {
                substitution.put("uri", uri);
                substitution.put("label", "\"" + l + "\"");
                SparqlProxy.postSparqlUpdateQuery(outputEndpoint, createLabel(substitution));
            }
        }
    }

    public String createLabel(Map<String, String> substitution) {
        String query = "";
        try {
            query = templates.get("insert_iri_info").substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        }
        return query;
    }


    @Override
    public void addToOutput(List<SubgraphForOutput> output, SparqlSelect query) {
        String outputIRI = "http://alignment-output#";
        Map<String, String> substitution = new HashMap<>();

        String alignmentHashCode = sourceEndpoint.hashCode() + "-" + targetEndpoint.hashCode();

        substitution.put("alignment", outputAlignmentIRI);
        substitution.put("sourceSubgraph", "<" + outputIRI + "subgraph-source-" + query.hashCode() + alignmentHashCode + ">");
        substitution.put("competencyQuestion", "<" + outputIRI + "competencyQuestion" + query.hashCode() + alignmentHashCode + ">");
        substitution.put("SPARQLCQA", "\"" + query.toString().replaceAll("\n", " ").replaceAll("\"", "\\\\\"\\\\\"") + "\"");

        substitution.put("form", "\"" + query.toSubgraphForm() + "\"");
        String createSourceInstance = createSourceSubgraph(substitution);
        try {
            SparqlProxy.postSparqlUpdateQuery(outputEndpoint, createSourceInstance);
        } catch (SparqlQueryMalFormedException
                | SparqlEndpointUnreachableException e) {
            e.printStackTrace();
        }

        int i = 0;
        for (SubgraphForOutput s : output) {
            String cellHashCode = query.hashCode() + "-" + i + alignmentHashCode + "-" + i;
            substitution.put("cell", "<" + outputIRI + "cell_" + cellHashCode + ">");
            substitution.put("score", "\"" + s.getAverageSimilarity() + "\"^^xsd:float");
            substitution.put("targetSubgraph", "<" + outputIRI + "subgraph_" + cellHashCode + ">");
            substitution.put("intensionForm", "\"" + s.toIntensionString() + "\"");
            substitution.put("extensionForm", "\"" + s.toExtensionString() + "\"");
            try {
                SparqlProxy.postSparqlUpdateQuery(outputEndpoint, createCell(substitution));
            } catch (SparqlQueryMalFormedException
                    | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
            try {
                SparqlProxy.postSparqlUpdateQuery(outputEndpoint, createTargetSubgraph(substitution));
            } catch (SparqlQueryMalFormedException
                    | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }

            if (s instanceof TripleSubgraph tripleSubgraph) {
                int j = 0;
                for (Triple t : tripleSubgraph.getTriples()) {
                    TripleSimilarity tripleSimilarity = (TripleSimilarity) tripleSubgraph.getSimilarityMap().get(t);

                    substitution.put("triple", "<" + outputIRI + "triple_" + cellHashCode + j + ">");
                    substitution.put("predicate", t.getPredicate().toString());
                    substitution.put("similarity", "\"" + tripleSubgraph.getSimilarityMap().get(t) + "\"^^xsd:float");
                    substitution.put("instance", getAnswer(t).toString());
                    boolean keepObjectType = tripleSimilarity.canKeepObjectType(t);
                    if (keepObjectType) {
                        substitution.put("object", t.getObjectType().toString());
                    } else {
                        substitution.put("object", t.getObject().toValueString());
                    }
                    boolean keepSubjectType = tripleSimilarity.canKeepSubjectType(t);
                    if (keepSubjectType) {
                        substitution.put("subject", t.getSubjectType().toString());
                    } else {
                        substitution.put("subject", t.getSubject().toValueString());
                    }

                    substitution.put("objectSimilarity", "\"" + tripleSimilarity.getObjectSimilarity() + "\"^^xsd:float");
                    substitution.put("subjectSimilarity", "\"" + tripleSimilarity.getSubjectSimilarity() + "\"^^xsd:float");
                    substitution.put("predicateSimilarity", "\"" + tripleSimilarity.getPredicateSimilarity() + "\"^^xsd:float");
                    substitution.put("keepObjectType", "\"" + keepObjectType + "\"^^xsd:boolean");
                    substitution.put("keepSubjectType", "\"" + keepSubjectType + "\"^^xsd:boolean");
                    try {
                        SparqlProxy.postSparqlUpdateQuery(outputEndpoint, createTriple(substitution));


                        if (!t.isSubjectTriple() || !keepSubjectType) {
                            pushLabels(t.getSubject().getLabels(), t.getSubject().toString());
                        }
                        if (!t.isObjectTriple() || !keepObjectType) {
                            if (t.getObject() instanceof IRI to) {
                                pushLabels(to.getLabels(), t.getObject().toString());
                            }
                        }
                        pushLabels(t.getPredicate().getLabels(), t.getPredicate().toString());

                        if (keepSubjectType) {
                            pushLabels(t.getSubjectType().getLabels(), t.getSubjectType().toString());
                        }
                        if (keepObjectType) {
                            pushLabels(t.getObjectType().getLabels(), t.getObjectType().toString());
                        }
                    } catch (SparqlQueryMalFormedException
                            | SparqlEndpointUnreachableException e) {
                        e.printStackTrace();
                    }
                    j++;
                }
                i++;
            }
        }

    }


    public Resource getAnswer(Triple triple) {
        return switch (triple.getType()) {
            case NONE -> null;
            case SUBJECT -> triple.getSubject();
            case PREDICATE -> triple.getPredicate();
            case OBJECT -> triple.getObject();
        };
    }

    @Override
    public void end() {

    }

}
