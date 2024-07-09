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
            <div id="displayCountMessage"></div>
            <div id="displayCount"></div>
            <div id="totalCount"></div>
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
                data-apiname="${panel}" data-index="${i}"></div>`;
        }).join('');
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
    function getVisiblePanelData(...props) {
        return Array.from(document.querySelectorAll('.hip-api'))
            .filter(isVisible)
            .map(el => props.reduce((acc, prop) => ({...acc, [prop]: el.dataset[prop]}), {index: parseInt(el.dataset.index)}));
    }
    
    it("only first page of my apis results are displayed when page loads",  () => {
        buildApiPanelsByCount(100);

        onPageShow();

        expect(getVisiblePanelData().map(p => p.index)).toEqual(
            [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
        );
    });

    it("when my apis page navigation occurs then the correct panels are shown",  () => {
        buildApiPanelsByCount(100);

        onPageShow();
        clickPageNumber(2);

        expect(getVisiblePanelData().map(p => p.index)).toEqual(
            [10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
        );

        expect(getCurrentPageNumber()).toBe(2);

    });

    it("when my apis name filter is applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        enterMyApiNameFilterText('api number 1');
        let myApisVisiblePanelData = getVisiblePanelData('apiname');
        expect(myApisVisiblePanelData).toEqual([
            { index: 0, apiname: 'api number 1'},
            { index: 9, apiname: 'api number 10'},
            { index: 10, apiname: 'api number 11'},
            { index: 11, apiname: 'api number 12'},
            { index: 12, apiname: 'api number 13'},
            { index: 13, apiname: 'api number 14'},
            { index: 14, apiname: 'api number 15'},
            { index: 15, apiname: 'api number 16'},
            { index: 16, apiname: 'api number 17'},
            { index: 17, apiname: 'api number 18'}
        ]);
    });
});
