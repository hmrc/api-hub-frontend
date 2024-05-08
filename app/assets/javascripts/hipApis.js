window.addEventListener('pageshow', () => {
    "use strict";

    const view = (() => {
        const statusFilterEls = Array.from(document.querySelectorAll('#statusFilters .govuk-checkboxes__input')),
            apiDetailPanelEls = Array.from(document.querySelectorAll('#apiList .api-panel')),
            elSearchResultsSize = document.getElementById('searchResultsSize'),
            elPaginationContainer = document.getElementById('pagination'),
            paginator = buildPaginator(elPaginationContainer);

        return {
            onFiltersChanged(handler) {
                statusFilterEls.forEach(elCheckbox => {
                    elCheckbox.addEventListener('change', handler);
                });
            },
            onPaginationChanged(handler) {
                paginator.onNavigation(pageNumber => {
                    handler(pageNumber);
                });
            },
            getStatusFilterValues() {
                return statusFilterEls.filter(el => el.checked).map(el => el.value);
            },
            getApiDetailPanels() {
                return apiDetailPanelEls;
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    apiDetail.el.style.display = apiDetail.visible ? 'block' : 'none';
                });
            },
            setResultCount(count) {
                elSearchResultsSize.textContent = count;
            },
            setPagination(currentPage, totalPages) {
                paginator.render(currentPage, totalPages);
            }
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

    const model = {
        apis: view.getApiDetailPanels().map(el => ({
            data: {
                apiStatus: el.dataset['apistatus']
                // add other properties that we want to filter on here...
            },
            el,
            hiddenByFilters: false,
            hiddenByPagination: false,
            get visible() {
                return !this.hiddenByFilters && !this.hiddenByPagination;
            }
        })),
        get resultCount() {
            return this.apis.filter(apiDetail => ! apiDetail.hiddenByFilters).length;
        },
        pagination: {
            currentPage: 1,
            get totalPages() {
                return Math.ceil(model.resultCount / this.itemsPerPage);
            },
            itemsPerPage: 3
        }
    };


    function setPaginationPage(pageNumber) {
        console.log(`Navigating to page ${pageNumber}`);
        model.pagination.currentPage = pageNumber;
        view.setPagination(model.pagination.currentPage, model.pagination.totalPages); //TODO repeated code
        applyPaginationFiltering();
        view.setApiPanelVisibility(model.apis);
    }

    view.onPaginationChanged(setPaginationPage);

    function applyPaginationFiltering() {
        const startIndex = (model.pagination.currentPage - 1) * model.pagination.itemsPerPage,
            endIndex = startIndex + model.pagination.itemsPerPage;
        model.apis
            .filter(apiDetail => ! apiDetail.hiddenByFilters)
            .forEach((apiDetail, index) => {
                apiDetail.hiddenByPagination = index < startIndex || index >= endIndex;
            });
    }

    function applyFilters() {
        const filterFns = buildFilterFunctions();
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenByFilters = ! filterFns.every(fn => fn(apiDetail.data));
        });

        setPaginationPage(1);

        view.setResultCount(model.resultCount);
    }

    view.onFiltersChanged(() => {
        applyFilters();
    });

    applyFilters();
});
