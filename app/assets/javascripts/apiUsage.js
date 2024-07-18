import {buildPaginator} from './paginationController.js';

export function onDomLoaded() {
    const applicationPanelEls = Array.from(document.querySelectorAll('#applicationsTable .govuk-table__body .govuk-table__row'));

    if (applicationPanelEls.length) {
        const paginator = buildPaginator(10);
        paginator.render(applicationPanelEls);
    }

}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
