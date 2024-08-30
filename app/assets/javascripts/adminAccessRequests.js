import {buildPaginator} from './paginationController.js';

export function onDomLoaded() {
    const accessRequestPanelEls = Array.from(document.querySelectorAll('#accessRequestList .hip-access-request-panel')),
        paginator = buildPaginator(10);
    paginator.render(accessRequestPanelEls);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
