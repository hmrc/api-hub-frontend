const view = (() => {
    return {
        onStatusFilterChange: function(handler) {
            document.querySelectorAll('#statusFilters .govuk-checkboxes__input').forEach(elCheckbox => {
                elCheckbox.addEventListener('change', handler);
            });
        },
        getStatusFilters: function() {
            return Array.from(document.querySelectorAll('#statusFilters .govuk-checkboxes__input')).reduce((acc, elCheckbox) => {
                acc[elCheckbox.value] = elCheckbox.checked;
                return acc;
            }, {});
        },

    };
})();

document.addEventListener('DOMContentLoaded', function(event) {
    view.onStatusFilterChange(function(status, checked) {
        console.log(view.getStatusFilters());
    });
})
