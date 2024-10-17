package rule_engine.rule_engine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rule_engine.rule_engine.models.Rule;

public interface RuleRepository extends JpaRepository<Rule, Long> {
}