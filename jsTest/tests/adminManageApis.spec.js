import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/adminManageApis.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo, isVisible} from "./testUtils.js";

describe('adminManageApis', () => {
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="apiDetailPanels">
                <div class="hip-api"></div>
            </div>
            <input id="apiFilter" type="text">
            <div id="noResultsPanel"></div>
            <div id="apiCount"></div>
            ${paginationContainerHtml}
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildApiPanels(count) {
        document.getElementById('apiDetailPanels').innerHTML = Array.from(
            {length: count},
            (_, i) => `<div class="hip-api" data-index="${i+1}" data-apiname="Api ${i+1}" data-apiref="Ref ${i+1}">Api ${i+1}</div>`
        ).join('');
    }

    function enterFilterText(value) {
        document.getElementById('apiFilter').value = value;
        document.getElementById('apiFilter').dispatchEvent(new Event('input'));
    }

    function noResultsPanelIsVisible() {
        return isVisible(document.getElementById('noResultsPanel'));
    }

    it("if 10 apis are present on the page then all are visible and pagination is not available",  () => {
        buildApiPanels(10);

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
        expect(noResultsPanelIsVisible()).toBeFalse();
    });

    it("if 11 apis are present on the page then only the first 10 are visible and pagination is available",  () => {
        buildApiPanels(11);

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
        expect(paginationHelper.getVisiblePanelIndexes('.hip-api')).toEqual(arrayFromTo(1, 10));
        expect(noResultsPanelIsVisible()).toBeFalse();
    });

    it("when the user enters some filter text then APIs with names that match the filter are shown",  () => {
        buildApiPanels(100);

        onPageShow();
        enterFilterText('Api 1');

        expect(paginationHelper.getVisiblePanelData('.hip-api', 'apiname').map(o => o.apiname)).toEqual([
            'Api 1', 'Api 10', 'Api 11', 'Api 12', 'Api 13', 'Api 14', 'Api 15', 'Api 16', 'Api 17', 'Api 18'
        ]);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();

        paginationHelper.clickNext();
        expect(paginationHelper.getVisiblePanelData('.hip-api', 'apiname').map(o => o.apiname)).toEqual(['Api 19', 'Api 100']);
        expect(noResultsPanelIsVisible()).toBeFalse();
    });

    it("when the user enters some filter text then APIs with publisher-refs that match the filter are shown",  () => {
        buildApiPanels(100);

        onPageShow();
        enterFilterText('Ref 1');

        expect(paginationHelper.getVisiblePanelData('.hip-api', 'apiref').map(o => o.apiref)).toEqual([
            'Ref 1', 'Ref 10', 'Ref 11', 'Ref 12', 'Ref 13', 'Ref 14', 'Ref 15', 'Ref 16', 'Ref 17', 'Ref 18'
        ]);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();

        paginationHelper.clickNext();
        expect(paginationHelper.getVisiblePanelData('.hip-api', 'apiref').map(o => o.apiref)).toEqual(['Ref 19', 'Ref 100']);
        expect(noResultsPanelIsVisible()).toBeFalse();
    });

    it("when the user enters some filter text that does not match any APIs then no APIs are shown",  () => {
        buildApiPanels(100);

        onPageShow();
        enterFilterText('nomathces');

        expect(paginationHelper.getVisiblePanelIndexes()).toEqual([]);
        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
        expect(noResultsPanelIsVisible()).toBeTrue();
    });
});
