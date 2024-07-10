import {buildPaginator} from './paginationController.js';

export function onDomLoaded() {
    const appDetailPanelEls = Array.from(document.querySelectorAll('#appDetailPanels .hip-application')),
        paginator = buildPaginator(10);
    paginator.render(appDetailPanelEls);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
