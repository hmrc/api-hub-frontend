import {buildPaginator} from './paginationController.js';
import {buildDomainFilters} from "./exploreApisDomainFilters.js";
import {buildStatusFilters} from "./exploreApisStatusFilters.js";
import {buildHodsFilters} from "./exploreApisHodsFilters.js";
import {buildPlatformFilters} from "./exploreApisPlatformFilters.js";
import {buildModel} from "./exploreApisModel.js";
import {setVisible, noop} from "./utils.js";
import {buildSearch} from "./exploreApisSearch.js";

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
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elResetFiltersLink = document.getElementById('resetFilters'),
            elNoResultsResetFiltersLink = document.getElementById('noResultsClearFilters');

        let onFiltersChangedHandler = noop;

        filters.forEach(filter=> filter.onChange(() => onFiltersChangedHandler()));

        function clearAllFilters() {
            filters.forEach(filter => filter.clear());
            onFiltersChangedHandler();
        }

        elResetFiltersLink.addEventListener('click', clearAllFilters);
        elNoResultsResetFiltersLink.addEventListener('click', clearAllFilters);

        return {
            displayResults() {
                // Only display results after all filters have been applied
                setVisible(elApiResultsContainer, true);
            },
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            get apiDetailPanels() {
                return [...apiDetailPanelEls];
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    setVisible(apiDetail.el, apiDetail.visible);
                });
            },
            initialiseFilters(apis) {
                filters.forEach(filter => filter.initialise(apis));
            },
            setResultCount(count) {
                elSearchResultsSize.textContent = count;
            },
            toggleNoResultsPanel(visible) {
                setVisible(elNoResultsPanel, visible);
            }
        };
    })();

    const paginator = buildPaginator(15)

    paginator.onPagination(itemsVisibility => {
        itemsVisibility.forEach(([el, visibleOnCurrentPage]) => {
            const api = model.getApiForElement(el);
            api.hiddenByPagination = !visibleOnCurrentPage;
        });
    });

    function buildFilterFunctions() {
        return filters.map(filter=> filter.buildFilterFunction());
    }

    const model = buildModel(view.apiDetailPanels);

    function applyFilters() {
        const filterFns = buildFilterFunctions();
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenByFilters = ! filterFns.every(fn => fn(apiDetail.data));
        });

        paginator.render(model.apis.filter(apiDetail => apiDetail.includeInResults).map(panel => panel.el));
        view.setApiPanelVisibility(model.apis);

        view.setResultCount(model.resultCount);
        view.toggleNoResultsPanel(model.resultCount === 0);
    }

    view.onFiltersChanged(() => {
        applyFilters();
    });

    view.initialiseFilters(model.apis);

    applyFilters();
    view.displayResults();

    const search = buildSearch();
    search.initialise(model);
    search.onSearch(searchTerm => {
        const encodedSearchTerm = encodeURIComponent(searchTerm);
        fetch(`apis/deep-search/${encodedSearchTerm}`)
            .then(response => response.json())
            .then(matchingIds => {
                const matchingIdsSet = new Set(matchingIds);
                model.apis.forEach(apiDetail => {
                    apiDetail.hiddenBySearch = !matchingIdsSet.has(apiDetail.data.id);
                });

                filters.forEach(filter => filter.clear());
                applyFilters();
            })
            .catch(e => console.error(e));
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
