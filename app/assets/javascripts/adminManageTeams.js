import {buildPaginator} from './paginationController.js';

export function onDomLoaded() {
    const teamRowEls = Array.from(document.querySelectorAll('#teamsTable .govuk-table__row')),
        paginator = buildPaginator(10);

    paginator.render(teamRowEls);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
