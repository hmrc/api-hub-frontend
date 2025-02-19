import {buildPaginator} from './paginationController.js';
import {setVisible} from "./utils.js";
import {buildTextFilter, dataAttribute} from "./textFilter.js";

export function onPageShow() {
    const elNoResultsPanel = document.getElementById('noResultsPanel'),
        elApiCount = document.getElementById('apiCount'),
        paginator = buildPaginator(10),
        filter = buildTextFilter(
            document.querySelectorAll('#apiDetailPanels .hip-api'),
            document.getElementById('apiFilter'),
            [
                dataAttribute('apiname').whenNormalised().includesTheFilterText(),
                dataAttribute('apinumber').whenNormalised().includesTheFilterText(),
                dataAttribute('apiref').whenNormalised().includesTheFilterText(),
            ]
        );

    filter.onChange(matchingEls => {
        elApiCount.textContent = matchingEls.length;
        setVisible(elNoResultsPanel,matchingEls.length === 0);
        paginator.render(matchingEls);
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}
