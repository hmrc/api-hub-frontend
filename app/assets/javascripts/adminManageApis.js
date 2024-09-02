import {buildPaginator} from './paginationController.js';
import {noop, normaliseText, setVisible} from "./utils.js";

export function onPageShow() {
    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiDetailPanels .hip-api')),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elApiCount = document.getElementById('apiCount'),
            elApiFilter = document.getElementById('apiFilter');

        let onFiltersChangedHandler = noop;

        elApiFilter.addEventListener('input', () => {
            onFiltersChangedHandler();
        });

        return {
            get apiDetailPanels() {
                return [...apiDetailPanelEls];
            },
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    setVisible(apiDetail.el, !apiDetail.hiddenByFilter);
                });
            },
            get apiFilterValue() {
                return elApiFilter.value;
            },
            toggleNoResultsPanel(visible) {
                setVisible(elNoResultsPanel, visible);
            },
            set resultCount(value) {
                elApiCount.textContent = value;
            }
        };
    })();

    const apiPanels = view.apiDetailPanels.map(el => ({
        el,
        apiName: el.dataset['apiname'],
        apiRef: el.dataset['apiref'],
        hiddenByFilter: false
    }));

    const paginator = buildPaginator(10);

    function applyFilter() {
        const normalisedFilterValue = normaliseText(view.apiFilterValue);
        apiPanels.forEach(apiDetail => {
            const filterMatchesName = normaliseText(apiDetail.apiName).includes(normalisedFilterValue),
                filterMatchesRef = normaliseText(apiDetail.apiRef).includes(normalisedFilterValue),
                filterMatchesAnything = filterMatchesName || filterMatchesRef;
            apiDetail.hiddenByFilter = ! filterMatchesAnything;
        });

        const filteredPanels = apiPanels.filter(apiDetail => ! apiDetail.hiddenByFilter);
        view.setApiPanelVisibility(apiPanels);
        paginator.render(filteredPanels.map(panel => panel.el));

        const resultCount = filteredPanels.length;
        view.toggleNoResultsPanel(resultCount === 0);
        view.resultCount = resultCount;
    }

    view.onFiltersChanged(() => {
        applyFilter();
    });

    paginator.render(apiPanels.map(o => o.el));
    applyFilter();
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}
