import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/adminManageApis.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('adminManageApis', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="apiDetailPanels">
                <div class="hip-api"></div>
            </div>
            <input id="nameFilter" type="text">
            <div id="noResultsPanel"></div>
            <div id="apiCount"></div>
            ${paginationContainerHtml}
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildApiPanels(count) {
        document.getElementById('apiDetailPanels').innerHTML = Array.from(
            {length: count},
            (_, i) => `<div class="hip-api" data-index="${i+1}" data-apiname="Api ${i+1}">Api ${i+1}</div>`
        ).join('');
    }

    function enterNameFilterText(value) {
        document.getElementById('nameFilter').value = value;
        document.getElementById('nameFilter').dispatchEvent(new Event('input'));
    }

    it("if 10 apis are present on the page then all are visible and pagination is not available",  () => {
        buildApiPanels(10);

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
    });

    it("if 11 apis are present on the page then only the first 10 are visible and pagination is available",  () => {
        buildApiPanels(11);

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
        expect(paginationHelper.getVisiblePanelIndexes('.hip-api')).toEqual(arrayFromTo(1, 10));
    });

    it("when the user enters some filter text then only the APIs that match the filter are shown",  () => {
        buildApiPanels(100);

        onPageShow();
        enterNameFilterText('Api 1');

        expect(paginationHelper.getVisiblePanelData('.hip-api', 'apiname').map(o => o.apiname)).toEqual([
            'Api 1', 'Api 10', 'Api 11', 'Api 12', 'Api 13', 'Api 14', 'Api 15', 'Api 16', 'Api 17', 'Api 18'
        ]);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();

        paginationHelper.clickNext();
        expect(paginationHelper.getVisiblePanelData('.hip-api', 'apiname').map(o => o.apiname)).toEqual(['Api 19', 'Api 100']);
    });

});
