import {buildPaginator} from './pagination.js';
import {buildDomainFilters} from "./hipApisDomainFilters.js";
import {buildStatusFilters} from "./hipApisStatusFilters.js";

export function onPageShow() {
    const domainFilters = buildDomainFilters(),
        statusFilters = buildStatusFilters();

    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiList .api-panel')),
            elSearchResultsSize = document.getElementById('searchResultsSize'),
            elPaginationContainer = document.getElementById('pagination'),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elResetFiltersLink = document.getElementById('resetFilters'),
            elNoResultsResetFiltersLink = document.getElementById('noResultsClearFilters'),
            paginator = buildPaginator(elPaginationContainer);

        let onFiltersChangedHandler = () => {};

        statusFilters.onChange(() => {
            onFiltersChangedHandler();
        });
        domainFilters.onChange(() => {
            onFiltersChangedHandler();
        });

        function clearAllFilters() {
            statusFilters.clear();
            domainFilters.clear();
            onFiltersChangedHandler();
        }

        elResetFiltersLink.addEventListener('click', clearAllFilters);
        elNoResultsResetFiltersLink.addEventListener('click', clearAllFilters);

        return {
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            onPaginationChanged(handler) {
                paginator.onNavigation(pageNumber => {
                    handler(pageNumber);
                });
            },
            getApiDetailPanels() {
                return apiDetailPanelEls;
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    apiDetail.el.style.display = apiDetail.visible ? 'block' : 'none';
                });
            },
            initialiseFilters(apis) {
                statusFilters.initialise();
                domainFilters.initialiseFromApis(apis);
            },
            setResultCount(count) {
                elSearchResultsSize.textContent = count;
            },
            setPagination(currentPage, totalPages) {
                paginator.render(currentPage, totalPages);
            },
            toggleNoResultsPanel(visible) {
                elNoResultsPanel.style.display = visible ? 'block' : 'none';
            }
        };
    })();

    function buildFilterFunctions() {
        return [
            statusFilters.buildFilterFunction(),
            domainFilters.buildFilterFunction()
        ];
    }

    const model = {
        apis: view.getApiDetailPanels().map(el => ({
            data: {
                apiStatus: el.dataset['apistatus'],
                domain: el.dataset['domain'],
                subdomain: el.dataset['subdomain']
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
        view.toggleNoResultsPanel(model.resultCount === 0);
    }

    view.onFiltersChanged(() => {
        applyFilters();
    });

    view.initialiseFilters(model.apis);

    applyFilters();
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
