var apiUrl = 'http://localhost:8080/api/rules';

// Default examples for the create rule field
document.getElementById('ruleString').value = "((age > 30 AND department = 'Sales') OR (age < 25 AND department = 'Marketing')) AND (salary > 50000 OR experience > 5)";
document.getElementById('description').value = "Can leave this field empty";

// Default examples for the evaluate field
document.getElementById('data').value = JSON.stringify({
    "age": 35,
    "department": "Sales",
    "salary": 100000,
    "experience": 5
});

// Default example for combining rules
document.getElementById('combineRulesInput').value = "((age > 30 AND department = 'Sales') OR (age != 25 AND department = 'Marketing')),(salary > 50000 OR experience > 5),(age > 30 AND department = 'Marketing')";

// Create Rule
document.getElementById('create-rule-form').addEventListener('submit', function(event) {
    event.preventDefault();
    var ruleString = document.getElementById('ruleString').value;
    var description = document.getElementById('description').value;

    axios.post(apiUrl + '/create', null, {
        params: { ruleString: ruleString, description: description }
    })
        .then(function(response) {
            alert('Rule created: ' + JSON.stringify(response.data));
            document.getElementById('create-rule-form').reset();
        })
        .catch(function(error) {
            console.error('Error creating rule:', error);
            const errorMessage = error.response ? error.response.data.message || error.response.data : 'Failed to create rule';
            alert('Error: ' + errorMessage);
        });
});

// Fetch All Rules
document.getElementById('fetch-rules').addEventListener('click', function() {
    axios.get(apiUrl + '/all')
        .then(function(response) {
            var rulesList = document.getElementById('rules-list');
            rulesList.innerHTML = ''; // Clear previous results
            response.data.forEach(function(rule) {
                var li = document.createElement('li');
                li.textContent = 'ID: ' + rule.id + ', Rule: ' + rule.ruleString + ', Description: ' + rule.description;
                rulesList.appendChild(li);
            });
        })
        .catch(function(error) {
            console.error('Error fetching rules:', error);
            const errorMessage = error.response ? error.response.data.message || error.response.data : 'Failed to fetch rules';
            alert('Error: ' + errorMessage);
        });
});

// Evaluate Rule
document.getElementById('evaluate-rule').addEventListener('click', function() {
    var id = document.getElementById('evaluateId').value;
    var data = JSON.parse(document.getElementById('data').value);

    axios.post(apiUrl + '/evaluate/' + id, data)
        .then(function(response) {
            document.getElementById('evaluation-result').textContent = 'Evaluation Result: ' + response.data;
        })
        .catch(function(error) {
            console.error('Error evaluating rule:', error);
            const errorMessage = error.response ? error.response.data.message || error.response.data : 'Failed to evaluate rule';
            alert('Error: ' + errorMessage);
        });
});

// Combine Rules
document.getElementById('combine-rules-button').addEventListener('click', function() {
    var combineInput = document.getElementById('combineRulesInput').value;

    axios.post(apiUrl + '/combine', null, {
        params: { ruleString: combineInput }
    })
        .then(function(response) {
            alert('Combined successfully!');
            document.getElementById('combineRulesInput').value = '';
        })
        .catch(function(error) {
            console.error('Error combining rules:', error);
            const errorMessage = error.response ? error.response.data.message || error.response.data : 'Failed to combine rules';
            alert('Error: ' + errorMessage);
        });
});

// Delete Rule
document.getElementById('delete-rule-button').addEventListener('click', function() {
    var id = document.getElementById('deleteId').value;

    axios.delete(apiUrl + '/' + id)
        .then(function(response) {
            alert('Rule deleted successfully: ' + JSON.stringify(response.data));
            document.getElementById('deleteId').value = '';
        })
        .catch(function(error) {
            console.error('Error deleting rule:', error);
            const errorMessage = error.response ? error.response.data.message || error.response.data : 'Failed to delete rule';
            alert('Error: ' + errorMessage);
        });
});
