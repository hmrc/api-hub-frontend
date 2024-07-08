import {buildPaginator, HIDDEN_BY_PAGINATION} from './paginationController.js';
import {buildNameFilter} from "./hipApisNameFilter.js";
import {setVisible, noop} from "./utils.js";

export function onPageShow() {
    const filters = [
        buildNameFilter()
    ];

    const view = (() => {
        const myApiPanelEls = Array.from(document.querySelectorAll('#myApisPanels .hip-api')),
            elSearchResultsSize = document.getElementById('searchResultsSize'),
            elPaginationContainer = document.getElementById('pagination'),
            elDisplayCountMessage = document.getElementById('displayCountMessage'),
            elDisplayCount = document.getElementById('displayCount'),
            elTotalCount = document.getElementById('totalCount');

        let onFiltersChangedHandler = noop;

        filters.forEach(filter=> filter.onChange(() => onFiltersChangedHandler()));

        return {
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            get myApisPanels() {
                return [...myApiPanelEls];
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
            get displayCountMessage() {
                return elDisplayCountMessage;
            },
            get displayCount() {
                return elDisplayCount;
            },
            get totalCount() {
                return elTotalCount;
            }
        };
    })();

    const paginator = buildPaginator(view.paginationContainer, 10)

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

        paginator.initialise(model.apis.filter(apiDetail => ! apiDetail.hiddenByFilters));
        view.setApiPanelVisibility(model.apis);

        view.setResultCount(model.resultCount);
    }

    paginator.onPaginationChanged(paginationDetails => {
        view.setApiPanelVisibility(model.apis);
        setVisible(view.displayCountMessage, paginationDetails.isPaginating);
        view.displayCount.textContent = paginationDetails.visibleItemCount;
        view.totalCount.textContent = paginationDetails.totalItemCount;
    });

    view.onFiltersChanged(() => {
        applyMyApisFilters();
    });

    view.initialiseFilters(model.apis);

    applyMyApisFilters();
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
