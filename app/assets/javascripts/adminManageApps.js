import {buildPaginator} from './paginationController.js';
import {setVisible} from "./utils.js";
import {buildTextFilter, dataAttribute} from "./textFilter.js";

export function onPageShow() {
    const elAppCount = document.getElementById('appCount'),
        elNoResultsPanel = document.getElementById('noResultsPanel'),
        paginator = buildPaginator(20),
        filter = buildTextFilter(
            document.querySelectorAll('#appDetailPanels .hip-application'),
            document.getElementById('appFilter'),
            [
                dataAttribute('appName').whenNormalised().includesTheFilterText(),
                dataAttribute('appId').whenNormalised().includesTheFilterText(),
                dataAttribute('clientIds').whenSplitBy(',').whenNormalised().includesTheFilterText(),
            ]
        );

    filter.onChange(matchingEls => {
        elAppCount.textContent = matchingEls.length;
        setVisible(elNoResultsPanel,matchingEls.length === 0);
        paginator.render(matchingEls);
    });

}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}