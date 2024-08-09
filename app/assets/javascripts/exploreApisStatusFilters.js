import {noop, setVisible} from "./utils.js";

export function buildStatusFilters() {
    const statusFilterEls = [],
        elStatusFiltersContainer = document.getElementById('statusFilters'),
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

    function getStatusesInUseByApis(apis) {
        return new Set(apis.map(apiDetail => apiDetail.data.apiStatus));
    }

    function setCheckboxVisibility(apiStatuses) {
        statusFilterEls.length = 0;
        document.querySelectorAll('input.statusFilter').forEach(elStatusCheckbox => {
            const status = elStatusCheckbox.value,
                statusInUse = apiStatuses.has(status);
            setVisible(elStatusCheckbox.parentElement, statusInUse);
            if (statusInUse) {
                statusFilterEls.push(elStatusCheckbox);
            }
        });
    }

    return {
        initialise(apis) {
            document.querySelectorAll('input.statusFilter')
                .forEach(elStatusCheckbox => {
                    elStatusCheckbox.addEventListener('change', () => {
                        onFiltersChangedHandler();
                    });
                });
            this.syncWithApis(apis);
        },
        syncWithApis(apis) {
            const apiStatuses = getStatusesInUseByApis(apis);
            setCheckboxVisibility(apiStatuses);

            // Although all APIs have a status, if we run a search that return no matches we need to hide the status filters
            const anyStatusesInUse = statusFilterEls.length > 0;
            setVisible(elStatusFiltersContainer, anyStatusesInUse);
            if (anyStatusesInUse) {
                const anyStatusesSelected = statusFilterEls.some(el => el.checked);
                collapseStatusFilterSection(!anyStatusesSelected);
                updateStatusFilterCount();
            }
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
