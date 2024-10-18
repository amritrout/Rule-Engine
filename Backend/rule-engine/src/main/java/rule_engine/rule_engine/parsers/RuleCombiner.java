package rule_engine.rule_engine.parsers;

import lombok.Getter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InvalidConditionException extends RuntimeException {
    public InvalidConditionException(String message) {
        super(message);
    }
}

class InvalidRuleException extends RuntimeException {
    public InvalidRuleException(String message) {
        super(message);
    }
}

// This is where we break down each condition
class Condition {
    @Getter
    String variable;
    String operator;
    String value;

    // Our Catalog
    private static final Map<String, List<String>> VALID_OPERATORS = new HashMap<String, List<String>>() {{
        put("age", Arrays.asList(">", "<", "=", ">=", "<=", "!="));
        put("salary", Arrays.asList(">", "<", "=", ">=", "<="));
        put("experience", Arrays.asList(">", "<", "=", ">=", "<="));
        put("department", Arrays.asList("=", "!="));
        put("name", Arrays.asList("=", "!="));
        put("email", Arrays.asList("=", "!="));
    }};

    public Condition(String condition) {
        if (condition.isEmpty()) {
            throw new InvalidConditionException("Condition cannot be empty.");
        }

        // This regex is a pain, but it works
        Pattern pattern = Pattern.compile("(\\w+)\\s*(!=|>|<|=)\\s*(['\"]?)(.*?)\\3");
        Matcher matcher = pattern.matcher(condition);

        if (matcher.matches()) {
            this.variable = matcher.group(1).trim();
            this.operator = matcher.group(2);
            this.value = matcher.group(4).trim().replace("'", "").replace("\"", ""); // Get rid of quotes

            validateVariable(variable);
            validateOperator(variable, operator);
        } else {
            throw new InvalidConditionException("Invalid condition format: " + condition);
        }
    }

    // Make sure we're not using some made-up variable
    private void validateVariable(String variable) {
        if (!VALID_OPERATORS.containsKey(variable)) {
            throw new InvalidConditionException("Invalid variable '" + variable + "'. Did you mean something else?");
        }
    }

    // Check if we're using the right operator for this variable
    private void validateOperator(String variable, String operator) {
        List<String> validOperators = VALID_OPERATORS.get(variable);
        if (validOperators == null || !validOperators.contains(operator)) {
            throw new InvalidConditionException("Invalid operator '" + operator + "' for variable '" + variable + "'. Check the docs for valid operators.");
        }
    }

    // Figure out if we're dealing with a number
    public boolean isNumeric() {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Check if this condition contradicts another one
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

// This is where we put conditions together into a rule
class Rule {
    List<Condition> conditions;
    String operator;

    // If it's not in this set then we don't want it
    private static final Set<String> VALID_ATTRIBUTES = new HashSet<>(Arrays.asList("age", "department", "salary", "experience", "name", "email"));

    public Rule(String ruleStr) {
        conditions = new ArrayList<>();

        ruleStr = ruleStr.trim();
        if (ruleStr.startsWith("(") && ruleStr.endsWith(")")) {
            ruleStr = ruleStr.substring(1, ruleStr.length() - 1);
        }

        // Figure out what kind of rule we're dealing with
        if (ruleStr.contains("AND")) {
            operator = "AND";
            String[] parts = ruleStr.split("AND");
            for (String part : parts) {
                addCondition(part.trim());
            }
        } else if (ruleStr.contains("OR")) {
            operator = "OR";
            String[] parts = ruleStr.split("OR");
            for (String part : parts) {
                addCondition(part.trim());
            }
        } else {
            operator = "SINGLE";
            addCondition(ruleStr);
        }
    }

    private void addCondition(String conditionStr) {
        Condition condition = new Condition(conditionStr);
        validateAttribute(condition.getVariable());
        conditions.add(condition);
    }

    // Make sure we're only using attributes we know about
    private void validateAttribute(String variable) {
        if (!VALID_ATTRIBUTES.contains(variable)) {
            throw new InvalidRuleException("Invalid attribute in rule: " + variable + ". Stick to the ones we know.");
        }
    }

    public Set<String> getVariables() {
        Set<String> vars = new HashSet<>();
        for (Condition cond : conditions) {
            vars.add(cond.getVariable());
        }
        return vars;
    }

    // Check if this rule looks like another one
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
            sb.append(conditions.get(i).toString());
            if (i < conditions.size() - 1) {
                sb.append(" ").append(operator).append(" ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}

//combine all the rules
public class RuleCombiner {

    public static String combineRules(List<String> ruleStrings) {
        // First, let's break down any complex rules
        ruleStrings = new ArrayList<>(splitConditions(ruleStrings));
        List<Rule> rules = new ArrayList<>();
        for (String ruleString : ruleStrings) {
            rules.add(new Rule(ruleString));
        }

        // Group similar rules together
        Map<Set<String>, List<Rule>> variableGroups = new HashMap<>();
        for (Rule rule : rules) {
            Set<String> vars = rule.getVariables();

            if (!variableGroups.containsKey(vars)) {
                variableGroups.put(vars, createNewList());
            }
            variableGroups.get(vars).add(rule);
        }

        List<String> combinedGroups = new ArrayList<>();

        // Decide which groups to combine with OR
        for (List<Rule> group : variableGroups.values()) {
            if (group.size() > 1 && shouldCombineWithOr(group)) {
                combinedGroups.add(combineGroupWithOr(group));
            } else {
                combinedGroups.add(group.get(0).toString());
            }
        }

        // Final step: combine everything with AND
        return combineWithOperator(combinedGroups, "AND");
    }

    private static List<Rule> createNewList() {
        return new ArrayList<>();
    }

    // This method decides if we should use OR to combine rules
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

    // Break down complex conditions into simpler ones
    public static List<String> splitConditions(List<String> inputList) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\(([^()]+)\\)");

        for (String inputString : inputList) {
            Matcher matcher = pattern.matcher(inputString);

            while (matcher.find()) {
                String condition = matcher.group(1);
                // Get rid of extra parentheses
                condition = condition.replaceAll("^\\(|\\)$", "").trim();
                result.add("(" + condition + ")");
            }
        }

        return result;
    }

    // Put it all together with the right operator
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

    // Uncomment this to test the RuleCombiner
    /*
    public static void main(String[] args) {
        List<String> inputList = new ArrayList<>();
        inputList.add("((age > 30 AND department = 'Sales') OR (age != 25 AND department = 'Marketing'))");
        inputList.add("(salary > 50000 OR experience > 5)");
        inputList.add("(age > 30 AND department = 'Marketing')");
        inputList.add("(name = 'John' AND email = 'john@example.com')");
        inputList.add("((name = 'Jane' AND email = 'jane@example.com'))");

        System.out.println(inputList);

        try {
            String combinedRule = combineRules(inputList);
            System.out.println("Combined rule: " + combinedRule);
        } catch (InvalidRuleException | InvalidConditionException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    */
}