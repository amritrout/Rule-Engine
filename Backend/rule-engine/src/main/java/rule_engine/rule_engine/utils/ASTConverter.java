package rule_engine.rule_engine.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import rule_engine.rule_engine.models.Node;

@Converter(autoApply = true)
public class ASTConverter implements AttributeConverter<Node, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Node attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize AST", e);
        }
    }

    @Override
    public Node convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, Node.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not deserialize AST", e);
        }
    }
}
