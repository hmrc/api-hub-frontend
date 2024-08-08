import {noop} from "./utils.js";

export function buildStatusFilters() {
    const statusFilterEls = Array.from(document.querySelectorAll('#statusFilters .govuk-checkboxes__input')),
        elStatusFilterCount = document.getElementById('statusFilterCount'),
        elViewStatusFilters = document.getElementById('viewStatusFilters');

    let onFiltersChangedHandler = noop;

    function getSelected() {
        return statusFilterEls.filter(el => el.checked).map(el => el.value);
    }

    function updateStatusFilterCount() {
        const selectionCount = document.querySelectorAll('#statusFilters input:checked').length;
        elStatusFilterCount.textContent = '' + selectionCount;
    }

    function collapseStatusFilterSection(isCollapsed) {
        if (isCollapsed) {
            elViewStatusFilters.removeAttribute('open');
        } else {
            elViewStatusFilters.setAttribute('open', 'open');
        }
    }

    return {
        initialise() {
            statusFilterEls.forEach(elCheckbox => {
                elCheckbox.addEventListener('change', () => onFiltersChangedHandler());
            });

            const anyStatusesSelected = statusFilterEls.some(el => el.checked);
            collapseStatusFilterSection(!anyStatusesSelected);
            updateStatusFilterCount();
        },
        syncWithApis(apis) {
            //TODO
        },
        onChange(handler) {
            onFiltersChangedHandler = () => {
                updateStatusFilterCount();
                handler();
            };
        },
        clear() {
            statusFilterEls.forEach(el => {
                el.checked = false;
            });
            collapseStatusFilterSection(true);
            updateStatusFilterCount();
        },
        buildFilterFunction() {
            const selectedStatuses = new Set(getSelected());
            return data => selectedStatuses.size === 0 || selectedStatuses.has(data.apiStatus);
        }
    };
}
