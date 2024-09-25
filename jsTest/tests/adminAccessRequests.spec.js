import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/adminAccessRequests.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('adminAccessRequests', () => {
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="accessRequestList">
                <div class="hip-access-request-panel"></div>
            </div>
            <span id="requestCount"></span>
            <div id="filterByStatusCheckboxes">
                <input type="checkbox" value="PENDING" checked>
                <input type="checkbox" value="APPROVED">
                <input type="checkbox" value="REJECTED">
                <input type="checkbox" value="CANCELLED">
            </div>
            ${paginationContainerHtml}
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    const statusValues = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];
    function buildRequestPanels(count) {
        document.getElementById('accessRequestList').innerHTML = Array.from(
            {length: count},
            (_, i) => `<div class="hip-access-request-panel" data-index="${i+1}" data-id="${i+1}" data-status="${statusValues[i % statusValues.length]}" >Request ${i+1}</div>`
        ).join('');
    }
    function clearFilters() {
        document.querySelectorAll('#filterByStatusCheckboxes input[type="checkbox"]').forEach(checkbox => checkbox.checked = false);
    }
    function clickFilterCheckbox(value) {
        document.querySelector(`#filterByStatusCheckboxes input[value="${value}"]`).click();
    }
    function getRequestCount() {
        return parseInt(document.getElementById('requestCount').textContent);
    }

    it("if 10 requests are present on the page then all are visible and pagination is not available",  () => {
        buildRequestPanels(10);
        clearFilters();

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
    });

    it("if 11 requests are present on the page then only the first 10 are visible and pagination is available",  () => {
        buildRequestPanels(11);
        clearFilters();

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
    });

    it("when the page loads only the first 10 requests are visible and the display message is correct",  () => {
        buildRequestPanels(101);
        clearFilters();

        onPageShow();

        expect(paginationHelper.getShowingCount()).toBe(10);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-access-request-panel')).toEqual(arrayFromTo(1, 10));
        expect(getRequestCount()).toBe(101);
    });

    it("when we navigate to the second page the correct requests are visible and the display message is correct",  () => {
        buildRequestPanels(101);
        clearFilters();

        onPageShow();
        paginationHelper.getPaginationPageLink(2).click();

        expect(paginationHelper.getShowingCount()).toBe(10);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-access-request-panel')).toEqual(arrayFromTo(11, 20));
        expect(getRequestCount()).toBe(101);
    });

    it("when we navigate to the final page the correct requests are visible and the display message is correct",  () => {
        buildRequestPanels(101);
        clearFilters();

        onPageShow();
        paginationHelper.getPaginationPageLink(11).click();

        expect(paginationHelper.getShowingCount()).toBe(1);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-access-request-panel')).toEqual(arrayFromTo(101, 101));
        expect(getRequestCount()).toBe(101);
    });

    it("when the page is first displayed only pending requests are visible",  () => {
        buildRequestPanels(8);

        onPageShow();
        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
        expect(paginationHelper.getVisiblePanelData('.hip-access-request-panel', 'status').map(o => o.status)).toEqual(['PENDING', 'PENDING']);
        expect(getRequestCount()).toBe(2);
    });

    it("when the user click additional filters the visible results are updated correctly",  () => {
        buildRequestPanels(8);

        onPageShow();

        expect(getRequestCount()).toBe(2);
        expect(paginationHelper.getVisiblePanelData('.hip-access-request-panel', 'status')).toEqual([
            {id: 1, status: "PENDING"},
            {id: 5, status: "PENDING"},
        ]);

        clickFilterCheckbox('APPROVED');
        expect(getRequestCount()).toBe(4);
        expect(paginationHelper.getVisiblePanelData('.hip-access-request-panel', 'status')).toEqual([
            {id: 1, status: "PENDING"},
            {id: 2, status: "APPROVED"},
            {id: 5, status: "PENDING"},
            {id: 6, status: "APPROVED"}
        ]);

        clickFilterCheckbox('REJECTED');
        expect(getRequestCount()).toBe(6);
        expect(paginationHelper.getVisiblePanelData('.hip-access-request-panel', 'status')).toEqual([
            {id: 1, status: "PENDING"},
            {id: 2, status: "APPROVED"},
            {id: 3, status: "REJECTED"},
            {id: 5, status: "PENDING"},
            {id: 6, status: "APPROVED"},
            {id: 7, status: "REJECTED"}
        ]);

        clickFilterCheckbox('APPROVED');
        expect(getRequestCount()).toBe(4);
        expect(paginationHelper.getVisiblePanelData('.hip-access-request-panel', 'status')).toEqual([
            {id: 1, status: "PENDING"},
            {id: 3, status: "REJECTED"},
            {id: 5, status: "PENDING"},
            {id: 7, status: "REJECTED"}
        ]);
    });

    it("when all filters are selected all results are displayed",  () => {
        buildRequestPanels(8);
        clearFilters();

        onPageShow();
        clickFilterCheckbox('APPROVED');
        clickFilterCheckbox('REJECTED');
        clickFilterCheckbox('PENDING');
        clickFilterCheckbox('CANCELLED');

        expect(getRequestCount()).toBe(8);
    });

    it("pagination is applied to the filtered results",  () => {
        buildRequestPanels(100);

        // PENDING filter is applied by default
        onPageShow();

        expect(paginationHelper.getShowingCount()).toBe(10);
        expect(paginationHelper.getTotalCount()).toBe(25);
        expect(getRequestCount()).toBe(25);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-access-request-panel')).toEqual([1, 5, 9, 13, 17, 21, 25, 29, 33, 37]);
    });

});
