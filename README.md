# Rule Engine

A sophisticated rule processing system built with Spring Boot that leverages Abstract Syntax Tree (AST) to evaluate complex business rules dynamically. This application enables organizations to define, combine, and evaluate eligibility rules based on various user attributes such as age, department, income, and experience.


<summary>Example Use Cases</summary>

```
# Sample Rule Structure
"((age > 30 AND department = 'Sales') OR (age < 25 AND department = 'Marketing')) 
 AND (salary > 50000 OR experience > 5)"
```

This rule would evaluate if a person is eligible based on:
- Age and department combinations
- Salary or experience thresholds
</details>

### Technical Stack

- Backend: Spring Boot
- Database: MySQL
- Containerization: Docker
- Frontend: Html,CSS,JavaScript

## Design Choices

<details>
<summary>1. Node Class for AST Representation</summary>

The `Node` class is a fundamental component of our Abstract Syntax Tree (AST) structure. It's designed to represent both operators and operands within a rule:

```java
@Data
public class Node {
    public String type;        // "operator" or "operand"
    public String operator;    // AND, OR, >, <, =, etc.
    public String value;       // value for conditions (age, department, etc.)
    public Node left;
    public Node right;

    public Node(String type, String operator, String value) {
        this.type = type;
        this.operator = operator;
        this.value = value;
    }
}
```

Key aspects of the Node class:
- **Flexibility**: Can represent both operators (AND, OR) and operands (age > 30, department = 'Sales', etc.).
- **Tree Structure**: `left` and `right` fields allow for building a binary tree structure.
- **Lombok @Data**: Reduces boilerplate by automatically generating getters, setters, equals, hashCode, and toString methods.

### Example: AST Representation of a Complex Rule

Let's consider the following rule:

```
rule1 = "((age > 30 AND department = 'Sales') OR (age < 25 AND department = 'Marketing')) AND (salary > 50000 OR experience > 5)"
```

After parsing, this rule would be represented as an AST structure using Node objects. Here's a visual representation:

```
                AND
               /   \
              OR    OR
             /  \   /  \
           AND  AND  >   >
          /  \  / \   |   |
         >   =  <  = salary experience
        / \  |  | \  |  |     |
      age 30 dep age 25 dep 50000    5
               |      |
             Sales   Marketing
```

This AST structure allows for efficient traversal and evaluation of the rule. During rule evaluation, the system can quickly navigate this tree structure, evaluating each node based on its type and operator.

The use of this Node-based AST structure provides several benefits:
1. **Hierarchical Representation**: Complex rules with nested conditions are easily represented.
2. **Efficient Evaluation**: The tree structure allows for quick traversal and evaluation.
3. **Extensibility**: New types of operations or conditions can be added by extending the Node class or modifying the parser.

This approach strikes a balance between simplicity in representation and the ability to handle complex rule structures, making it a robust choice for our Rule Engine implementation.
</details>


<details>
<summary>2. RuleParser: Comprehensive Rule Parsing</summary>

The `RuleParser` class is a crucial component of our Rule Engine, responsible for converting string-based rule expressions into Abstract Syntax Tree (AST) structures. Here are some key features and design choices:

1. **Catalog-based Validation**: 
   - Utilizes a predefined catalog (`VALID_OPERATORS`) to validate operators for different attributes.
   - Ensures that only appropriate operators are used with specific attributes (e.g., '>' is valid for 'age' but not for 'department').

2. **Recursive Parsing**:
   - Implements a recursive descent parser to handle nested expressions.
   - Separate methods for parsing OR expressions (`parseOrExpression`), AND expressions (`parseAndExpression`), and individual conditions (`parseCondition`).

3. **Operator Precedence**:
   - Ensures correct operator precedence (OR has lower precedence than AND).
   - Handles parentheses to override default precedence.

4. **Robust Error Handling**:
   - Custom exceptions (`InvalidRuleException`, `InvalidConditionException`) for clear error reporting.
   - Provides detailed error messages for invalid attributes, operators, or rule structures.

5. **Flexible Condition Parsing**:
   - Supports various formats of conditions (e.g., "age > 30", "department = 'Sales'").
   - Handles both numeric and string comparisons appropriately.

6. **AST Construction**:
   - Builds a binary tree structure where each node represents either an operator (AND, OR) or an operand (individual condition).
   - Uses the `Node` class to represent both operators and operands uniformly.

7. **Extensibility**:
   - Designed to be easily extended with new operators or attributes by updating the `VALID_OPERATORS` catalog.

