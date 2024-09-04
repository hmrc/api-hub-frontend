import {buildPaginator} from './paginationController.js';
import {setVisible} from "./utils.js";
import {buildTextFilter, dataAttribute} from "./textFilter.js";

export function onPageShow() {
    const elSearchResultsSize = document.getElementById('searchResultsSize'),
        elNoSearchResults = document.getElementById('noSearchResults'),
        paginator = buildPaginator(10),
        filter = buildTextFilter(
            document.querySelectorAll('#myApisPanels .hip-api'),
            document.getElementById('nameFilter'),
            [
                dataAttribute('apiname').whenNormalised().includesTheFilterText()
            ]
        );

    filter.onChange(matchingEls => {
        elSearchResultsSize.textContent = matchingEls.length;
        setVisible(elNoSearchResults,matchingEls.length === 0);
        paginator.render(matchingEls);
    });

}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
