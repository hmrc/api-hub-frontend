import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/hipApis.js';

describe('hipApis', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="statusFilters">
                <input class="govuk-checkboxes__input" type="checkbox" value="ALPHA" checked>
                <input class="govuk-checkboxes__input" type="checkbox" value="BETA" checked>
                <input class="govuk-checkboxes__input" type="checkbox" value="LIVE" checked>
                <input class="govuk-checkboxes__input" type="checkbox" value="DEPRECATED">                
            </div>
            <div id="domainFilters">
                <div><input class="domainFilter" type="checkbox" value="d1"></div>
                <div class="subdomainCheckboxes" data-domain="d1">
                    <div><input class="subDomainFilter" type="checkbox" value="d1s1" data-domain="d1"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d1s2" data-domain="d1"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d1s3" data-domain="d1"></div>
                </div>
                <div><input class="domainFilter" type="checkbox" value="d2"></div>
                <div class="subdomainCheckboxes" data-domain="d2">
                    <div><input class="subDomainFilter" type="checkbox" value="d2s1" data-domain="d2"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d2s2" data-domain="d2"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d2s3" data-domain="d2"></div>
                </div>
                <div><input class="domainFilter" type="checkbox" value="d3"></div>
                <div class="subdomainCheckboxes" data-domain="d3">
                    <div><input class="subDomainFilter" type="checkbox" value="d3s1" data-domain="d3"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d3s2" data-domain="d3"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d3s3" data-domain="d3"></div>
                </div>
            </div>            
            <div id="apiList"></div>
            <div id="searchResultsSize"></div>
            <div id="pagination"></div>
            <div id="noResultsPanel"></div>
            <div id="resetFilters"></div>
            <div id="noResultsClearFilters"></div>
            <div id="domainFilterCount"></div>
            <div id="viewDomainFilters"></div>
        `));
        document = dom.window.document;
        globalThis.document = document;
    });

    function buildApiPanelsByCount(count) {
        const panels = [],
            statuses = ['ALPHA', 'BETA', 'LIVE', 'DEPRECATED'],
            domainValues = [
                ['d1', 'd1s1'],
                ['d1', 'd1s2'],
                ['d1', 'd1s3'],
                ['d2', 'd2s1'],
                ['d3', ''],
                ['', 'd3s1'],
                ['dx', 'dxs1'],
                ['', ''],
            ];
        let i= 0;
        while (i < count) {
            const apiStatus = statuses[i % statuses.length],
                [domain, subdomain] = domainValues[i % domainValues.length];
            panels.push({apiStatus, domain, subdomain})
            i++;
        }
        buildApiPanels(...panels);
    }
    function buildApiPanels(...panels) {
        document.getElementById('apiList').innerHTML = panels.map((panel, i) => {
            return `<div class="api-panel" data-apistatus="${panel.apiStatus}" data-domain="${panel.domain}" data-subdomain="${panel.subdomain}" data-index="${i}"></div>`;
        }).join('');
    }
    function getVisiblePanelData() {
        return Array.from(document.querySelectorAll('.api-panel'))
            .filter(el => el.style.display === 'block')
            .map(el => ({
                apiStatus: el.dataset['apistatus'],
                index: parseInt(el.dataset['index']),
                domain: el.dataset['domain'],
                subdomain: el.dataset['subdomain']
            }));
    }
    function getResultCount() {
        return parseInt(document.getElementById('searchResultsSize').textContent);
    }
    function clickDomainFilter(value) {
        document.querySelector(`input[value="${value}"].domainFilter`).click();
    }
    function clickSubdomainFilter(value) {
        document.querySelector(`input[value="${value}"].subDomainFilter`).click();
    }
    function clickStatusFilter(value) {
        document.querySelector(`#statusFilters input[value="${value}"]`).click();
    }
    function clickPageNumber(pageNumber) {
        document.querySelector(`#pagination .govuk-pagination__link[data-page="${pageNumber}"]`).click();
    }
    function indexAndStatus(panel) {
        return {apiStatus: panel.apiStatus, index: panel.index};
    }

    it("when page initially displayed then only panels with selected statuses are visible",  () => {
        buildApiPanels({apiStatus: 'ALPHA'}, {apiStatus: 'BETA'}, {apiStatus: 'LIVE'}, {apiStatus: 'DEPRECATED'});

        onPageShow();

        expect(getVisiblePanelData().map(indexAndStatus)).toEqual([{apiStatus: 'ALPHA', index: 0}, {apiStatus: 'BETA', index: 1}, {apiStatus: 'LIVE', index: 2}]);
        expect(getResultCount()).toBe(3);
    });

    it("when status is deselected then panels with that status are hidden",  () => {
        buildApiPanels({apiStatus: 'ALPHA'}, {apiStatus: 'BETA'}, {apiStatus: 'LIVE'}, {apiStatus: 'DEPRECATED'});

        onPageShow();
        clickStatusFilter('BETA');

        expect(getVisiblePanelData().map(indexAndStatus)).toEqual([{apiStatus: 'ALPHA', index: 0}, {apiStatus: 'LIVE', index: 2}]);
        expect(getResultCount()).toBe(2);
    });

    it("when status is selected then panels with that status are shown",  () => {
        buildApiPanels({apiStatus: 'ALPHA'}, {apiStatus: 'BETA'}, {apiStatus: 'LIVE'}, {apiStatus: 'DEPRECATED'});

        onPageShow();
        clickStatusFilter('DEPRECATED');

        expect(getVisiblePanelData().map(indexAndStatus)).toEqual([{apiStatus: 'ALPHA', index: 0}, {apiStatus: 'BETA', index: 1}, {apiStatus: 'LIVE', index: 2}, {apiStatus: 'DEPRECATED', index: 3}]);
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

    it("when domain filter is applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickDomainFilter('d1');

        expect(getVisiblePanelData()).toEqual([
            { apiStatus: 'ALPHA', index: 0, domain: 'd1', subdomain: 'd1s1' },
            { apiStatus: 'BETA', index: 1, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 2, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'ALPHA', index: 8, domain: 'd1', subdomain: 'd1s1' },
            { apiStatus: 'BETA', index: 9, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 10, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'ALPHA', index: 16, domain: 'd1', subdomain: 'd1s1' },
            { apiStatus: 'BETA', index: 17, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 18, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'ALPHA', index: 24, domain: 'd1', subdomain: 'd1s1' },
            { apiStatus: 'BETA', index: 25, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 26, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'ALPHA', index: 32, domain: 'd1', subdomain: 'd1s1' },
            { apiStatus: 'BETA', index: 33, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 34, domain: 'd1', subdomain: 'd1s3' }
        ]);
    });

    it("when multiple domain filters are applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickDomainFilter('d1');
        clickSubdomainFilter('d1s1');
        clickDomainFilter('d2');

        expect(getVisiblePanelData()).toEqual([
            { apiStatus: 'BETA', index: 1, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 2, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'BETA', index: 9, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 10, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'BETA', index: 17, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 18, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'BETA', index: 25, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 26, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'BETA', index: 33, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 34, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'BETA', index: 41, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 42, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'BETA', index: 49, domain: 'd1', subdomain: 'd1s2' },
            { apiStatus: 'LIVE', index: 50, domain: 'd1', subdomain: 'd1s3' },
            { apiStatus: 'BETA', index: 57, domain: 'd1', subdomain: 'd1s2' }
        ]);
    });

});
