package fr.irit.complex.utils;

import fr.irit.input.ParameterException;
import org.junit.jupiter.api.Test;

import java.io.IOException;


import static org.junit.jupiter.api.Assertions.*;

class ParametersTest {

    @Test
    void init() throws IOException, ParameterException {

        Parameters parameters = Parameters.load("src/test/resources/runConfig.json");

        assertEquals(1, parameters.getQueries().size());
        assertTrue(parameters.getQueryTemplates().containsKey("http://localhost:3031/targetOntology/"));
        assertTrue(parameters.getQueryTemplates().containsKey("http://localhost:3031/sourceOntology/"));
        assertEquals(3, parameters.getQueryTemplates().get("http://localhost:3031/targetOntology/").size());
        assertEquals(3, parameters.getQueryTemplates().get("http://localhost:3031/sourceOntology/").size());

    }



}