export function buildStatusFilters() {
    const statusFilterEls = Array.from(document.querySelectorAll('#statusFilters .govuk-checkboxes__input'));

    let onFiltersChangedHandler = () => {};

    function getSelected() {
        return statusFilterEls.filter(el => el.checked).map(el => el.value);
    }

    return {
        initialise() {
            statusFilterEls.forEach(elCheckbox => {
                elCheckbox.addEventListener('change', () => onFiltersChangedHandler());
            });
        },
        onChange(handler) {
            onFiltersChangedHandler = handler;
        },
        clear() {
            statusFilterEls.forEach(el => {
                el.checked = false;
            });
        },
        buildFilterFunction() {
            const selectedStatuses = new Set(getSelected());
            return data => selectedStatuses.size === 0 || selectedStatuses.has(data.apiStatus);
        }
    };
}
