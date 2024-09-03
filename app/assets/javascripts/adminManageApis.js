import {buildPaginator} from './paginationController.js';
import {setVisible} from "./utils.js";
import {buildTextFilter, dataAttribute} from "./textFilter.js";

export function onPageShow() {
    const view = (() => {
        const apiDetailPanelEls = document.querySelectorAll('#apiDetailPanels .hip-api'),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elApiCount = document.getElementById('apiCount'),
            elApiFilter = document.getElementById('apiFilter');

        return {
            get apiDetailPanels() {
                return [...apiDetailPanelEls];
            },
            get apiFilter() {
                return elApiFilter;
            },
            toggleNoResultsPanel(visible) {
                setVisible(elNoResultsPanel, visible);
            },
            set resultCount(value) {
                elApiCount.textContent = value;
            }
        };
    })();

    const paginator = buildPaginator(10),
        filter = buildTextFilter(
            view.apiDetailPanels,
            view.apiFilter,
            [
                dataAttribute('apiname').whenNormalised().includesTheFilterText(),
                dataAttribute('apiref').whenNormalised().includesTheFilterText(),
            ]
        );

    filter.onChange(matchingEls => {
        view.resultCount = matchingEls.length;
        view.toggleNoResultsPanel(matchingEls.length === 0);
        paginator.render(matchingEls);
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}
