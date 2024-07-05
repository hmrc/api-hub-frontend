import {buildPaginator, HIDDEN_BY_PAGINATION} from './paginationController.js';
import {setVisible} from "./utils.js";

export function onDomLoaded() {
    const apiPanelEls = Array.from(document.querySelectorAll('#myApisPanels .hip-api')),
        elPaginationContainer = document.getElementById('pagination'),
        elDisplayCountMessage = document.getElementById('displayCountMessage'),
        elDisplayCountShowing = document.getElementById('displayCount'),
        elDisplayCountTotal = document.getElementById('totalCount');

    const apiPanels = apiPanelEls.map(el => ({
        el,
        get visible() {
            return !this[HIDDEN_BY_PAGINATION];
        }
    }));

    const paginator = buildPaginator(elPaginationContainer, 20);

    paginator.onPaginationChanged(paginationDetails => {
        apiPanels.forEach(apiPanel => {
            setVisible(apiPanel.el, apiPanel.visible);
        });

        setVisible(elDisplayCountMessage, paginationDetails.isPaginating);
        elDisplayCountShowing.textContent = paginationDetails.visibleItemCount;
        elDisplayCountTotal.textContent = paginationDetails.totalItemCount;
    });

    paginator.initialise(apiPanels);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
