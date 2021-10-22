package fr.irit.sparql.query.select;

import fr.irit.sparql.query.SparqlQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A select query. No keyword has to be explicitly written, except for aggregation attributes and filters.
 */
public class SparqlSelect extends SparqlQuery {
    private String select;
    private ArrayList<String> selectFocus;

    public SparqlSelect(Set<Map.Entry<String, String>> prefix, String from, String select,
                        String where) {
        super(prefix, from, where);
        setSelect(select);
    }

    public SparqlSelect(Set<Map.Entry<String, String>> prefix, String select, String where) {
        super(prefix, "", where);
        setSelect(select);
    }

    public SparqlSelect(String select, String where) {
        super(new HashSet<>(), "", where);
        setSelect(select);
    }

    public SparqlSelect(String query) {
        super(query);
        mainQuery = mainQuery.trim().replaceAll("SELECT", "select").replaceAll("WHERE", "where").replaceAll("\n", " ");
        selectFocus = new ArrayList<>();
        Pattern pattern = Pattern.compile("""
                select[ \t
                distncDISTNC]+(\\?[A-Za-z0-9_-]+)[ \t
                ]+(\\?*[A-Za-z0-9_-]*[ \t
                ]*)where[ \t
                ]*\\{(.+)}[ \t
                ]*$""");
        Matcher matcher = pattern.matcher(mainQuery);
        while (matcher.find()) {
            selectFocus.add(matcher.group(1).trim());
            if (!matcher.group(2).trim().isEmpty()) {
                selectFocus.add(matcher.group(2).trim());
            }
            where = matcher.group(3).trim();
        }
        Pattern pattern2 = Pattern.compile("""
                select([ \t
                distncDISTNC]+\\?[A-Za-z0-9_-]+[ \t
                ]+\\?*[A-Za-z0-9_-]*[ \t
                ]*)where""");
        Matcher matcher2 = pattern2.matcher(mainQuery);
        if (matcher2.find()) {
            select = matcher2.group(1);
        }


    }


    public String getSelect() {
        return select;
    }

    public void setSelect(String select) {
        this.select = select;
    }


    public String toString() {
        return mainQuery;
    }

    public String toSubgraphForm() {

        String ret = where;
        if (selectFocus.size() > 1) {
            int i = 0;
            for (String sf : selectFocus) {
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + " ", "\\?answer" + i + " ");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "\\.", "\\?answer" + i + ".");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "}", "\\?answer" + i + "}");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "\\)", "\\?answer" + i + ")");
                i++;
            }
        } else {
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + " ", "\\?answer ");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "\\.", "\\?answer.");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "}", "\\?answer}");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "\\)", "\\?answer)");
        }
        return ret.replaceAll("\n", " ").replaceAll("\"", "\\\"");
    }

    public ArrayList<String> getSelectFocus() {
        return selectFocus;
    }



    public int getFocusLength() {
        return selectFocus.size();
    }
}
