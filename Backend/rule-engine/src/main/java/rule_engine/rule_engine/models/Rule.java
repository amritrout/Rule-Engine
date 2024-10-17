package rule_engine.rule_engine.models;
import jakarta.persistence.*;
import lombok.Data;
import rule_engine.rule_engine.utils.ASTConverter;

@Data
@Entity
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ruleString;
    private String description;

    @Lob
    @Convert(converter = ASTConverter.class)
    private Node ast;
}
