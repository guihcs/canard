package fr.irit.main;

import fr.irit.complex.ComplexAlignmentGeneration;
import fr.irit.input.ParameterException;
import fr.irit.sparql.query.exceptions.SparqlEndpointUnreachableException;
import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;

import java.io.IOException;

public class Main {


    public static void main(String[] args) {

        try {
            System.out.println("===============================================================================");
            ExecutionConfig executionConfig = new ExecutionConfig(args);
            ComplexAlignmentGeneration complexAlignmentGeneration = new ComplexAlignmentGeneration(executionConfig);
            System.out.println("Running with " + executionConfig.getMaxMatches() + " support instances - " + executionConfig.getSimilarityThreshold() + " similarity.");

            if (executionConfig.isSystemCanRun()) {
                complexAlignmentGeneration.run();
            }

            executionConfig.end();
            System.out.println("Matching process ended");


        } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException | IOException ex) {
            ex.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("At least 1 argument is expected, you provided: " + args.length +
                    "\nPlease refer to Readme.md file");
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
        }


    }
}
