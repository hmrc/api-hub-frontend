import {buildPaginator, HIDDEN_BY_PAGINATION} from './paginationController.js';
import {buildNameFilter} from "./hipApisNameFilter.js";
import {setVisible, noop} from "./utils.js";

export function onPageShow() {
    const filters = [
            buildDomainFilters(),
            buildStatusFilters(),
            buildHodsFilters(),
            buildNameFilter()
        ];

    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiList .api-panel')),
            elSearchResultsSize = document.getElementById('searchResultsSize'),
            elPaginationContainer = document.getElementById('pagination'),
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
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            get apiDetailPanels() {
                return [...apiDetailPanelEls];
            },
            get paginationContainer() {
                return elPaginationContainer;
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

    const paginator = buildPaginator(view.paginationContainer, 15)

    function buildFilterFunctions() {
        return filters.map(filter=> filter.buildFilterFunction());
    }

    const model = {
        apis: view.apiDetailPanels.map(el => ({
            data: {
                apiStatus: el.dataset['apistatus'],
                domain: el.dataset['domain'],
                subdomain: el.dataset['subdomain'],
                hods: new Set(el.dataset['hods'].split(',').filter(h => h)),
                apiName: el.dataset['apiname'],
            },
            el,
            hiddenByFilters: false,
            get visible() {
                return !this.hiddenByFilters && !this[HIDDEN_BY_PAGINATION];
            }
        })),
        get resultCount() {
            return this.apis.filter(apiDetail => ! apiDetail.hiddenByFilters).length;
        }
    };

    function applyFilters() {
        const filterFns = buildFilterFunctions();
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenByFilters = ! filterFns.every(fn => fn(apiDetail.data));
        });

        paginator.initialise(model.apis.filter(apiDetail => ! apiDetail.hiddenByFilters));
        view.setApiPanelVisibility(model.apis);

        view.setResultCount(model.resultCount);
        view.toggleNoResultsPanel(model.resultCount === 0);
    }

    paginator.onPaginationChanged(() => {
        view.setApiPanelVisibility(model.apis);
    });

    view.onFiltersChanged(() => {
        applyFilters();
    });

    view.initialiseFilters(model.apis);

    applyFilters();
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
