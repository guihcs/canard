package fr.irit.output;

import fr.irit.complex.subgraphs.SubgraphForOutput;
import fr.irit.complex.utils.Parameters;
import fr.irit.sparql.query.select.SparqlSelect;

import java.util.List;

public abstract class Output {

    protected final String sourceEndpoint;
    protected final String targetEndpoint;

    public Output(Parameters params) {
        sourceEndpoint = params.getSourceEndpoint();
        targetEndpoint = params.getTargetEndpoint();

    }

    public abstract void init();

    public abstract void addToOutput(List<SubgraphForOutput> output, SparqlSelect sq);

    public abstract void end();

}
