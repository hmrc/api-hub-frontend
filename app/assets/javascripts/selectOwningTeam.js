import './accessible-autocomplete.min.js';

document.addEventListener('DOMContentLoaded', function() {
    const elOwningTeamSelect = document.getElementById('owningTeamSelection');
    if (elOwningTeamSelect) {
        accessibleAutocomplete.enhanceSelectElement({
            defaultValue: '',
            selectElement: elOwningTeamSelect,
            showAllValues: true
        });
    }
});