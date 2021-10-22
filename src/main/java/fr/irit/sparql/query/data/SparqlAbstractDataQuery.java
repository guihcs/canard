package fr.irit.sparql.query.data;

import fr.irit.sparql.query.exceptions.SparqlQueryMalFormedException;
import fr.irit.sparql.query.exceptions.SparqlQueryUnseparableException;
import fr.irit.sparql.query.SparqlQuery;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SparqlAbstractDataQuery extends SparqlQuery {
    protected String keyword;
    private StringBuilder data;

    public SparqlAbstractDataQuery(Set<Map.Entry<String, String>> prefix, String data) {
        super(prefix, "", "");
        this.data = new StringBuilder(data);
    }

    public void setData(StringBuilder data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return formatPrefixes() + keyword + "{" + data + "}";
    }

    public StringBuilder toStringBuilder() {
        StringBuilder ret = new StringBuilder(formatPrefixes() + keyword + "{");
        ret.append(data);
        ret.append("}");
        return ret;
    }

    public SparqlAbstractDataQuery serparateQuery(int maxSize) throws SparqlQueryUnseparableException, SparqlQueryMalFormedException {
        StringBuilder temp = new StringBuilder(data.subSequence(0, maxSize));

        Pattern pattern = Pattern.compile(".*([ABC])");
        Matcher m = pattern.matcher(temp);
        int idLastTriple;
        if (m.find()) {
            idLastTriple = m.start(1);
        } else {
            throw new SparqlQueryMalFormedException("no triple founded");
        }
        SparqlAbstractDataQuery ret;
        try {
            ret = (SparqlAbstractDataQuery) clone();
            ret.setData(new StringBuilder(data.substring(idLastTriple)));
        } catch (CloneNotSupportedException ex) {
            throw new SparqlQueryUnseparableException();
        }
        setData(new StringBuilder(data.subSequence(0, idLastTriple)));

        return ret;
    }

}
