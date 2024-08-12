import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/myApis.js';
import {paginationHelper, paginationContainerHtml, isVisible} from "./testUtils.js";

describe('myApis', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <input id="nameFilter">
            <div id="myApisPanels"></div>
            <div id="searchResultsSize"></div>
            <div id="noSearchResults"></div>
            ${paginationContainerHtml}
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildApiPanelsByCount(count) {
        const panels = [],
            names = [...Array(count)].map((_, i) => `api number ${i + 1}`);
        let i= 0;
        while (i < count) {
            const name = names[i];
            panels.push(name)
            i++;
        }
        buildApiPanels(...panels);
    }
    function buildApiPanels(...panels) {
        document.getElementById('myApisPanels').innerHTML = panels.map((panel, i) => {
            return `<div class="hip-api" 
                data-id="${i}"
                data-apiname="${panel}" data-index="${i}"></div>`;
        }).join('');
    }
    function enterMyApiNameFilterText(value) {
        document.getElementById('nameFilter').value = value;
        document.getElementById('nameFilter').dispatchEvent(new Event('input'));
    }

    it("only first page of my apis results are displayed when page loads",  () => {
        buildApiPanelsByCount(100);

        onPageShow();

        expect(paginationHelper.getVisiblePanelIndexes('.hip-api')).toEqual(
            [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
        );
    });

    it("when my apis page navigation occurs then the correct panels are shown",  () => {
        buildApiPanelsByCount(100);

        onPageShow();
        paginationHelper.getPaginationPageLink(2).click();

        expect(paginationHelper.getVisiblePanelIndexes('.hip-api')).toEqual(
            [10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
        );

        expect(paginationHelper.getCurrentPageNumber()).toBe(2);

    });

    it("when my apis name filter is applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        enterMyApiNameFilterText('api number 1');
        let myApisVisiblePanelData = paginationHelper.getVisiblePanelData('.hip-api', 'apiname');

        expect(myApisVisiblePanelData).toEqual([
            { id: 0, apiname: 'api number 1'},
            { id: 9, apiname: 'api number 10'},
            { id: 10, apiname: 'api number 11'},
            { id: 11, apiname: 'api number 12'},
            { id: 12, apiname: 'api number 13'},
            { id: 13, apiname: 'api number 14'},
            { id: 14, apiname: 'api number 15'},
            { id: 15, apiname: 'api number 16'},
            { id: 16, apiname: 'api number 17'},
            { id: 17, apiname: 'api number 18'}
        ]);
        expect(isVisible(document.querySelector('[id=noSearchResults]'))).toBeFalse();
    });

    it("when my apis name filter is applied and there are no results then appropriate message is shown",  () => {
        buildApiPanelsByCount(10);
        onPageShow();

        enterMyApiNameFilterText('xylophone');

        expect(isVisible(document.querySelector('[id=noSearchResults]'))).toBeTrue();
    });

});
