import {buildPaginator} from './paginationController.js';
import {setVisible} from "./utils.js";
import {buildTextFilter, dataAttribute} from "./textFilter.js";

export function onPageShow() {
    const elAppCount = document.getElementById('appCount'),
        elNoResultsPanel = document.getElementById('noResultsPanel'),
        paginator = buildPaginator(20),
        filter = buildTextFilter(
            document.querySelectorAll('#appDetailPanels .hip-app'),
            document.getElementById('appFilter'),
            [
                dataAttribute('appName').whenNormalised().includesTheFilterText(),
                dataAttribute('appId').whenNormalised().includesTheFilterText()
            ]
        );

    filter.onChange(matchingEls => {
        elAppCount.textContent = matchingEls.length;
        if (elNoResultsPanel) {
            setVisible(elNoResultsPanel, matchingEls.length === 0);
        }
        paginator.render(matchingEls);
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}