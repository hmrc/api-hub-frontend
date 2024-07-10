import {buildPaginator, HIDDEN_BY_PAGINATION} from './paginationController.js';
import {noop, setVisible} from "./utils.js";

export function onDomLoaded() {
    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiDetailPanels .hip-api')),
            elPaginationContainer = document.getElementById('pagination'),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elDisplayCountMessage = document.getElementById('displayCountMessage'),
            elDisplayCountShowing = document.getElementById('displayCount'),
            elDisplayCountTotal = document.getElementById('totalCount'),
            elNameFilter = document.getElementById('nameFilter');

        let onFiltersChangedHandler = noop;

        elNameFilter.addEventListener('input', () => {
            onFiltersChangedHandler();
        });

        return {
            get apiDetailPanels() {
                return [...apiDetailPanelEls];
            },
            get paginationContainer() {
                return elPaginationContainer;
            },
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    setVisible(apiDetail.el, apiDetail.visible);
                });
            },
            setResultCount(count) {
                elDisplayCountTotal.textContent = count;
            },
            get nameFilterValue() {
                return elNameFilter.value;
            },
            toggleNoResultsPanel(visible) {
                setVisible(elNoResultsPanel, visible);
            },
            get displayCountMessage() {
                return elDisplayCountMessage;
            },
            get displayCount() {
                return elDisplayCountShowing;
            },
            get totalCount() {
                return elDisplayCountTotal;
            }
        };
    })();

    const apiPanels = view.apiDetailPanels.map(el => ({
        el,
        apiName: el.dataset['apiname'],
        hiddenByFilter: false,
        get visible() {
            return !this.hiddenByFilter && !this[HIDDEN_BY_PAGINATION];
        }
    }));

    const paginator = buildPaginator(view.paginationContainer, 3);

    function normalise(value) {
        return value.trim().toLowerCase();
    }

    function applyFilter() {
        const normalisedNameFilterValue = normalise(view.nameFilterValue);
        apiPanels.forEach(apiDetail => {
            apiDetail.hiddenByFilter = ! normalise(apiDetail.apiName).includes(normalisedNameFilterValue);
        });

        paginator.initialise(apiPanels.filter(apiDetail => ! apiDetail.hiddenByFilter));
        view.setApiPanelVisibility(apiPanels);

        const resultCount = apiPanels.filter(apiDetail => ! apiDetail.hiddenByFilter).length;
        view.setResultCount(resultCount);
        view.toggleNoResultsPanel(resultCount === 0);
    }

    view.onFiltersChanged(() => {
        applyFilter();
    });

    paginator.onPaginationChanged(paginationDetails => {
        view.setApiPanelVisibility(apiPanels);
        setVisible(view.displayCountMessage, paginationDetails.isPaginating);
        view.displayCount.textContent = paginationDetails.visibleItemCount;
        view.totalCount.textContent = paginationDetails.totalItemCount;
    });

    paginator.initialise(apiPanels);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