8. **Context-based Evaluation**:
   - The `evaluate` method allows for evaluating parsed rules against a given context (map of attribute-value pairs).
   - Supports both numeric and string-based comparisons during evaluation.

Example usage:
```java
String rule = "age > 30 AND (department = 'Sales' OR experience > 5)";
Node ast = RuleParser.parseExpression(rule);

Map<String, Object> context = new HashMap<>();
context.put("age", 35);
context.put("department", "Sales");
context.put("experience", 3);

boolean result = RuleParser.evaluate(ast, context);
```

This design allows for efficient parsing and evaluation of complex rule structures, providing both flexibility and performance in rule processing.
</details>

<details>
<summary>3. RuleCombiner: Efficient Rule Optimization</summary>

The `RuleCombiner` class is a sophisticated component designed to merge and optimize multiple rules. Its primary goal is to reduce redundancy and improve evaluation efficiency. Here are the key aspects of the RuleCombiner:

1. **Rule Grouping**:
   - Groups rules based on the variables they operate on.
   - This allows for more efficient combination of related rules.

2. **Intelligent OR Combination**:
   - Identifies rules with similar structures that can be combined using OR operations.
   - Uses the `shouldCombineWithOr` method to determine if rules are candidates for OR combination.

3. **Mutual Exclusivity Check**:
   - Implements logic to detect mutually exclusive conditions within rules.
   - This is crucial for determining when rules can be safely combined with OR operations.

4. **Hierarchical Combination**:
   - Combines rules at different levels: first within variable groups, then across groups.
   - Uses AND operations to combine different variable groups, preserving the overall rule logic.

5. **String-based Rule Representation**:
   - Works with string representations of rules, making it flexible and easy to integrate with various rule input methods.

6. **Condition Splitting**:
   - Implements a `splitConditions` method to break down complex rules into individual conditions.
   - This granular approach allows for more precise rule combination.

7. **Preservation of Rule Semantics**:
   - Ensures that the combined rule maintains the same logical outcomes as the individual rules.

8. **Optimization for Evaluation Efficiency**:
   - The resulting combined rule is structured to minimize redundant checks during evaluation.

Example usage:
```java
List<String> inputRules = Arrays.asList(
    "((age > 30 AND department = 'Sales') OR (age != 25 AND department = 'Marketing'))",
    "(salary > 50000 OR experience > 5)",
    "(age > 30 AND department = 'Marketing')"
);

String combinedRule = RuleCombiner.combineRules(inputRules);
```
</details>

</details>

<details>
<summary>4. Use of Lombok for Boilerplate Reduction</summary>

We've utilized Lombok annotations (e.g., `@Data`) to reduce boilerplate code in model classes:

- **Improved Readability**: Less cluttered code, focusing on business logic.
- **Reduced Error Prone Code**: Automatically generated methods (getters, setters, etc.) minimize manual coding errors.
</details>

<details>
<summary>5. Exception Handling for Invalid Rules</summary>

Custom exceptions (`InvalidRuleException`, `InvalidConditionException`) are used to provide clear error messages for invalid rule structures or unsupported operations.
</details>

<details>
<summary>6. Extensible Attribute and Operator System</summary>

The system is designed to be easily extensible:

- **Attribute Catalog**: A centralized catalog of valid attributes with their corresponding operators.
- **Easy Addition**: New attributes or operators can be added by updating the catalog, without major code changes.
</details>

<details>
<summary>7. Use of Regular Expressions for Parsing</summary>

Regular expressions are employed for parsing conditions:

- **Flexibility**: Allows for various formats of condition strings.
- **Robustness**: Helps in validating and extracting components of a condition reliably.
</details>

<details>
<summary>8. Immutable Rule Objects</summary>

Rule objects are designed to be immutable:

- **Thread Safety**: Ensures that rules can be safely used in multi-threaded environments.
- **Predictability**: Prevents unexpected changes to rule definitions during runtime.
</details>

<details>
<summary>9. Spring Boot Integration</summary>

The use of Spring Boot provides:

- **Dependency Injection**: Facilitates loose coupling and easier testing.
- **Easy Configuration**: Utilizes application.properties for database and other configurations.
- **Production-Ready Features**: Includes metrics, health checks, and externalized configuration.
</details>

These design choices collectively contribute to a robust, flexible, and maintainable Rule Engine system. They allow for easy extension of functionality while maintaining performance and code quality.

## Prerequisites

- Java 17 or higher
- Maven
- MySQL
- Docker (optional)

## Backend Setup & Configuration

