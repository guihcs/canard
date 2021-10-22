package fr.irit.sparql.files;

import fr.irit.sparql.exceptions.IncompleteSubstitutionException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is made to ease the reuse of predefined queries. Their syntax is inspired by the
 * Jinja template syntax (http://jinja.pocoo.org/), where variables can be specified in html
 * code between double curly brackets : {{ myVariableName }}. These variables are then substituted
 * with their value before rendering the HTML. The principle is the same here, with SPARQL queries.
 */
public class QueryTemplate {
    private final String query;
    private final Set<String> toSubstitute;

    public QueryTemplate(String query) {
        this.query = query;
        toSubstitute = new HashSet<>();
        Pattern p = Pattern.compile("\\{\\{ ?([A-Za-z0-9]+) ?}}");
        Matcher m = p.matcher(query);
        while (m.find()) {
            toSubstitute.add(m.group(1));
        }
    }

    /**
     * @return a query, where the unbound parameters are instantiated according to the substitution map
     */
    public String substitute(Map<String, String> substitution) throws IncompleteSubstitutionException {
        String query = this.query;
        if (substitution.keySet().containsAll(toSubstitute)) {
            for (String key : toSubstitute) {
                query = query.replaceAll("\\{\\{ ?" + key + " ?}}", substitution.get(key));
            }
        } else {
            throw new IncompleteSubstitutionException("Some elements of the substitution " + toSubstitute + "are not resolved by " + substitution);
        }
        return query;
    }
}
