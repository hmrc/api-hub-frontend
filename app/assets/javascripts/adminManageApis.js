import {buildPaginator} from './paginationController.js';
import {noop, normaliseText, setVisible} from "./utils.js";

export function onPageShow() {
    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiDetailPanels .hip-api')),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elApiCount = document.getElementById('apiCount'),
            elNameFilter = document.getElementById('nameFilter');

        let onFiltersChangedHandler = noop;

        elNameFilter.addEventListener('input', () => {
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
            get nameFilterValue() {
                return elNameFilter.value;
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
        hiddenByFilter: false
    }));

    const paginator = buildPaginator(10);

    function applyFilter() {
        const normalisedNameFilterValue = normaliseText(view.nameFilterValue);
        apiPanels.forEach(apiDetail => {
            apiDetail.hiddenByFilter = ! normaliseText(apiDetail.apiName).includes(normalisedNameFilterValue);
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
