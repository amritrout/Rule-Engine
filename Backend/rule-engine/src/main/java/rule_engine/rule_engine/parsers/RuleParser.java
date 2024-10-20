package rule_engine.rule_engine.parsers;

import org.springframework.stereotype.Component;
import rule_engine.rule_engine.models.Node;

import java.util.*;

@Component
public class RuleParser {
    // Catalog
    private static final Map<String, List<String>> VALID_OPERATORS = new HashMap<String, List<String>>() {{
        put("age", Arrays.asList(">", "<", "=", ">=", "<=", "!="));
        put("salary", Arrays.asList(">", "<", "=", ">=", "<=", "!="));
        put("experience", Arrays.asList(">", "<", "=", ">=", "<=", "!="));
        put("department", Arrays.asList("=", "!="));
        put("name", Arrays.asList("=", "!="));
        put("email", Arrays.asList("=", "!="));
    }};

    private static final Set<String> VALID_ATTRIBUTES = new HashSet<>(VALID_OPERATORS.keySet());

    // TODO: ADD support for NOT operator
    // For now, keeping it simple with AND/OR operations
    public static Node parseExpression(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            throw new InvalidRuleException("Expression cannot be empty");
        }

        expr = expr.replaceAll("\\s+", "");
        // Makes life easier to just use && and || internally
        // Yeah, I know this isn't the most elegant solution but it works ¯\_(ツ)_/¯
        expr = expr.replace("AND", "&&").replace("OR", "||");

