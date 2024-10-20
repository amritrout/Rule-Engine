package rule_engine.rule_engine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rule_engine.rule_engine.models.Node;
import rule_engine.rule_engine.models.Rule;
import rule_engine.rule_engine.parsers.RuleCombiner;
import rule_engine.rule_engine.parsers.RuleParser;
import rule_engine.rule_engine.repositories.RuleRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RulesService {

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private RuleParser ruleParser;

    public Rule createRule(String ruleString, String description) {
        Rule rule = new Rule();
        rule.setRuleString(ruleString);
        rule.setDescription(description);

        // Convert rule string to AST and store it
        Node ast = RuleParser.parseExpression(ruleString);

        //for Debugging
        //System.out.println("AST Structure:");
        //RuleParser.printAST(ast, 0);

        rule.setAst(ast);

        return ruleRepository.save(rule);
    }

    public Rule combineRules(List<String> rules){
        System.out.println(rules);
        Rule rule=new Rule();
        String combinedRule = RuleCombiner.combineRules(rules);
        rule.setRuleString(combinedRule);


        Node ast = RuleParser.parseExpression(combinedRule);
        rule.setAst(ast);
        return ruleRepository.save(rule);

    }

    public Optional<Rule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    public List<Rule> getAllRules() {
        return ruleRepository.findAll();
    }

    public boolean evaluateRule(Rule rule, Map<String, Object> data) {
        Node ast = rule.getAst();
        return ruleParser.evaluate(ast, data);
    }
}
