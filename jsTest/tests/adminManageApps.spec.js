import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/adminManageApps.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('adminManageApps', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="appDetailPanels">
                <div class="hip-application"></div>
            </div>
            ${paginationContainerHtml}
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildAppPanels(count) {
        document.getElementById('appDetailPanels').innerHTML = Array.from(
            {length: count},
            (_, i) => `<div class="hip-application" data-index="${i+1}">App ${i+1}</div>`
        ).join('');
    }

    it("if 20 applications are present on the page then all are visible and pagination is not available",  () => {
        buildAppPanels(20);

        onDomLoaded();

        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
    });

    it("if 21 applications are present on the page then only the first 20 are visible and pagination is available",  () => {
        buildAppPanels(21);

        onDomLoaded();

        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
    });

    it("when the page loads only the first 20 applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();

        expect(paginationHelper.getShowingCount()).toBe(20);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-application')).toEqual(arrayFromTo(1, 20));
    });

    it("when we navigate to the second page the correct applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();
        paginationHelper.getPaginationPageLink(2).click();

        expect(paginationHelper.getShowingCount()).toBe(20);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-application')).toEqual(arrayFromTo(21, 40));
    });

    it("when we navigate to the final page the correct applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();
        paginationHelper.getPaginationPageLink(6).click();

        expect(paginationHelper.getShowingCount()).toBe(1);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.hip-application')).toEqual(arrayFromTo(101, 101));
    });
});
