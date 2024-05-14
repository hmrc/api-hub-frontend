import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/hipApis.js';

describe('hipApis', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="statusFilters">
                <input class="govuk-checkboxes__input" type="checkbox" value="ALPHA" checked="checked">
                <input class="govuk-checkboxes__input" type="checkbox" value="BETA" checked="checked">
                <input class="govuk-checkboxes__input" type="checkbox" value="LIVE" checked="checked">
                <input class="govuk-checkboxes__input" type="checkbox" value="DEPRECATED">                
            </div>
            <div id="apiList"></div>
            <div id="searchResultsSize"></div>
            <div id="pagination"></div>
        `));
        document = dom.window.document;
        globalThis.document = document;
    });

    function buildApiPanelsByCount(count) {
        const panels = [],
            statuses = ['ALPHA', 'BETA', 'LIVE', 'DEPRECATED'];
        let i= 0;
        while (i < count) {
            panels.push({apiStatus: statuses[i % statuses.length]});
            i++;
        }
        buildApiPanels(...panels);
    }
    function buildApiPanels(...panels) {
        document.getElementById('apiList').innerHTML = panels.map((panel, i) => {
            return `<div class="api-panel" data-apistatus="${panel.apiStatus}" data-index="${i}"></div>`;
        }).join('');
    }
    function getVisiblePanelData() {
        return Array.from(document.querySelectorAll('.api-panel'))
            .filter(el => el.style.display === 'block')
            .map(el => ({apiStatus: el.dataset['apistatus'], index: parseInt(el.dataset['index'])}));
    }
    function getResultCount() {
        return parseInt(document.getElementById('searchResultsSize').textContent);
    }
    function clickStatusFilter(value) {
        document.querySelector(`#statusFilters input[value="${value}"]`).click();
    }
    function clickPageNumber(pageNumber) {
        document.querySelector(`#pagination .govuk-pagination__link[data-page="${pageNumber}"]`).click();
    }

    it("when page initially displayed then only panels with selected statuses are visible",  () => {
        buildApiPanels({apiStatus: 'ALPHA'}, {apiStatus: 'BETA'}, {apiStatus: 'LIVE'}, {apiStatus: 'DEPRECATED'});

        onPageShow();

        expect(getVisiblePanelData()).toEqual([{apiStatus: 'ALPHA', index: 0}, {apiStatus: 'BETA', index: 1}, {apiStatus: 'LIVE', index: 2}]);
        expect(getResultCount()).toBe(3);
    });

    it("when status is deselected then panels with that status are hidden",  () => {
        buildApiPanels({apiStatus: 'ALPHA'}, {apiStatus: 'BETA'}, {apiStatus: 'LIVE'}, {apiStatus: 'DEPRECATED'});

        onPageShow();
        clickStatusFilter('BETA');

        expect(getVisiblePanelData()).toEqual([{apiStatus: 'ALPHA', index: 0}, {apiStatus: 'LIVE', index: 2}]);
        expect(getResultCount()).toBe(2);
    });

    it("when status is selected then panels with that status are shown",  () => {
        buildApiPanels({apiStatus: 'ALPHA'}, {apiStatus: 'BETA'}, {apiStatus: 'LIVE'}, {apiStatus: 'DEPRECATED'});

        onPageShow();
        clickStatusFilter('DEPRECATED');

        expect(getVisiblePanelData()).toEqual([{apiStatus: 'ALPHA', index: 0}, {apiStatus: 'BETA', index: 1}, {apiStatus: 'LIVE', index: 2}, {apiStatus: 'DEPRECATED', index: 3}]);
        expect(getResultCount()).toBe(4);
    });

    it("only first page of results are displayed when page loads",  () => {
        buildApiPanelsByCount(100);

        onPageShow();

        expect(getVisiblePanelData().map(p => p.index)).toEqual(
            [0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, 16, 17, 18] // 'DEPRECATED' apis hidden by default
        );
    });

    it("when page navigation occurs then the correct panels are shown",  () => {
        buildApiPanelsByCount(100);

        onPageShow();
        clickPageNumber(2);

        expect(getVisiblePanelData().map(p => p.index)).toEqual(
            [20, 21, 22, 24, 25, 26, 28, 29, 30, 32, 33, 34, 36, 37, 38] // 'DEPRECATED' apis hidden by default
        );
    });

    it("when filters change then page is reset to 1",  () => {
        buildApiPanelsByCount(100);

        onPageShow();
        clickPageNumber(2);
        clickStatusFilter('DEPRECATED');

        expect(getVisiblePanelData().map(p => p.index)).toEqual(
            [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
        );
    });

});
