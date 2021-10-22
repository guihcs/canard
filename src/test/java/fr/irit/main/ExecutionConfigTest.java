package fr.irit.main;

import fr.irit.input.ParameterException;
import fr.irit.sparql.query.select.SparqlSelect;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ExecutionConfigTest {


    @Test
    public void getQueries() throws IOException, ParameterException {
        ExecutionConfig executionConfig = new ExecutionConfig(new String[]{"/home/guilherme/IdeaProjects/Canard/runConfig.json"});


        SparqlSelect sparqlSelect = executionConfig.getQueries().get(0);

        System.out.println(sparqlSelect.getIRIList());
    }
}