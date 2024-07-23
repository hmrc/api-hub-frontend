import {buildPaginator, HIDDEN_BY_PAGINATION} from './paginationController.js';
import {buildNameFilter} from "./exploreApisNameFilter.js";
import {setVisible, noop} from "./utils.js";

export function onPageShow() {
    const filters = [
        buildNameFilter()
    ];

    const view = (() => {
        const myApiPanelEls = Array.from(document.querySelectorAll('#myApisPanels .hip-api')),
            elSearchResultsSize = document.getElementById('searchResultsSize'),
            elNoSearchResults = document.getElementById('noSearchResults');

        let onFiltersChangedHandler = noop;

        filters.forEach(filter=> filter.onChange(() => onFiltersChangedHandler()));

        return {
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            get myApisPanels() {
                return [...myApiPanelEls];
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    setVisible(apiDetail.el, apiDetail.visible);
                });
            },
            setNoSearchResultsVisibility(apis) {
                setVisible(elNoSearchResults, !apis.some(anyApi => anyApi.visible));
            },
            initialiseFilters(apis) {
                filters.forEach(filter => filter.initialise(apis));
            },
            setResultCount(count) {
                elSearchResultsSize.textContent = count;
            }
        };
    })();

    const paginator = buildPaginator(10)

    function buildFilterFunctions() {
        return filters.map(filter=> filter.buildFilterFunction());
    }

    const model = {
        apis: view.myApisPanels.map(el => ({
            data: {
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

    function applyMyApisFilters() {
        const myApisFilterFns = buildFilterFunctions();
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenByFilters = ! myApisFilterFns.every(fn => fn(apiDetail.data));
        });

        view.setApiPanelVisibility(model.apis);
        paginator.render(model.apis.filter(apiDetail => ! apiDetail.hiddenByFilters).map(panel => panel.el));

        view.setResultCount(model.resultCount);
        view.setNoSearchResultsVisibility(model.apis);
    }

    view.onFiltersChanged(() => {
        applyMyApisFilters();
    });

    view.initialiseFilters(model.apis);

    applyMyApisFilters();
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
