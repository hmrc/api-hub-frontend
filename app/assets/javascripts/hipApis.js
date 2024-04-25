window.addEventListener('pageshow', () => {
    "use strict";

    const view = (() => {
        const statusFilterEls = Array.from(document.querySelectorAll('#statusFilters .govuk-checkboxes__input')),
            apiDetailPanelEls = Array.from(document.querySelectorAll('#apiList .api-panel')),
            elSearchResultsSize = document.getElementById('searchResultsSize');

        return {
            onFiltersChanged(handler) {
                statusFilterEls.forEach(elCheckbox => {
                    elCheckbox.addEventListener('change', handler);
                });
            },
            getStatusFilterValues() {
                return statusFilterEls.filter(el => el.checked).map(el => el.value);
            },
            getApiDetailPanels() {
                return apiDetailPanelEls;
            },
            updateApiDetailsPanels(model) {
                let visibleCount = 0;
                model.forEach(apiDetail => {
                    apiDetail.el.style.display = apiDetail.visible ? 'block' : 'none';
                    if (apiDetail.visible) {
                        visibleCount++;
                    }
                });
                elSearchResultsSize.textContent = visibleCount.toString();
            },
        };
    })();

    function buildFilterFunctions() {
        function buildApiStatusFilterFunction() {
            const selectedStatuses = new Set(view.getStatusFilterValues());
            return data => selectedStatuses.has(data.apiStatus);
        }
        // add new filters here...

        return [buildApiStatusFilterFunction()];
    }

    const model = view.getApiDetailPanels().map(el => ({
        data: {
            apiStatus: el.dataset['apistatus']
            // add other properties that we want to filter on here...
        },
        el,
        visible: true
    }));

    function applyFilters() {
        const filterFns = buildFilterFunctions();
        model.forEach(apiDetail => {
            apiDetail.visible = filterFns.every(fn => fn(apiDetail.data));
        });
        view.updateApiDetailsPanels(model);
    }

    view.onFiltersChanged(() => {
        applyFilters();
    });

    applyFilters();

});
