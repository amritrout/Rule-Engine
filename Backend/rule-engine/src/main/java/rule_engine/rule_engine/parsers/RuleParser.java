package rule_engine.rule_engine.parsers;

import org.springframework.stereotype.Component;
import rule_engine.rule_engine.models.Node;
import java.util.HashMap;
import java.util.Map;

@Component
public class RuleParser {

    // TODO: ADD support for NOT operator
    // For now, keeping it simple with AND/OR operations
    public static Node parseExpression(String expr) {
        expr = expr.replaceAll("\\s+", "");
        // Makes life easier to just use && and || internally
        // Yeah, I know this isn't the most elegant solution but it works ¯\_(ツ)_/¯
        expr = expr.replace("AND", "&&").replace("OR", "||");
        return parseOrExpression(expr); // Start with OR since it has lower precedence
    }

    // Handles OR expressions first - lowest precedence
    // example-"a && b || c && d" -> splits on the ||
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
    // Supports three types of comparisons: >, <, and =
    // FIXME: Should probably add support for >=, <=, and != at some point
    private static Node parseCondition(String expr) {
        expr = expr.trim();

        // Handle nested expressions first
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return parseExpression(expr.substring(1, expr.length() - 1));
        }

        // Not the prettiest code, but gets the job done
        // Would be nice to refactor this into something more elegant someday
        if (expr.contains(">")) {
            String[] parts = expr.split(">");
            return new Node("operand", ">", parts[0].trim() + ">" + parts[1].trim());
        } else if (expr.contains("<")) {
            String[] parts = expr.split("<");
            return new Node("operand", "<", parts[0].trim() + "<" + parts[1].trim());
        } else if (expr.contains("=")) {
            String[] parts = expr.split("=");
            return new Node("operand", "=", parts[0].trim() + "=" + parts[1].trim().replace("'", ""));
        }
        return null;
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
            if (node.operator.equals("AND")) {
                return evaluate(node.left, context) && evaluate(node.right, context);
            } else if (node.operator.equals("OR")) {
                return evaluate(node.left, context) || evaluate(node.right, context);
            }
        }

        // Handle the actual comparisons
        // Bit of a mess with the type casting, but it works for our use case
        if (node.type.equals("operand")) {
            String[] parts;
            String variable, value;

            if (node.operator.equals(">")) {
                parts = node.value.split(">");
                variable = parts[0].trim();
                value = parts[1].trim();

                if (context.get(variable) == null) {
                    throw new IllegalArgumentException("Variable '" + variable + "' not found in context");
                }

                return (int) context.get(variable) > Integer.parseInt(value);
            } else if (node.operator.equals("<")) {
                parts = node.value.split("<");
                variable = parts[0].trim();
                value = parts[1].trim();

                if (context.get(variable) == null) {
                    throw new IllegalArgumentException("Variable '" + variable + "' not found in context");
                }

                return (int) context.get(variable) < Integer.parseInt(value);
            } else if (node.operator.equals("=")) {
                parts = node.value.split("=");
                variable = parts[0].trim();
                value = parts[1].trim();

                if (context.get(variable) == null) {
                    throw new IllegalArgumentException("Variable '" + variable + "' not found in context");
                }

                return context.get(variable).toString().equals(value);
            }
        }

        return false;
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
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }*/
}