        try {
            return parseOrExpression(expr); // Start with OR since it has lower precedence
        } catch (Exception e) {
            if (e instanceof InvalidRuleException || e instanceof InvalidConditionException) {
                throw e;
            }
            throw new InvalidRuleException("Failed to parse expression: " + expr);
        }
    }

    // Handles OR expressions first - lowest precedence
    // e.g., "a && b || c && d" -> splits on the ||
    private static Node parseOrExpression(String expr) {
        int idx = findOperator(expr, "||");
        if (idx != -1) {
            Node node = new Node("operator", "OR", null);
            node.left = parseAndExpression(expr.substring(0, idx));
            node.right = parseOrExpression(expr.substring(idx + 2));
            return node;
        }
        return parseAndExpression(expr);
    }

    // After OR, we handle AND expressions
    private static Node parseAndExpression(String expr) {
        int idx = findOperator(expr, "&&");
        if (idx != -1) {
            Node node = new Node("operator", "AND", null);
            node.left = parseCondition(expr.substring(0, idx));
            node.right = parseAndExpression(expr.substring(idx + 2));
            return node;
        }
        return parseCondition(expr);
    }

    // Handles the basic conditions - this is where the rubber meets the road
    // Supports six types of comparisons: >, <, =, >=, <=, and !=
    private static Node parseCondition(String expr) {
        expr = expr.trim();

        // Handle nested expressions first
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return parseExpression(expr.substring(1, expr.length() - 1));
        }

        String variable = extractVariable(expr);
        if (!VALID_ATTRIBUTES.contains(variable)) {
            throw new InvalidConditionException("Invalid attribute: " + variable);
        }

        String operator = extractOperator(expr);
        if (!VALID_OPERATORS.get(variable).contains(operator)) {
            throw new InvalidConditionException("Invalid operator '" + operator + "' for variable '" + variable + "'");
        }

        // Not the prettiest code, but gets the job done
        // Would be nice to refactor this into something more elegant someday
        String[] parts = expr.split(operator, 2);
        validateParts(parts, expr);

        String value = parts[1].trim().replaceAll("^'|'$", "");  // Remove surrounding quotes if present
        return new Node("operand", operator, variable + operator + value);
    }

    private static String extractVariable(String expr) {
        for (String operator : Arrays.asList("!=", ">=", "<=", "=", ">", "<")) {
            if (expr.contains(operator)) {
                return expr.split(operator)[0].trim();
            }
        }
        throw new InvalidConditionException("No valid operator found in expression: " + expr);
    }

    private static String extractOperator(String expr) {
        if (expr.contains("!=")) return "!=";
        if (expr.contains(">=")) return ">=";
        if (expr.contains("<=")) return "<=";
        if (expr.contains("=")) return "=";
        if (expr.contains(">")) return ">";
        if (expr.contains("<")) return "<";
        throw new InvalidConditionException("No valid operator found in expression: " + expr);
    }

    private static void validateParts(String[] parts, String expr) {
        if (parts.length != 2) {
            throw new InvalidConditionException("Invalid condition format: " + expr);
        }
        if (parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            throw new InvalidConditionException("Both variable and value must be provided: " + expr);
        }
    }

    private static int findOperator(String expr, String operator) {
        int depth = 0;
        for (int i = 0; i < expr.length() - 1; i++) {
            if (expr.charAt(i) == '(') depth++;
            if (expr.charAt(i) == ')') depth--;
            if (depth == 0 && expr.startsWith(operator, i)) {
                return i;
            }
        }
        return -1;
    }

    // The main evaluation logic
    // Takes our AST and a context map of variables and their values
    public static boolean evaluate(Node node, Map<String, Object> context) {
        if (node == null) return false;

        // Handle the AND/OR operations
        if (node.type.equals("operator")) {
            switch (node.operator) {
                case "AND":
                    return evaluate(node.left, context) && evaluate(node.right, context);
                case "OR":
                    return evaluate(node.left, context) || evaluate(node.right, context);
                default:
                    throw new InvalidRuleException("Unknown operator: " + node.operator);
            }
        }

        // Handle the actual comparisons
        // Bit of a mess with the type casting, but it works for our use case
        if (node.type.equals("operand")) {
            String[] parts = node.value.split(node.operator, 2);
            String variable = parts[0].trim();
            String value = parts[1].trim();
            validateContextVariable(variable, context);

            switch (node.operator) {
                case ">":
                case "<":
                case ">=":
                case "<=":
                    validateNumericOperation(variable, value);
                    double contextValue = ((Number) context.get(variable)).doubleValue();
                    double compareValue = Double.parseDouble(value);
                    switch (node.operator) {
                        case ">": return contextValue > compareValue;
                        case "<": return contextValue < compareValue;
                        case ">=": return contextValue >= compareValue;
                        case "<=": return contextValue <= compareValue;
                    }
                case "=":
                    if (isNumericVariable(variable)) {
                        validateNumericOperation(variable, value);
                        return ((Number) context.get(variable)).doubleValue() == Double.parseDouble(value);
                    }
                    return context.get(variable).toString().equals(value);
                case "!=":
                    if (isNumericVariable(variable)) {
                        validateNumericOperation(variable, value);
                        return ((Number) context.get(variable)).doubleValue() != Double.parseDouble(value);
                    }
                    return !context.get(variable).toString().equals(value);
                default:
                    throw new InvalidConditionException("Unsupported operator: " + node.operator);
            }
        }

        return false;
    }

    private static void validateContextVariable(String variable, Map<String, Object> context) {
        if (!context.containsKey(variable)) {
            throw new InvalidConditionException("Variable '" + variable + "' not found in context");
        }
    }

    private static void validateNumericOperation(String variable, String value) {
        if (!isNumericVariable(variable)) {
            throw new InvalidConditionException("Numeric operation not allowed for variable: " + variable);
        }
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new InvalidConditionException("Invalid numeric value: " + value);
        }
    }

    private static boolean isNumericVariable(String variable) {
        return Arrays.asList("age", "salary", "experience").contains(variable);
    }

    // Helpful debug method - dumps the AST structure to console
    public static void printAST(Node node, int level) {
        if (node == null) return;

        String indent = "  ".repeat(level);

        if (node.type.equals("operator")) {
            System.out.println(indent + "Operator: " + node.operator);
        } else if (node.type.equals("operand")) {
            System.out.println(indent + "Operand: " + node.value);
        }

        printAST(node.left, level + 1);
        printAST(node.right, level + 1);
    }

    /* Keeping this commented out for now - good for testing though!
    public static void main(String[] args) {
        String rule = "((age > 30 AND department = 'Sales') OR (age < 25 AND department = 'Marketing')) "
                + "AND (salary > 50000 OR experience > 5)";

        Map<String, Object> context = new HashMap<>();
        context.put("age", 35);
        context.put("department", "Sales");
        context.put("salary", 60000);
        context.put("experience", 6);

        Node ast = parseExpression(rule);

        System.out.println("AST Structure:");
        printAST(ast, 0);

        try {
            boolean result = evaluate(ast, context);
            System.out.println("\nEvaluation Result: " + result);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }*/
}