package fr.irit.sparql.query.ask;

import fr.irit.sparql.query.SparqlQuery;

import java.util.Map.Entry;
import java.util.Set;

public class SparqlAsk extends SparqlQuery {
    public SparqlAsk(Set<Entry<String, String>> prefix, String data) {
        super(prefix, "", data);
    }

    public String toString() {
        return formatPrefixes() + " ASK{" + getWhere() + "}";
    }

}
