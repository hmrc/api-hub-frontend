import {buildPaginator, HIDDEN_BY_PAGINATION} from './paginationController.js';
import {setVisible} from "./utils.js";

export function onDomLoaded() {
    const appDetailPanelEls = Array.from(document.querySelectorAll('#appDetailPanels .hip-application')),
        elPaginationContainer = document.getElementById('pagination'),
        elDisplayCountMessage = document.getElementById('displayCountMessage'),
        elDisplayCountShowing = document.getElementById('displayCount'),
        elDisplayCountTotal = document.getElementById('totalCount');

    const applicationPanels = appDetailPanelEls.map(el => ({
        el,
        get visible() {
            return !this[HIDDEN_BY_PAGINATION];
        }
    }));

    const paginator = buildPaginator(elPaginationContainer, 10);

    paginator.onPaginationChanged(paginationDetails => {
        applicationPanels.forEach(appPanel => {
            setVisible(appPanel.el, appPanel.visible);
        });

        setVisible(elDisplayCountMessage, paginationDetails.isPaginating);
        elDisplayCountShowing.textContent = paginationDetails.visibleItemCount;
        elDisplayCountTotal.textContent = paginationDetails.totalItemCount;
    });

    paginator.initialise(applicationPanels);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
