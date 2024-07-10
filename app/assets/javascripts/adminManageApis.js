import {buildPaginator} from './paginationController.js';
import {noop, setVisible} from "./utils.js";

export function onDomLoaded() {
    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll('#apiDetailPanels .hip-api')),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
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
        };
    })();

    const apiPanels = view.apiDetailPanels.map(el => ({
        el,
        apiName: el.dataset['apiname'],
        hiddenByFilter: false
    }));

    const paginator = buildPaginator(10);

    function normalise(value) {
        return value.trim().toLowerCase();
    }

    function applyFilter() {
        const normalisedNameFilterValue = normalise(view.nameFilterValue);
        apiPanels.forEach(apiDetail => {
            apiDetail.hiddenByFilter = ! normalise(apiDetail.apiName).includes(normalisedNameFilterValue);
        });

        const filteredPanels = apiPanels.filter(apiDetail => ! apiDetail.hiddenByFilter);
        view.setApiPanelVisibility(apiPanels);
        paginator.render(filteredPanels.map(panel => panel.el));

        const resultCount = filteredPanels.length;
        view.toggleNoResultsPanel(resultCount === 0);
    }

    view.onFiltersChanged(() => {
        applyFilter();
    });

    paginator.render(apiPanels.map(o => o.el));
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
