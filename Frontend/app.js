var apiUrl = 'http://localhost:8080/api/rules';

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
            alert('Failed to create rule');
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
            alert('Failed to evaluate rule');
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
            document.getElementById('combineRulesInput').value = ''; // Clear input field
        })
        .catch(function(error) {
            console.error('Error combining rules:', error);
            alert('Failed to combine rules');
        });
});
