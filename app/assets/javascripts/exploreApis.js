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
            elApiList = document.getElementById('apiList'),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elNoResultsResetFiltersLink = document.getElementById('noResultsClearFilters');

        let onFiltersChangedHandler = noop;

        filters.forEach(filter=> filter.onChange(() => onFiltersChangedHandler()));

        function clearAllFilters() {
            filters.forEach(filter => filter.clear());
            onFiltersChangedHandler();
        }

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
            orderApiPanelsByIndex(apis) {
                const panelsToReorder = apis.filter(apiDetail => Number.isFinite(apiDetail.index))
                    .toSorted((a, b) => a.index - b.index)
                    .map(apiDetail => apiDetail.el);
                elApiList.prepend(...panelsToReorder); // move all panels in single DOM operation to minimise reflows
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
        view.setApiPanelVisibility(model.apis);
        //TODO remove
        console.log(model.apis.map(a => `${a.index} [${a.hiddenByFilters ? 'F' : '.'}${a.hiddenBySearch ? 'S' : '.'}${a.hiddenByPagination ? 'P' : '.'}] ${a.el.querySelector('a').innerText}`))
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
                applyFilters();
            })
            .catch(e => console.error(e));
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
