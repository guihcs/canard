# Complex Alignment System based on CQA and Instances

## General information
This system 
Note: this approach does not deal with CQA containing UNION for the moment.

## Query templates
- A folder containing the query templates must be accessible by the system.
- A default query template folder is accessible but each ontology has its specifities
- List of query templates:
  * labels.sparql: query to retrieve the labels (?x) of a an entity defined as {{uri}}
  * matched.sparql: query to retrieve all entities (?x) linked to a given entity {{uri}}
  * similar.sparql: query to retrieve entities (?x) which are similar to another entity. In these implementations, the uris are similar if they share a label {{labelValue}}.

## Parameter File structure
- Note: all endpoint addresses must end with /.

### Source and Target ontology fields
Each of these fields (*source_ontology* and *target_ontology*) is meant for the description of the ontologies to be aligned
List of fields and their meaning:
- *file*: path to ontology file (not needed in this first version as everything goes through the sparql endpoint)
- *sparqlEndpoint*: accessible sparqlEndpoint with populated ontology
- *query_template*: path to directory containing the query templates for the ontology
- *name*: ontology name in order to name the alignment 

### Competency question
This approach is based on competency questions for alignment creation.
A set of Competency questions in the form of SPARQL queries over the source ontology is required.
- *CQAs_SPARQL_folder*: path to directory containing the competency questions. This directory must have one file per competency question.

### Output
Various output forms are possible: into a EDOAL file, into a SPARQL endpoint (with a custom ontology to represent the outputs (c.f. output-ontology.owl).
The system can output the correspondences in different manners. The *output* field of the parameter file is an array of output objects: each object *type* must be given.
List of possible outputs:
- *edoal*: Outputs the alignment into an EDOAL file. If no *file* path is given, it goes to "./output.edoal".
- *sparql*: Outputs the alignment into a specified SPARQL *endpoint*.
- *query*: Outputs an equivalent SPARQL query for each source CQA into a defined *folder* (a sub-folder is created for each CQA and all the possible SPARQL queries are output in different files in the subfolders)

### Example of parameter file

{   "source_ontology": 
	{ "sparqlEndpoint":"http://localhost:3030/AgronomicTaxon/",
      "name": "agronomicTaxon"},
    "target_ontology": 
	{ "sparqlEndpoint":"http://localhost:3030/AgroVoc/",
      "query_templates": "query_templates/AgroVoc/"},
    "CQAs_SPARQL_folder" : "needs/agrotaxon",
    "output": [
       {"type":"edoal",
  	    "file": "output/agronomic-agrovoc.edoal"},
	   {"type": "sparql",
	    "endpoint": "http://localhost:3030/output/"}
	  ]
}
