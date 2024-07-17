import './accessible-autocomplete.min.js';

document.addEventListener('DOMContentLoaded', function() {
    accessibleAutocomplete.enhanceSelectElement({
        defaultValue: '',
        selectElement: document.getElementById('owningTeam'),
        showAllValues: true
    })
});