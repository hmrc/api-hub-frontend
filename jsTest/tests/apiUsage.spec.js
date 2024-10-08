import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/apiUsage.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('apiUsage', () => {
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="applicationsTable">
                <div id="table" class="govuk-table__body"></div>
            </div>
            ${paginationContainerHtml}
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildAppPanels(count) {
        document.getElementById('table').innerHTML = Array.from(
            {length: count},
            (_, i) => `<div class="govuk-table__row" data-index="${i+1}">App ${i+1}</div>`
        ).join('');
    }

    it("if 10 applications are present on the page then all are visible and pagination is not available",  () => {
        buildAppPanels(10);

        onDomLoaded();

        expect(paginationHelper.getVisiblePanelIndexes('.govuk-table__row')).toEqual(arrayFromTo(1, 10));
        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
    });

    it("if 11 applications are present on the page then only the first 10 are visible and pagination is available",  () => {
        buildAppPanels(11);

        onDomLoaded();

        expect(paginationHelper.getVisiblePanelIndexes('.govuk-table__row')).toEqual(arrayFromTo(1, 10));
        expect(paginationHelper.getShowingCount()).toBe(10);
        expect(paginationHelper.getTotalCount()).toBe(11);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
    });

    it("when we navigate to the second page the correct applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();
        paginationHelper.getPaginationPageLink(2).click();

        expect(paginationHelper.getShowingCount()).toBe(10);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.govuk-table__row')).toEqual(arrayFromTo(11, 20));
    });

    it("when we navigate to the final page the correct applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();
        paginationHelper.getPaginationPageLink(11).click();

        expect(paginationHelper.getShowingCount()).toBe(1);
        expect(paginationHelper.getTotalCount()).toBe(101);
        expect(paginationHelper.getVisiblePanelIndexes('.govuk-table__row')).toEqual(arrayFromTo(101, 101));
    });
});
