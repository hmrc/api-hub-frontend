import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/adminAccessRequests.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('adminAccessRequests', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="accessRequestList">
                <div class="hip-access-request-panel"></div>
            </div>
            ${paginationContainerHtml}
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildRequestPanels(count) {
        document.getElementById('accessRequestList').innerHTML = Array.from(
            {length: count},
            (_, i) => `<div class="hip-access-request-panel" data-index="${i+1}">Request ${i+1}</div>`
        ).join('');
    }

    it("if 10 requests are present on the page then all are visible and pagination is not available",  () => {
        buildRequestPanels(10);

        onDomLoaded();

        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
    });

    it("if 11 requests are present on the page then only the first 10 are visible and pagination is available",  () => {
        buildRequestPanels(11);

        onDomLoaded();

        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
    });

    it("when the page loads only the first 10 requests are visible and the display message is correct",  () => {
        buildRequestPanels(101);

        onDomLoaded();

        expect(paginationHelper.getShowingCount()).toBe(10);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-access-request-panel')).toEqual(arrayFromTo(1, 10));
    });

    it("when we navigate to the second page the correct requests are visible and the display message is correct",  () => {
        buildRequestPanels(101);

        onDomLoaded();
        paginationHelper.getPaginationPageLink(2).click();

        expect(paginationHelper.getShowingCount()).toBe(10);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-access-request-panel')).toEqual(arrayFromTo(11, 20));
    });

    it("when we navigate to the final page the correct requests are visible and the display message is correct",  () => {
        buildRequestPanels(101);

        onDomLoaded();
        paginationHelper.getPaginationPageLink(11).click();

        expect(paginationHelper.getShowingCount()).toBe(1);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-access-request-panel')).toEqual(arrayFromTo(101, 101));

    });
});
