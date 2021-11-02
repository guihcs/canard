
# 4 Match

The step 4 is in the method [getAnswers][1].

The method works as follows: first the method gets the source answers of the CQA query.
The method is inside a loop to ensure a match count is satisfied. There is some offset and a match limit.
The offset and limit are used in the query.

To get the source answers, the query is built and is sent to the source endpoint by http request.
The responses are in json format and need to be converted in [single answers][2]
or [pair answers][3] according to the query arity.

With the results in hand. The system try to find for each answer exact matches in the target ontology.
This match is delegated to the [IRI][5] class in the method *findExistingMatches*.

The method uses the *matched* query template to find exact matches.

```sparql
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
SELECT DISTINCT ?x WHERE {
    {?x rdfs:seeAlso {{uri}}.}
UNION{?x owl:sameAs {{uri}}.}
UNION{?x skos:closeMatch {{uri}}.}
UNION{?x skos:exactMacth {{uri}}.}
UNION{{{uri}} rdfs:seeAlso ?x.}
UNION{{{uri}} owl:sameAs ?x.}
UNION{{{uri}} skos:closeMatch ?x.}
UNION{{{uri}} skos:exactMatch ?x.}
}
```

If the process return empty matches the system tries to find similar matches.
This process is delegated to the [Resource][4] class in the method *findSimilarResource*.
The method uses the similar query template that uses a regex expression:

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX skos-xl: <http://www.w3.org/2008/05/skos-xl#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>


SELECT DISTINCT ?x WHERE {
{?x ?z ?label.}
UNION {?x skos-xl:prefLabel ?z.
?z skos-xl:literalForm ?label.}
filter (regex(?label, "^{{labelValue}}$","i")).
}

```

This ends the step 4.

[1]: ../src/main/java/fr/irit/complex/ComplexAlignmentGeneration.java
[2]: ../src/main/java/fr/irit/complex/answer/unary/SingleAnswer.java
[3]: ../src/main/java/fr/irit/complex/answer/binary/PairAnswer.java
[4]: ../src/main/java/fr/irit/resource/Resource.java
[4]: ../src/main/java/fr/irit/resource/IRI.java