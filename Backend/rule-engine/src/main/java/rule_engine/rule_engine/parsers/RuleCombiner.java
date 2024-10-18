package rule_engine.rule_engine.parsers;
import lombok.Getter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Condition {
    @Getter
    String variable;
    String operator;
    String value;

    public Condition(String condition) {
        if (condition.contains(">")) {
            String[] parts = condition.split(">");
            this.variable = parts[0].trim();
            this.operator = ">";
            this.value = parts[1].trim();
        } else if (condition.contains("<")) {
            String[] parts = condition.split("<");
            this.variable = parts[0].trim();
            this.operator = "<";
            this.value = parts[1].trim();
        } else if (condition.contains("=")) {
            String[] parts = condition.split("=");
            this.variable = parts[0].trim();
            this.operator = "=";
            this.value = parts[1].trim().replace("'", "");
        }
    }

    public boolean isNumeric() {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isMutuallyExclusive(Condition other) {
        if (!this.variable.equals(other.variable)) return false;

        if (this.isNumeric() && other.isNumeric()) {
            double val1 = Double.parseDouble(this.value);
            double val2 = Double.parseDouble(other.value);

            if (this.operator.equals(">") && other.operator.equals("<")) {
                return val1 >= val2;
            } else if (this.operator.equals("<") && other.operator.equals(">")) {
                return val2 >= val1;
            }
        }

        if (this.operator.equals("=") && other.operator.equals("=")) {
            return !this.value.equals(other.value);
        }

        return false;
    }

    @Override
    public String toString() {
        String displayValue = isNumeric() ? value : "'" + value + "'";
        return variable + " " + operator + " " + displayValue;
    }
}

class Rule {
    List<Condition> conditions;
    String operator;

    public Rule(String ruleStr) {
        conditions = new ArrayList<>();

        ruleStr = ruleStr.trim();
        if (ruleStr.startsWith("(") && ruleStr.endsWith(")")) {
            ruleStr = ruleStr.substring(1, ruleStr.length() - 1);
        }

        if (ruleStr.contains("AND")) {
            operator = "AND";
            String[] parts = ruleStr.split("AND");
            for (String part : parts) {
                conditions.add(new Condition(part.trim()));
            }
        } else if (ruleStr.contains("OR")) {
            operator = "OR";
            String[] parts = ruleStr.split("OR");
            for (String part : parts) {
                conditions.add(new Condition(part.trim()));
            }
        } else {
            operator = "SINGLE";
            conditions.add(new Condition(ruleStr));
        }
    }

    public Set<String> getVariables() {
        Set<String> vars = new HashSet<>();
        for (Condition cond : conditions) {
            vars.add(cond.getVariable());
        }
        return vars;
    }

    public boolean hasSimilarStructure(Rule other) {
        return this.operator.equals(other.operator) &&
                this.conditions.size() == other.conditions.size();
    }

    @Override
    public String toString() {
        if (conditions.size() == 1) {
            return conditions.get(0).toString();
        }

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < conditions.size(); i++) {
            sb.append(conditions.get(i));
            if (i < conditions.size() - 1) {
                sb.append(" ").append(operator).append(" ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}

public class RuleCombiner {

    public static String combineRules(List<String> ruleStrings) {
        ruleStrings = new ArrayList<>(splitConditions(ruleStrings));
        List<Rule> rules = new ArrayList<>();
        for (String ruleString : ruleStrings) {
            rules.add(new Rule(ruleString));
        }

        Map<Set<String>, List<Rule>> variableGroups = new HashMap<>();
        for (Rule rule : rules) {
            Set<String> vars = rule.getVariables();

            if (!variableGroups.containsKey(vars)) {
                variableGroups.put(vars, createNewList());
            }
            variableGroups.get(vars).add(rule);
        }

        List<String> combinedGroups = new ArrayList<>();

        for (List<Rule> group : variableGroups.values()) {
            if (group.size() > 1 && shouldCombineWithOr(group)) {
                combinedGroups.add(combineGroupWithOr(group));
            } else {
                combinedGroups.add(group.get(0).toString());
            }
        }

        return combineWithOperator(combinedGroups, "AND");
    }

    private static List<Rule> createNewList() {
        return new ArrayList<>();
    }

    private static boolean shouldCombineWithOr(List<Rule> group) {
        Rule first = group.get(0);
        for (int i = 1; i < group.size(); i++) {
            Rule current = group.get(i);
            if (!first.hasSimilarStructure(current)) {
                return false;
            }

            boolean foundMutuallyExclusive = false;
            for (Condition c1 : first.conditions) {
                for (Condition c2 : current.conditions) {
                    if (c1.isMutuallyExclusive(c2)) {
                        foundMutuallyExclusive = true;
                        break;
                    }
                }
                if (foundMutuallyExclusive) break;
            }

            if (!foundMutuallyExclusive) return false;
        }
        return true;
    }

    private static String combineGroupWithOr(List<Rule> group) {
        return combineWithOperator(combineRulesToString(group), "OR");
    }

    private static List<String> combineRulesToString(List<Rule> group) {
        List<String> expressions = new ArrayList<>();
        for (Rule rule : group) {
            expressions.add(rule.toString());
        }
        return expressions;
    }

    private static String combineWithOperator(List<String> expressions, String operator) {
        if (expressions.size() == 1) return expressions.get(0);

        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < expressions.size(); i++) {
            result.append(expressions.get(i));
            if (i < expressions.size() - 1) {
                result.append(" ").append(operator).append(" ");
            }
        }
        result.append(")");
        return result.toString();
    }

    public static List<String> splitConditions(List<String> inputList) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\(([^()]+)\\)");

        for (String inputString : inputList) {
            Matcher matcher = pattern.matcher(inputString);

            while (matcher.find()) {
                String condition = matcher.group(1);
                condition = condition.replaceAll("^\\(|\\)$", "").trim();
                result.add("(" + condition + ")");
            }
        }

        return result;
    }

    /*
    public static void main(String[] args) {
        List<String> inputList = new ArrayList<>();
        inputList.add("((age > 30 AND department = 'Sales') OR (age < 25 AND department = 'Marketing'))");
        inputList.add("(salary > 50000 OR experience > 5)");
        inputList.add("(age > 30 AND department = 'Marketing')");
        inputList.add("(name = 'John' AND email = 'john@example.com')");
        inputList.add("((name = 'Jane' AND email = 'jane@example.com'))");

        String combinedRule = combineRules(inputList);
        System.out.println("Combined rule: " + combinedRule);
    }*/
}
