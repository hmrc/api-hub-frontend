import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/myApis.js';
import {isVisible} from "./testUtils.js";

describe('myApis', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <input id="nameFilter">
            <div id="myApisPanels"></div>
            <div id="searchResultsSize"></div>
            <div id="pagination"></div>
            
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
            return `<div class="api-panel" 
                data-apiname="${panel.name}"></div>`;
        }).join('');
    }
    function getVisiblePanelData(...props) {
        return Array.from(document.querySelectorAll('.api-panel'))
            .filter(isVisible)
            .map(el => props.reduce((acc, prop) => ({...acc, [prop]: el.dataset[prop]}), {index: parseInt(el.dataset.index)}));
    }
    function getResultCount() {
        return parseInt(document.getElementById('searchResultsSize').textContent);
    }
    function clickPageNumber(pageNumber) {
        document.querySelector(`#pagination .govuk-pagination__link[data-page="${pageNumber}"]`).click();
    }
    function getCurrentPageNumber() {
        return parseInt(document.querySelector('.govuk-pagination__item--current').textContent);
    }
    function enterMyApiNameFilterText(value) {
        document.getElementById('nameFilter').value = value;
        document.getElementById('nameFilter').dispatchEvent(new Event('input'));
    }
    function getMyApiNameFilterText() {
        return document.getElementById('nameFilter').value;
    }
    
    it("only first page of my apis results are displayed when page loads",  () => {
        buildApiPanelsByCount(100);

        onPageShow();

        expect(getVisiblePanelData().map(p => p.index)).toEqual(
            [0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, 16, 17, 18] // 'DEPRECATED' apis hidden by default
        );
    });

    it("when my apis page navigation occurs then the correct panels are shown",  () => {
        buildApiPanelsByCount(100);

        onPageShow();
        clickPageNumber(2);

        expect(getVisiblePanelData().map(p => p.index)).toEqual(
            [20, 21, 22, 24, 25, 26, 28, 29, 30, 32, 33, 34, 36, 37, 38] // 'DEPRECATED' apis hidden by default
        );
    });

    it("when second page of results is displayed and reset filters link and is clicked then we return to the first page",  () => {
        onPageShow();
        clickPageNumber(2);

        expect(getCurrentPageNumber()).toBe(1);
    });

    it("when my apis name filter is applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        enterMyApiNameFilterText('api number 1');
        expect(getVisiblePanelData('apiname')).toEqual([
            { index: 0, apiname: 'api number 1'},
            { index: 9, apiname: 'api number 10'},
            { index: 10, apiname: 'api number 11'},
            { index: 12, apiname: 'api number 13'},
            { index: 13, apiname: 'api number 14'},
            { index: 14, apiname: 'api number 15'},
            { index: 16, apiname: 'api number 17'},
            { index: 17, apiname: 'api number 18'},
            { index: 18, apiname: 'api number 19'},
        ]);
    });
});
