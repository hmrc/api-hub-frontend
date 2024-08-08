import {buildPaginator} from './paginationController.js';
import {buildDomainFilters} from "./exploreApisDomainFilters.js";
import {buildStatusFilters} from "./exploreApisStatusFilters.js";
import {buildHodsFilters} from "./exploreApisHodsFilters.js";
import {buildPlatformFilters} from "./exploreApisPlatformFilters.js";
import {buildModel} from "./exploreApisModel.js";
import {setVisible, noop, normaliseText} from "./utils.js";
import {buildSearch} from "./exploreApisSearch.js";
import {buildSearchResultPanel, buildFilterResultPanel} from "./exploreApisResultPanels.js";

export function onPageShow() {
    const filters = [
            buildDomainFilters(),
            buildStatusFilters(),
            buildHodsFilters(),
            buildPlatformFilters()
        ];

    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiList .api-panel')),
            elSearchResultsSize = document.getElementById('searchResultsSize'),
            elApiResultsContainer = document.getElementById('apiResultsContainer'),
            elApiList = document.getElementById('apiList'),
            searchBox = buildSearch(),
            searchResultsPanel = buildSearchResultPanel(),
            filterResultsPanel = buildFilterResultPanel();

        searchBox.initialise();

        return {
            displayResults() {
                // Only display results after all filters have been applied
                setVisible(elApiResultsContainer, true);
            },
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
                elSearchResultsSize.textContent = count;
            },
            clearSearch() {
                searchBox.clear();
            },
            showSearchResultsPanel: searchResultsPanel.show,
            hideSearchResultsPanel: searchResultsPanel.hide,
            onClearSearch: searchResultsPanel.onClear,
            showFilterResultsPanel: filterResultsPanel.show,
            hideFilterResultsPanel: filterResultsPanel.hide,
            onClearFilters: filterResultsPanel.onClear,
            onSearch: searchBox.onSearch
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
            view.showSearchResultsPanel(showingAllResults, model.searchResultCount, model.currentSearchText);
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
        view.orderApiPanelsByIndex(model.apis);
        filters.forEach(filter => filter.syncWithApis(model.apis.filter(apiDetail => !apiDetail.hiddenBySearch)));
        applyFiltersAndPagination();
    });

    view.onClearFilters(() => {
        clearAllFilters();
    });

    applyFiltersAndPagination();
    view.displayResults();

    view.onSearch(searchTerm => {
        if (normaliseText(searchTerm) === model.currentSearchText) {
            return;
        }
        model.currentSearchText = normaliseText(searchTerm);

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
                model.apis.sort((a, b) => a.index - b.index);
                view.orderApiPanelsByIndex(model.apis);

                filters.forEach(filter => filter.clear());
                filters.forEach(filter => filter.syncWithApis(model.apis.filter(apiDetail => !apiDetail.hiddenBySearch)));
                applyFiltersAndPagination();
            })
            .catch(e => console.error(e));
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
