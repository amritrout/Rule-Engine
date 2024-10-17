package rule_engine.rule_engine.models;
import lombok.Data;

@Data
public class Node {
    public String type;        // "operator" or "operand"
    public String operator;    // AND, OR, >, <, =.
    public String value;       // value for conditions (age, department, etc.)
    public Node left;
    public Node right;

    public Node() {
    }

    public Node(String type, String operator, String value) {
        this.type = type;
        this.operator = operator;
        this.value = value;
    }
}