<summary>Local Development</summary>

1. Navigate to `src/main/resources/application.properties` and update MySQL credentials:
   ```properties
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

2. Start the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
</details>

<summary>Building JAR</summary>

Choose one of the following methods:

```bash
# Build with tests
mvn clean install

# Build without tests (for Docker)
mvn clean install -DskipTests
```

## Docker Deployment

<summary>Quick Start</summary>

The repository includes a pre-built JAR file for immediate Docker deployment.

```bash
# First time or when changes are made
docker-compose up --build

# For subsequent runs
docker-compose up
```

<summary>Using Custom Build</summary>

1. Open `Dockerfile` and modify JAR file source:
   ```dockerfile
   # Uncomment this line
   #COPY target/rule-engine-0.0.1-SNAPSHOT.jar app.jar
   
   # Comment out this line
   COPY rule-engine-0.0.1-SNAPSHOT.jar app.jar
   ```

2. Build new JAR:
   ```bash
   mvn clean install -DskipTests
   ```

3. Deploy with Docker:
   ```bash
   docker-compose up --build
   ```

## Docker Configuration

The application runs in two containers:
- Spring Boot application (Port: 8080)
- MySQL database (Port: 3307)

<summary>Default Environment Variables</summary>

MySQL configurations (preset in both `docker-compose.yml` and `Dockerfile`):
```
Database: rule_engine_db
Username: user123
Password: root@password
```

## Frontend Setup and Usage

### Setup Instructions

1. Ensure the Spring Boot backend is running (either locally or via Docker).
2. Navigate to the frontend folder:
   ```bash
   cd path/to/frontend/folder
   ```
3. Open the `index.html` file in your web browser.

### Frontend Interface

![image](https://github.com/user-attachments/assets/940daad2-1660-45aa-924d-ebea19f21854)


The frontend interface consists of four main sections:

1. **Create Rule**
2. **All Rules**
3. **Evaluate Rule**
4. **Combine Rules**

### Usage Examples

#### 1. Create Rule

The create rule form takes two inputs:
- Rule String
- Description

Example rule string:
```
((age > 30 AND department = 'Sales') OR (age < 25 AND department = 'Marketing')) AND (salary > 50000 OR experience > 5)
```

#### 2. Evaluate Rule

To evaluate a rule, you need:
- Rule ID
- JSON format data

Example JSON data:
```json
{
  "age": 35,
  "department": "Sales",
  "salary": 60000,
  "experience": 3
}
```

#### 3. Combine Rules

To combine rules, enter multiple rule strings separated by commas:

Example:
```
((age > 30 AND department = 'Sales') OR (age != 25 AND department = 'Marketing')),(salary > 50000 OR experience > 5),(age > 30 AND department = 'Marketing')
```

## API Endpoints

### Rule Management

- POST `/api/rules/create`: Create a new rule
  - Parameters:
    - `ruleString` (query parameter): The rule string
    - `description` (query parameter): Description of the rule
  - Returns: Created Rule object

- POST `/api/rules/combine`: Combine multiple rules
  - Parameters:
    - `ruleString` (query parameter): Comma-separated list of rule strings to combine
  - Returns: Combined Rule object

- GET `/api/rules/{id}`: Get a specific rule by ID
  - Parameters:
    - `id` (path variable): The ID of the rule to retrieve
  - Returns: Rule object if found, or appropriate error response

- GET `/api/rules/all`: Get all rules
  - Returns: List of all Rule objects

- POST `/api/rules/evaluate/{id}`: Evaluate data against a specific rule
  - Parameters:
    - `id` (path variable): The ID of the rule to evaluate
    - Request body: JSON object containing the data to evaluate
  - Returns: Boolean result of the evaluation

### Data Format

- Rule Creation:
  ```
  POST /api/rules/create?ruleString=age > 30 AND department = 'Sales'&description=Sales department age rule
  ```

- Rule Combination:
  ```
  POST /api/rules/combine?ruleString=age > 30,department = 'Sales',salary > 50000
  ```

- Rule Evaluation:
  ```
  POST /api/rules/evaluate/1
  {
    "age": 35,
    "department": "Sales",
    "salary": 60000,
    "experience": 3
  }
  ```

## Troubleshooting

<summary>Common Issues</summary>

- **Database Connection Issues (Local)**: Verify MySQL is running and credentials in application.properties are correct
- **Docker Deployment**: Ensure ports 8080 and 3307 are available
- **Build Issues**: Try using `-DskipTests` flag during Maven build

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
