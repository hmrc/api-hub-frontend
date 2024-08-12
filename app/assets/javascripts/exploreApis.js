import {buildPaginator} from './paginationController.js';
import {buildDomainFilters} from "./exploreApisDomainFilters.js";
import {buildStatusFilters} from "./exploreApisStatusFilters.js";
import {buildHodsFilters} from "./exploreApisHodsFilters.js";
import {buildPlatformFilters} from "./exploreApisPlatformFilters.js";
import {buildModel} from "./exploreApisModel.js";
import {setVisible, addToSortedMethodToArray, normaliseText, isVisible} from "./utils.js";
import {buildSearch} from "./exploreApisSearch.js";
import {buildSearchResultPanel, buildFilterResultPanel} from "./exploreApisResultPanels.js";

export function onPageShow() {
    const filters = [
            buildDomainFilters(),
            buildStatusFilters(),
            buildHodsFilters(),
            buildPlatformFilters()
        ];

    addToSortedMethodToArray();

    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiList .api-panel')),
            elApiResultsSize = document.getElementById('apiResultsSize'),
            elApiResults = document.getElementById('apiResults'),
            elApiList = document.getElementById('apiList'),
            searchBox = buildSearch(),
            searchResultsPanel = buildSearchResultPanel(),
            filterResultsPanel = buildFilterResultPanel();

        searchBox.initialise();

        return {
            get apiDetailPanels() {
                return [...apiDetailPanelEls];
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    setVisible(apiDetail.el, apiDetail.visible);
                });
            },
            orderApiPanelsByIndex(apis) {
                const panelsToReorder = apis.filter(apiDetail => Number.isFinite(apiDetail.index))
                    .toSorted((a, b) => a.index - b.index)
                    .map(apiDetail => apiDetail.el);
                elApiList.prepend(...panelsToReorder); // move all panels in single DOM operation to minimise reflows
            },
            setResultCount(count) {
                elApiResultsSize.textContent = `(${count})`;
            },
            clearSearch() {
                searchBox.clear();
            },
            showSearchResultsPanelSuccess: searchResultsPanel.showSuccess,
            showSearchResultsPanelError: searchResultsPanel.showError,
            hideSearchResultsPanel: searchResultsPanel.hide,
            onClearSearch: searchResultsPanel.onClear,
            showFilterResultsPanel: filterResultsPanel.show,
            hideFilterResultsPanel: filterResultsPanel.hide,
            onClearFilters: filterResultsPanel.onClear,
            onSearch: searchBox.onSearch,
            get searchTerm() {
                return searchBox.searchTerm;
            },
            set displayResults(visible) {
                setVisible(elApiResults, visible);
                setVisible(elApiResultsSize, visible);
            },
            set enableFilters(enabled) {
                document.querySelectorAll('input[type=checkbox]').forEach(checkbox => checkbox.disabled = !enabled);
            }
        };
    })();

    function updateHiddenByPaginationValues(itemsVisibility) {
        itemsVisibility.forEach(([el, visibleOnCurrentPage]) => {
            const api = model.getApiForElement(el);
            api.hiddenByPagination = !visibleOnCurrentPage;
        });
        view.setApiPanelVisibility(model.apis);
    }

    const paginator = buildPaginator(15, updateHiddenByPaginationValues)
    const model = buildModel(view.apiDetailPanels);

    function buildFilterFunctions() {
        return filters.map(filter=> filter.buildFilterFunction());
    }

    function applyFiltersAndPagination() {
        const filterFns = buildFilterFunctions();
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenByFilters = ! filterFns.every(fn => fn(apiDetail.data));
        });

        const apiPanelsToShowAfterFiltering = model.apis.filter(apiDetail => apiDetail.includeInResults).map(panel => panel.el);
        paginator.render(apiPanelsToShowAfterFiltering);

        view.setResultCount(model.resultCount);

        const filteredCount = model.filteredCount;
        if (model.currentSearchText) {
            const showingAllResults = filteredCount === 0;
            view.showSearchResultsPanelSuccess(showingAllResults, model.searchResultCount, model.currentSearchText);
        } else {
            view.hideSearchResultsPanel();
        }

        if (model.filteredCount > 0) {
            view.showFilterResultsPanel(model.resultCount, model.filteredCount);
        } else {
            view.hideFilterResultsPanel();
        }
    }

    filters.forEach(filter => filter.initialise(model.apis));
    filters.forEach(filter=> filter.onChange(applyFiltersAndPagination));

    function clearAllFilters() {
        filters.forEach(filter => filter.clear());
        applyFiltersAndPagination();
    }

    view.onClearSearch(() => {
        view.clearSearch();
        model.currentSearchText = null;
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenBySearch = false;
            apiDetail.index = apiDetail.originalIndex;
        });
        model.sortApisByIndex();
        view.orderApiPanelsByIndex(model.apis);
        filters.forEach(filter => filter.syncWithApis(model.apis.filter(apiDetail => !apiDetail.hiddenBySearch)));
        applyFiltersAndPagination();
    });

    view.onClearFilters(() => {
        clearAllFilters();
    });

    applyFiltersAndPagination();
    // Only display results after all filters have been applied
    view.displayResults = true;

    function performSearch(searchTerm, clearFilters = true) {
        model.currentSearchText = normaliseText(searchTerm);
        view.enableFilters = false;
        view.displayResults = false;

        const encodedSearchTerm = encodeURIComponent(searchTerm);
        fetch(`apis/deep-search/${encodedSearchTerm}`)
            .then(response => response.json())
            .then(matchingIds => {
                const matchingIdLookup = new Map(matchingIds.map((id, index) => [id, index]));
                model.apis.forEach(apiDetail => {
                    if (matchingIdLookup.has(apiDetail.data.id)) {
                        apiDetail.index = matchingIdLookup.get(apiDetail.data.id);
                        apiDetail.hiddenBySearch = false;
                    } else {
                        apiDetail.index = Number.POSITIVE_INFINITY;
                        apiDetail.hiddenBySearch = true;
                    }
                });
                model.sortApisByIndex();
                view.orderApiPanelsByIndex(model.apis);

            })
            .catch(e => {
                model.apis.forEach(apiDetail => apiDetail.hiddenBySearch = true);
                model.currentSearchText = null;
                view.showSearchResultsPanelError();
                console.error(e)
            })
            .finally(() => {
                if (clearFilters) {
                    filters.forEach(filter => filter.clear());
                }
                view.enableFilters = true;
                view.displayResults = true;

                filters.forEach(filter => filter.syncWithApis(model.apis.filter(apiDetail => !apiDetail.hiddenBySearch)));
                applyFiltersAndPagination();
            });
    }

    view.onSearch(searchTerm => {
        if (normaliseText(searchTerm) === model.currentSearchText) {
            return;
        }
        performSearch(searchTerm);
    });

    if (view.searchTerm) {
        performSearch(view.searchTerm, false);
    }
}

if (typeof window !== 'undefined') {
    /* When navigating back to a page containing form elements browsers will automatically re-populate the inputs with the values
    that the user set previously. We need to be able to see these values when setting up the page because if the filters or search box
    are populated we need to make sure the APIs are shown/hidden/sorted appropriately. For this reason we use the 'pageshow' event
    here rather than 'DOMContentLoaded'. */
    window.addEventListener('pageshow', onPageShow);
}
