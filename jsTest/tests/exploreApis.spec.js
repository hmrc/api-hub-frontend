import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/exploreApis.js';
import {paginationHelper, paginationContainerHtml, isVisible} from "./testUtils.js";

describe('exploreApis', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <input id="nameFilter">
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
            <div>
                <div><input class="platformFilter" type="checkbox" value="sdes"></div>
                <div><input class="platformFilter" type="checkbox" value="digi"></div>
            </div>
            <div id="apiResultsContainer" class="govuk-!-display-none">
                <div id="apiList"></div>
                <div id="searchResultsSize"></div>
                <div id="noResultsPanel"></div>
            </div>
            <div id="resetFilters"></div>
            <div id="noResultsClearFilters"></div>
            <div id="domainFilterCount"></div>
            <details id="viewDomainFilters"><summary></summary></details>
            <div id="hodFilterCount"></div>
            <details id="viewHodFilters"><summary></summary></details>
            <div id="statusFilterCount"></div>
            <details id="viewStatusFilters"><summary></summary></details>
            <input id="filterPlatformSelfServe" type="checkbox">
            <input id="filterPlatformNonSelfServe" type="checkbox">
            <details id="viewPlatformFilters"><summary></summary></details>
            ${paginationContainerHtml}
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
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
            hodsValues = ['', 'ems', 'internal,ems,invalid', 'apim', 'invalid'],
            platformValues = ['hip', 'sdes', 'digi'],
            names = [...Array(count)].map((_, i) => `api number ${i + 1}`);
        let i= 0;
        while (i < count) {
            const apistatus = statuses[i % statuses.length],
                [domain, subdomain] = domainValues[i % domainValues.length],
                hods = hodsValues[i % hodsValues.length],
                name = names[i],
                platform = platformValues[i % platformValues.length];
            panels.push({apistatus, domain, subdomain, hods, name, platform})
            i++;
        }
        buildApiPanels(...panels);
    }
    function buildApiPanels(...panels) {
        document.getElementById('apiList').innerHTML = panels.map((panel, i) => {
            return `<div class="api-panel" 
                data-apistatus="${panel.apistatus}" 
                data-domain="${panel.domain || ''}" 
                data-subdomain="${panel.subdomain || ''}" 
                data-index="${i}" 
                data-hods="${panel.hods || ''}" 
                data-platform="${panel.platform || ''}" 
                data-apiname="${panel.name}"></div>`;
        }).join('');
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
    function noResultsPanelIsVisible() {
        return isVisible(document.getElementById('noResultsPanel'));
    }
    function enterNameFilterText(value) {
        document.getElementById('nameFilter').value = value;
        document.getElementById('nameFilter').dispatchEvent(new Event('input'));
    }
    function getNameFilterText() {
        return document.getElementById('nameFilter').value;
    }
    function clickNonSelfServePlatformFilter(value) {
        if (value) {
            document.querySelector(`input[value="${value}"].platformFilter`).click();
        } else {
            document.querySelector('#filterPlatformNonSelfServe').click();
        }
    }

    it("when platform filter is applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickNonSelfServePlatformFilter();

        expect(new Set(paginationHelper.getVisiblePanelData('.api-panel', 'platform').map(({platform}) => platform))).toEqual(new Set(['sdes', 'digi']));

        clickNonSelfServePlatformFilter('sdes');
        expect(new Set(paginationHelper.getVisiblePanelData('.api-panel', 'platform').map(({platform}) => platform))).toEqual(new Set(['digi']));
    });

    it("when page initially displayed then only panels with selected statuses are visible",  () => {
        buildApiPanels({apistatus: 'ALPHA'}, {apistatus: 'BETA'}, {apistatus: 'LIVE'}, {apistatus: 'DEPRECATED'});

        onPageShow();

        expect(paginationHelper.getVisiblePanelData('.api-panel', 'apistatus')).toEqual([{apistatus: 'ALPHA', index: 0}, {apistatus: 'BETA', index: 1}, {apistatus: 'LIVE', index: 2}]);
        expect(getResultCount()).toBe(3);
    });

    it("api results are visible after onPageShow runs",  () => {
        const elApiResultsContainer = document.getElementById('apiResultsContainer');
        expect(isVisible(elApiResultsContainer)).toBe(false);

        onPageShow();

        expect(isVisible(elApiResultsContainer)).toBe(true);
    });

    it("when status is deselected then panels with that status are hidden",  () => {
        buildApiPanels({apistatus: 'ALPHA'}, {apistatus: 'BETA'}, {apistatus: 'LIVE'}, {apistatus: 'DEPRECATED'});

        onPageShow();
        clickStatusFilter('BETA');

        expect(paginationHelper.getVisiblePanelData('.api-panel', 'apistatus')).toEqual([{apistatus: 'ALPHA', index: 0}, {apistatus: 'LIVE', index: 2}]);
        expect(getResultCount()).toBe(2);
    });

    it("when status is selected then panels with that status are shown",  () => {
        buildApiPanels({apistatus: 'ALPHA'}, {apistatus: 'BETA'}, {apistatus: 'LIVE'}, {apistatus: 'DEPRECATED'});

        onPageShow();
        clickStatusFilter('DEPRECATED');

        expect(paginationHelper.getVisiblePanelData('.api-panel', 'apistatus')).toEqual([{apistatus: 'ALPHA', index: 0}, {apistatus: 'BETA', index: 1}, {apistatus: 'LIVE', index: 2}, {apistatus: 'DEPRECATED', index: 3}]);
        expect(getResultCount()).toBe(4);
    });

    it("only first page of results are displayed when page loads",  () => {
        buildApiPanelsByCount(100);

        onPageShow();

        expect(paginationHelper.getVisiblePanelIndexes('.api-panel')).toEqual(
            [0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, 16, 17, 18] // 'DEPRECATED' apis hidden by default
        );
    });

    it("when page navigation occurs then the correct panels are shown",  () => {
        buildApiPanelsByCount(100);

        onPageShow();
        paginationHelper.getPaginationPageLink(2).click();

        expect(paginationHelper.getVisiblePanelIndexes('.api-panel')).toEqual(
            [20, 21, 22, 24, 25, 26, 28, 29, 30, 32, 33, 34, 36, 37, 38] // 'DEPRECATED' apis hidden by default
        );
    });

    it("when filters change then page is reset to 1",  () => {
        buildApiPanelsByCount(100);

        onPageShow();
        paginationHelper.getPaginationPageLink(2).click();
        clickStatusFilter('DEPRECATED');

        expect(paginationHelper.getVisiblePanelIndexes('.api-panel')).toEqual(
            [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
        );
    });

    it("when domain filter is applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickDomainFilter('d1');

        expect(paginationHelper.getVisiblePanelData('.api-panel', 'domain', 'subdomain')).toEqual([
            { index: 0, domain: 'd1', subdomain: 'd1s1'},
            { index: 1, domain: 'd1', subdomain: 'd1s2'},
            { index: 2, domain: 'd1', subdomain: 'd1s3'},
            { index: 8, domain: 'd1', subdomain: 'd1s1'},
            { index: 9, domain: 'd1', subdomain: 'd1s2'},
            { index: 10, domain: 'd1', subdomain: 'd1s3'},
            { index: 16, domain: 'd1', subdomain: 'd1s1'},
            { index: 17, domain: 'd1', subdomain: 'd1s2'},
            { index: 18, domain: 'd1', subdomain: 'd1s3'},
            { index: 24, domain: 'd1', subdomain: 'd1s1'},
            { index: 25, domain: 'd1', subdomain: 'd1s2'},
            { index: 26, domain: 'd1', subdomain: 'd1s3'},
            { index: 32, domain: 'd1', subdomain: 'd1s1'},
            { index: 33, domain: 'd1', subdomain: 'd1s2'},
            { index: 34, domain: 'd1', subdomain: 'd1s3'}
        ]);
    });

    it("when multiple domain filters are applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickDomainFilter('d1');
        clickSubdomainFilter('d1s1');
        clickDomainFilter('d2');

        expect(paginationHelper.getVisiblePanelData('.api-panel', 'domain', 'subdomain')).toEqual([
            { index: 1, domain: 'd1', subdomain: 'd1s2' },
            { index: 2, domain: 'd1', subdomain: 'd1s3' },
            { index: 9, domain: 'd1', subdomain: 'd1s2' },
            { index: 10, domain: 'd1', subdomain: 'd1s3' },
            { index: 17, domain: 'd1', subdomain: 'd1s2' },
            { index: 18, domain: 'd1', subdomain: 'd1s3' },
            { index: 25, domain: 'd1', subdomain: 'd1s2' },
            { index: 26, domain: 'd1', subdomain: 'd1s3' },
            { index: 33, domain: 'd1', subdomain: 'd1s2' },
            { index: 34, domain: 'd1', subdomain: 'd1s3' },
            { index: 41, domain: 'd1', subdomain: 'd1s2' },
            { index: 42, domain: 'd1', subdomain: 'd1s3' },
            { index: 49, domain: 'd1', subdomain: 'd1s2' },
            { index: 50, domain: 'd1', subdomain: 'd1s3' },
            { index: 57, domain: 'd1', subdomain: 'd1s2' }
        ]);
    });

    it("when hod filters are applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        clickHodFilter('internal');
        clickHodFilter('ems');
        expect(paginationHelper.getVisiblePanelData('.api-panel','hods')).toEqual([
            { index: 1,  hods: 'ems'},
            { index: 2,  hods: 'internal,ems,invalid'},
            { index: 6,  hods: 'ems'},
            { index: 12, hods: 'internal,ems,invalid'},
            { index: 16, hods: 'ems'},
            { index: 17, hods: 'internal,ems,invalid'},
            { index: 21, hods: 'ems'},
            { index: 22, hods: 'internal,ems,invalid'},
            { index: 26, hods: 'ems'},
            { index: 32, hods: 'internal,ems,invalid'},
            { index: 36, hods: 'ems'},
            { index: 37, hods: 'internal,ems,invalid'},
            { index: 41, hods: 'ems'},
            { index: 42, hods: 'internal,ems,invalid'},
            { index: 46, hods: 'ems'}
        ]);
    });

    it("when name filter is applied then correct panels are shown",  () => {
        buildApiPanelsByCount(100);
        onPageShow();

        enterNameFilterText('api number 1');
        expect(paginationHelper.getVisiblePanelData('.api-panel', 'apiname')).toEqual([
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

        it("when reset filters link is clicked then name filter is cleared",  () => {
            onPageShow();
            enterNameFilterText('some api');

            clickResetFiltersLink();

            expect(getNameFilterText()).toBe('');
        });

        it("when second page of results is displayed and reset filters link and is clicked then we return to the first page",  () => {
            onPageShow();
            paginationHelper.getPaginationPageLink(2).click();

            clickResetFiltersLink();

            expect(paginationHelper.getCurrentPageNumber()).toBe(1);
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
