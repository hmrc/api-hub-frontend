import {buildPaginator} from './pagination.js';

export function onPageShow() {

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
            get itemsPerPage() {
                return 15;
            }
        }
    };

    function setPaginationPageNumber(pageNumber) {
        model.pagination.currentPage = pageNumber;

        const {currentPage, itemsPerPage, totalPages} = model.pagination,
            startIndex = (currentPage - 1) * itemsPerPage,
            endIndex = startIndex + itemsPerPage;

        model.apis
            .filter(apiDetail => ! apiDetail.hiddenByFilters)
            .forEach((apiDetail, index) => {
                apiDetail.hiddenByPagination = index < startIndex || index >= endIndex;
            });

        view.setApiPanelVisibility(model.apis);
        view.setPagination(currentPage, totalPages);
    }

    view.onPaginationChanged(setPaginationPageNumber);

    function applyFilters() {
        const filterFns = buildFilterFunctions();
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenByFilters = ! filterFns.every(fn => fn(apiDetail.data));
        });

        setPaginationPageNumber(1);

        view.setResultCount(model.resultCount);
    }

    view.onFiltersChanged(() => {
        applyFilters();
    });

    applyFilters();
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
