package rule_engine.rule_engine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import rule_engine.rule_engine.models.Rule;
import rule_engine.rule_engine.services.RulesService;

import java.util.*;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    @Autowired
    private RulesService rulesService;

    @PostMapping("/create")
    public Rule createRule(@RequestParam String ruleString, @RequestParam String description) {
        try {
            System.out.println("Received ruleString: " + ruleString);
            System.out.println("Received description: " + description);
            return rulesService.createRule(ruleString, description);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/combine")
    public Rule combineRule(@RequestParam String ruleString) {
        try {
            String[] ruleArr = ruleString.split(",");
            List<String> ruleList = Arrays.asList(ruleArr);

            return rulesService.combineRules(ruleList);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to combine rules: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{id}")
    public Optional<Rule> getRule(@PathVariable Long id) {
        return rulesService.getRuleById(id);
    }

    @GetMapping("/all")
    public List<Rule> getAllRules() {
        return rulesService.getAllRules();
    }

    @PostMapping("/evaluate/{id}")
    public boolean evaluateRule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            Optional<Rule> rule = rulesService.getRuleById(id);
            if (rule.isPresent()) {
                return rulesService.evaluateRule(rule.get(), data);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rule with ID " + id + " not found");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error evaluating rule: " + e.getMessage(), e);
        }
    }

    
    @DeleteMapping("/{id}")
    public void deleteRule(@PathVariable Long id) {
        try {
            if (!rulesService.deleteRuleById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule with ID " + id + " not found");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting rule: " + e.getMessage(), e);
        }
    }
}
