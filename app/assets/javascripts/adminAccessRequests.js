import {buildPaginator} from './paginationController.js';
import {setVisible} from "./utils.js";

export function onPageShow() {
    const filterByStatusCheckboxEls = Array.from(document.querySelectorAll('#filterByStatusCheckboxes input[type="checkbox"]')),
        accessRequestPanelEls = Array.from(document.querySelectorAll('#accessRequestList .hip-access-request-panel')),
        elRequestCount = document.getElementById('requestCount'),
        paginator = buildPaginator(10),
        requestsModel = accessRequestPanelEls.map(el => ({
            el,
            status: el.dataset.status,
            hiddenByFilters: false
        }));

    function onFilterUpdate() {
        const selectedStatuses = new Set(filterByStatusCheckboxEls
            .filter(checkbox => checkbox.checked)
            .map(checkbox => checkbox.value)
        );

        requestsModel.forEach(accessRequest => {
            accessRequest.hiddenByFilters = selectedStatuses.size > 0 && !selectedStatuses.has(accessRequest.status);
            setVisible(accessRequest.el, !accessRequest.hiddenByFilters);
        });

        const unfilteredRequests = requestsModel.filter(accessRequest => !accessRequest.hiddenByFilters);
        elRequestCount.textContent = unfilteredRequests.length.toString();
        paginator.render(unfilteredRequests.map(accessRequest => accessRequest.el));
    }
    filterByStatusCheckboxEls.forEach(checkbox => checkbox.addEventListener('change', onFilterUpdate));

    onFilterUpdate();
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}
