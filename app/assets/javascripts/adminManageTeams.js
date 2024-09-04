import {buildPaginator} from './paginationController.js';
import {buildTextFilter, dataAttribute} from "./textFilter.js";

export function onPageShow() {
    const elTeamCount = document.getElementById('teamCount'),
        paginator = buildPaginator(10),
        filter = buildTextFilter(
            document.querySelectorAll('#teamsTable .govuk-table__body .govuk-table__row'),
            document.getElementById('teamFilter'),
            [
                dataAttribute('name').whenNormalised().includesTheFilterText(),
                dataAttribute('id').whenNormalised().includesTheFilterText(),
                dataAttribute('emails').whenSplitBy(',').whenNormalised().startsWithTheFilterText(),
            ]
        );

    filter.onChange(matchingEls => {
        elTeamCount.textContent = matchingEls.length;
        paginator.render(matchingEls);
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}
