package fr.irit.sparql.query.select;

import java.util.List;
import java.util.Map;

public class SelectResponse {

    private Head head;
    private Results results;

    public static class Head {
        private List<String> vars;

        public List<String> getVars() {
            return vars;
        }
    }

    public static class Results {

        public static class Binding {
            private String type;
            private String value;

            public String getType() {
                return type;
            }

            public String getValue() {
                return value;
            }
        }
        private List<Map<String, Binding>> bindings;

        public List<Map<String, Binding>> getBindings() {
            return bindings;
        }
    }

    public Head getHead() {
        return head;
    }

    public Results getResults() {
        return results;
    }
}
