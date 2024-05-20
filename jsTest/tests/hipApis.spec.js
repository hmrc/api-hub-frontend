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
            <div id="hodFilters">
                <div><input class="hodFilter" type="checkbox" value="ems"></div>
                <div><input class="hodFilter" type="checkbox" value="internal"></div>
                <div><input class="hodFilter" type="checkbox" value="apim"></div>
            </div>            
            <div id="apiList"></div>
            <div id="searchResultsSize"></div>
            <div id="pagination"></div>
            <div id="noResultsPanel"></div>
            <div id="resetFilters"></div>
            <div id="noResultsClearFilters"></div>
            <div id="domainFilterCount"></div>
            <details id="viewDomainFilters"><summary></summary></details>
            <div id="hodFilterCount"></div>
            <details id="viewHodFilters"><summary></summary></details>
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
            ],
            hodsValues = ['', 'ems', 'internal,ems,invalid', 'apim', 'invalid'];
        let i= 0;
        while (i < count) {
            const apiStatus = statuses[i % statuses.length],
                [domain, subdomain] = domainValues[i % domainValues.length],
                hods = hodsValues[i % hodsValues.length];
            panels.push({apiStatus, domain, subdomain, hods})
            i++;
        }
        buildApiPanels(...panels);
    }
    function buildApiPanels(...panels) {
        document.getElementById('apiList').innerHTML = panels.map((panel, i) => {
            return `<div class="api-panel" data-apistatus="${panel.apiStatus}" data-domain="${panel.domain || ''}" data-subdomain="${panel.subdomain || ''}" data-index="${i}" data-hods="${panel.hods || ''}"></div>`;
        }).join('');
    }
    function getVisiblePanelData() {
        return Array.from(document.querySelectorAll('.api-panel'))
            .filter(el => el.style.display === 'block')
            .map(el => ({
                apiStatus: el.dataset['apistatus'],
                index: parseInt(el.dataset['index']),
                domain: el.dataset['domain'],
                subdomain: el.dataset['subdomain'],
                hods: el.dataset['hods']
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
    function domainFilterIsSelected(value) {
        return document.querySelector(`input[value="${value}"].domainFilter`).checked;
    }
    function subdomainFilterIsSelected(value) {
        return document.querySelector(`input[value="${value}"].subDomainFilter`).checked;
    }
    function clickStatusFilter(value) {
        document.querySelector(`#statusFilters input[value="${value}"]`).click();
    }
    function statusFilterIsSelected(value) {
        return document.querySelector(`#statusFilters input[value="${value}"]`).checked;
    }
    function clickHodFilter(value) {
        document.querySelector(`#hodFilters input[value="${value}"]`).click();
    }
    function hodFilterIsSelected(value) {
        return document.querySelector(`#hodFilters input[value="${value}"]`).checked;
    }
    function domainFiltersCollapsed() {
        return document.getElementById('viewDomainFilters').open === false;
    }
    function hodFiltersCollapsed() {
        return document.getElementById('viewHodFilters').open === false;
    }
    function clickPageNumber(pageNumber) {
        document.querySelector(`#pagination .govuk-pagination__link[data-page="${pageNumber}"]`).click();
    }
    function getCurrentPageNumber() {
        return parseInt(document.querySelector('.govuk-pagination__item--current').textContent);
    }
    function noResultsPanelIsVisible() {
        return document.getElementById('noResultsPanel').style.display === 'block';
    }
    function indexAndStatus(panel) {
        return {apiStatus: panel.apiStatus, index: panel.index};
    }

    it("when page initially displayed then only panels with selected statuses are visible",  () => {
        buildApiPanels({apiStatus: 'ALPHA'}, {apiStatus: 'BETA'}, {apiStatus: 'LIVE'}, {apiStatus: 'DEPRECATED'});
        console.log(document.getElementById('apiList').innerHTML);

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
            { apiStatus: 'ALPHA', index: 0, domain: 'd1', subdomain: 'd1s1', hods: ''},
            { apiStatus: 'BETA', index: 1, domain: 'd1', subdomain: 'd1s2', hods: 'ems'},
            { apiStatus: 'LIVE', index: 2, domain: 'd1', subdomain: 'd1s3', hods: 'internal,ems,invalid'},
            { apiStatus: 'ALPHA', index: 8, domain: 'd1', subdomain: 'd1s1', hods: 'apim'},
            { apiStatus: 'BETA', index: 9, domain: 'd1', subdomain: 'd1s2', hods: 'invalid'},
            { apiStatus: 'LIVE', index: 10, domain: 'd1', subdomain: 'd1s3', hods: ''},
            { apiStatus: 'ALPHA', index: 16, domain: 'd1', subdomain: 'd1s1', hods: 'ems'},
            { apiStatus: 'BETA', index: 17, domain: 'd1', subdomain: 'd1s2', hods: 'internal,ems,invalid'},
            { apiStatus: 'LIVE', index: 18, domain: 'd1', subdomain: 'd1s3', hods: 'apim'},
            { apiStatus: 'ALPHA', index: 24, domain: 'd1', subdomain: 'd1s1', hods: 'invalid'},
            { apiStatus: 'BETA', index: 25, domain: 'd1', subdomain: 'd1s2', hods: ''},
            { apiStatus: 'LIVE', index: 26, domain: 'd1', subdomain: 'd1s3', hods: 'ems'},
            { apiStatus: 'ALPHA', index: 32, domain: 'd1', subdomain: 'd1s1', hods: 'internal,ems,invalid'},
            { apiStatus: 'BETA', index: 33, domain: 'd1', subdomain: 'd1s2', hods: 'apim'},
            { apiStatus: 'LIVE', index: 34, domain: 'd1', subdomain: 'd1s3', hods: 'invalid'}
        ]);
    });

    it("when multiple domain filters are applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickDomainFilter('d1');
        clickSubdomainFilter('d1s1');
        clickDomainFilter('d2');

        expect(getVisiblePanelData()).toEqual([
            { apiStatus: 'BETA', index: 1, domain: 'd1', subdomain: 'd1s2', hods: 'ems'},
            { apiStatus: 'LIVE', index: 2, domain: 'd1', subdomain: 'd1s3', hods: 'internal,ems,invalid'},
            { apiStatus: 'BETA', index: 9, domain: 'd1', subdomain: 'd1s2', hods: 'invalid'},
            { apiStatus: 'LIVE', index: 10, domain: 'd1', subdomain: 'd1s3', hods: ''},
            { apiStatus: 'BETA', index: 17, domain: 'd1', subdomain: 'd1s2', hods: 'internal,ems,invalid'},
            { apiStatus: 'LIVE', index: 18, domain: 'd1', subdomain: 'd1s3', hods: 'apim'},
            { apiStatus: 'BETA', index: 25, domain: 'd1', subdomain: 'd1s2', hods: ''},
            { apiStatus: 'LIVE', index: 26, domain: 'd1', subdomain: 'd1s3', hods: 'ems'},
            { apiStatus: 'BETA', index: 33, domain: 'd1', subdomain: 'd1s2', hods: 'apim'},
            { apiStatus: 'LIVE', index: 34, domain: 'd1', subdomain: 'd1s3', hods: 'invalid'},
            { apiStatus: 'BETA', index: 41, domain: 'd1', subdomain: 'd1s2', hods: 'ems'},
            { apiStatus: 'LIVE', index: 42, domain: 'd1', subdomain: 'd1s3', hods: 'internal,ems,invalid'},
            { apiStatus: 'BETA', index: 49, domain: 'd1', subdomain: 'd1s2', hods: 'invalid'},
            { apiStatus: 'LIVE', index: 50, domain: 'd1', subdomain: 'd1s3', hods: ''},
            { apiStatus: 'BETA', index: 57, domain: 'd1', subdomain: 'd1s2', hods: 'internal,ems,invalid'}
        ]);
    });

    it("when hod filters are applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickHodFilter('internal');
        clickHodFilter('ems');

        expect(getVisiblePanelData()).toEqual([
            { apiStatus: 'BETA', index: 1, domain: 'd1', subdomain: 'd1s2', hods: 'ems'},
            { apiStatus: 'LIVE', index: 2, domain: 'd1', subdomain: 'd1s3', hods: 'internal,ems,invalid'},
            { apiStatus: 'LIVE', index: 6, domain: 'dx', subdomain: 'dxs1', hods: 'ems'},
            { apiStatus: 'ALPHA', index: 12, domain: 'd3', subdomain: '', hods: 'internal,ems,invalid'},
            { apiStatus: 'ALPHA', index: 16, domain: 'd1', subdomain: 'd1s1', hods: 'ems'},
            { apiStatus: 'BETA', index: 17, domain: 'd1', subdomain: 'd1s2', hods: 'internal,ems,invalid'},
            { apiStatus: 'BETA', index: 21, domain: '', subdomain: 'd3s1', hods: 'ems'},
            { apiStatus: 'LIVE', index: 22, domain: 'dx', subdomain: 'dxs1', hods: 'internal,ems,invalid'},
            { apiStatus: 'LIVE', index: 26, domain: 'd1', subdomain: 'd1s3', hods: 'ems'},
            { apiStatus: 'ALPHA', index: 32, domain: 'd1', subdomain: 'd1s1', hods: 'internal,ems,invalid'},
            { apiStatus: 'ALPHA', index: 36, domain: 'd3', subdomain: '', hods: 'ems'},
            { apiStatus: 'BETA', index: 37, domain: '', subdomain: 'd3s1', hods: 'internal,ems,invalid'},
            { apiStatus: 'BETA', index: 41, domain: 'd1', subdomain: 'd1s2', hods: 'ems'},
            { apiStatus: 'LIVE', index: 42, domain: 'd1', subdomain: 'd1s3', hods: 'internal,ems,invalid'},
            { apiStatus: 'LIVE', index: 46, domain: 'dx', subdomain: 'dxs1', hods: 'ems'}
        ]);
    });

    describe("domain filter selection counter", () => {
        beforeEach(() => {
            buildApiPanelsByCount(100);
        });

        function getDomainFilterCount() {
            return parseInt(document.getElementById('domainFilterCount').textContent);
        }

        it("when page is first displayed then domain filter count is 0",  () => {
            onPageShow();

            expect(getDomainFilterCount()).toBe(0);
        });

        it("when domain filter with 3 subdomains is selected then domain filter count is 4",  () => {
            onPageShow();

            clickDomainFilter('d1');

            expect(getDomainFilterCount()).toBe(4);
        });

        it("when domain filter with 3 subdomains is deselected then domain filter count is 0",  () => {
            onPageShow();
            clickDomainFilter('d1');

            clickDomainFilter('d1');

            expect(getDomainFilterCount()).toBe(0);
        });

        it("when subdomain is deselected then domain filter count reduces by 1",  () => {
            onPageShow();
            clickDomainFilter('d1');

            clickSubdomainFilter('d1s2');

            expect(getDomainFilterCount()).toBe(3);
        });

        it("when all subdomains are deselected then domain filter count is 1",  () => {
            onPageShow();
            clickDomainFilter('d1');

            clickSubdomainFilter('d1s1');
            clickSubdomainFilter('d1s2');
            clickSubdomainFilter('d1s3');

            expect(getDomainFilterCount()).toBe(1);
        });
    });

    describe("HoD filter selection counter", () => {
        beforeEach(() => {
            buildApiPanelsByCount(100);
        });

        function getHodFilterCount() {
            return parseInt(document.getElementById('hodFilterCount').textContent);
        }

        it("when page is first displayed then hod filter count is 0",  () => {
            onPageShow();

            expect(getHodFilterCount()).toBe(0);
        });

        it("when hod filter with is selected then domain filter count is 1",  () => {
            onPageShow();

            clickHodFilter('apim');

            expect(getHodFilterCount()).toBe(1);
        });
    });

    describe("reset filters", () => {
        beforeEach(() => {
            buildApiPanelsByCount(100);
        });

        function clickResetFiltersLink() {
            document.getElementById('resetFilters').click();
        }

        it("when reset filters link is clicked then all status filters are reset",  () => {
            onPageShow();

            clickResetFiltersLink();

            expect(statusFilterIsSelected('ALPHA')).toBe(false);
            expect(statusFilterIsSelected('BETA')).toBe(false);
            expect(statusFilterIsSelected('LIVE')).toBe(false);
            expect(statusFilterIsSelected('DEPRECATED')).toBe(false);
        });

        it("when reset filters link is clicked then domain filters are reset",  () => {
            onPageShow();
            clickDomainFilter('d1');
            clickDomainFilter('d2');
            clickDomainFilter('d3');

            clickResetFiltersLink();

            expect(domainFilterIsSelected('d1')).toBe(false);
            expect(domainFilterIsSelected('d2')).toBe(false);
            expect(domainFilterIsSelected('d3')).toBe(false);
            expect(subdomainFilterIsSelected('d1s1')).toBe(false);
            expect(subdomainFilterIsSelected('d1s2')).toBe(false);
            expect(subdomainFilterIsSelected('d1s3')).toBe(false);
            expect(subdomainFilterIsSelected('d2s1')).toBe(false);
        });

        it("when reset filters link is clicked then domain filters section is collapsed",  () => {
            onPageShow();

            clickResetFiltersLink();

            expect(domainFiltersCollapsed()).toBe(true);
        });

        it("when reset filters link is clicked then hod filters are reset",  () => {
            onPageShow();
            clickHodFilter('apim');
            clickHodFilter('ems');

            clickResetFiltersLink();

            expect(hodFilterIsSelected('apim')).toBe(false);
            expect(hodFilterIsSelected('ems')).toBe(false);
        });

        it("when reset filters link is clicked then hod filters section is collapsed",  () => {
            onPageShow();

            clickResetFiltersLink();

            expect(hodFiltersCollapsed()).toBe(true);
        });

        it("when second page of results is displayed and reset filters link and is clicked then we return to the first page",  () => {
            onPageShow();
            clickPageNumber(2);

            clickResetFiltersLink();

            expect(getCurrentPageNumber()).toBe(1);
        });
    });

    describe("no results", () => {
        it("when no results match filters then 'no results' message is displayed",  () => {
            buildApiPanelsByCount(1);
            clickStatusFilter('ALPHA');

            onPageShow();

            expect(noResultsPanelIsVisible()).toBe(true);
        });

        it("when one result matches filters then 'no results' message is not displayed",  () => {
            buildApiPanelsByCount(1);

            onPageShow();

            expect(noResultsPanelIsVisible()).toBe(false);
        });
    });
});
