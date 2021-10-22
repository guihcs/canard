package fr.irit.sparql.query.data.insert;

import fr.irit.sparql.query.data.SparqlAbstractDataQuery;

import java.util.Map;
import java.util.Set;

public class SparqlInsertData extends SparqlAbstractDataQuery {
    public SparqlInsertData(Set<Map.Entry<String, String>> prefix, String data) {
        super(prefix, data);
        keyword = "INSERT DATA";
    }

    public void setKeyWords(String keyword) {
        this.keyword = keyword;
    }
}
