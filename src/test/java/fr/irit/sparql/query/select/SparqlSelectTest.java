package fr.irit.sparql.query.select;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlSelectTest {



    @Test
    public void main(){
        SparqlSelect sparqlSelect = new SparqlSelect("""
                SELECT distinct ?s WHERE { \s
                { ?s <http://cmt#hasDecision> ?o2 . ?o2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://cmt#Acceptance> . } UNION  { ?s <http://cmt#acceptedBy> ?o2 . } \s
                 }""");


    }

}