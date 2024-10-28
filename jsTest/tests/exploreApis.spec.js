import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/exploreApis.js';
import {paginationHelper, paginationContainerHtml, isVisible, waitFor, arrayFromTo} from "./testUtils.js";

describe('exploreApis', () => {
    let document, fetch;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <input id="filterPlatformSelfServe" type="checkbox">
            <input id="filterPlatformNonSelfServe" type="checkbox">
            <details id="viewPlatformFilters">
                <input class="platformFilter" value="sdes" type="checkbox">
                <input class="platformFilter" value="hip" type="checkbox" data-selfserve="true">
                <input class="platformFilter" value="digi" type="checkbox">
            </details>
            <fieldset id="domainFilters">
                <details id="viewDomainFilters">
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
                </details>
                <div id="domainFilterCount"></div>
            </fieldset>
            
            <fieldset id="hodFilters">
                <details id="viewHodFilters">
                    <div><input class="hodFilter" type="checkbox" value="ems"></div>
                    <div><input class="hodFilter" type="checkbox" value="internal"></div>
                    <div><input class="hodFilter" type="checkbox" value="apim"></div>
                </details>            
                <div id="hodFilterCount"></div>
            </fieldset>
            
<!--            <fieldset id="statusFilters">-->
<!--                <details id="viewStatusFilters">-->
<!--                    <div><input class="govuk-checkboxes__input statusFilter" type="checkbox" value="ALPHA"></div>-->
<!--                    <div><input class="govuk-checkboxes__input statusFilter" type="checkbox" value="BETA"></div>-->
<!--                    <div><input class="govuk-checkboxes__input statusFilter" type="checkbox" value="LIVE"></div>-->
<!--                    <div><input class="govuk-checkboxes__input statusFilter" type="checkbox" value="DEPRECATED"></div>                -->
<!--                </details>-->
<!--                <div id="statusFilterCount"></div>-->
<!--            </fieldset>-->
            
            <span id="apiResultsSize"></span>
            
            <form id="deepSearch">
                <input id="search">
                <button id="search_button"></button>
            </form>
            
            <div id="apiResults" class="govuk-!-display-none">
                <div id="searchResultsPanel" class="govuk-!-display-none">
                    <div id="resultsSuccess">
                        <span id="searchResultsShowing"></span>
                        <span id="searchResultsCount"></span>
                        <span id="searchResultsCountPlural">s</span>
                        <span id="searchResultsTerm"></span>
                    </div>
                    <div id="resultsError"></div>
                    <a id="clearSearch"></a>
                </div>
            
                <div id="filterResultsPanel">
                    <span id="filterResultsCount"></span>
                    <span id="filterResultsCountPlural"></span>
                    <span id="filterResultsHiddenCount"></span>
                    <span id="filterResultsSingleApi"></span>
                    <span id="filterResultsMultipleApis"></span>
                    <a id="clearFilters"></a>
                </div>
            
                <div id="apiList"></div>
            </div>
            ${paginationContainerHtml}
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function setSearchResults(apiIds) {
        fetch = globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.resolve({
            json: () => Promise.resolve(apiIds)
        }));
    }
    function setSearchError(reason) {
        fetch = globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.reject(reason));
    }

    function buildApiPanels(...panels) {
        document.getElementById('apiList').innerHTML = panels.map((panel, i) => {
            return `<div class="api-panel" 
                data-apistatus="${panel.apistatus}" 
                data-domain="${panel.domain || ''}" 
                data-subdomain="${panel.subdomain || ''}" 
                data-id="${i}" 
                data-hods="${panel.hods || ''}" 
                data-platform="${panel.platform || ''}"></div>`;
        }).join('');
    }

    function buildApiPanelsByCount(count) {
        const panels = [],
            statuses = ['ALPHA', 'BETA', 'LIVE', 'DEPRECATED'],
            domainValues = [
                ['d1', 'd1s1'],
                ['d1', 'd1s2'],
                ['d1', 'd1s3'],
                ['d1', ''],
                ['d2', 'd2s1'],
                ['d2', 'd2s2'],
                ['d2', 'd2s3'],
                ['d2', ''],
                ['d3', 'd3s1'],
                ['d3', 'd3s2'],
                ['d3', 'd3s3'],
                ['d3', ''],
            ],
            hodsValues = ['', 'ems', 'internal', 'apim', 'ems,internal,apim'],
            platformValues = ['hip', 'sdes', 'digi'];
        let i= 0;
        while (i < count) {
            const apistatus = statuses[i % statuses.length],
                [domain, subdomain] = domainValues[i % domainValues.length],
                hods = hodsValues[i % hodsValues.length],
                platform = platformValues[i % platformValues.length];
            panels.push({apistatus, domain, subdomain, hods, platform});
            i++;
        }
        buildApiPanels(...panels);
    }

    function getAttributeValuesForAllVisiblePanelsAsArray(attribute) {
        return paginationHelper.getVisiblePanelData('.api-panel', attribute).map(el => el[attribute]);
    }
    function getAttributeValuesForAllVisiblePanels(attribute) {
        return new Set(getAttributeValuesForAllVisiblePanelsAsArray(attribute));
    }

    function platformFilterSelfServe() {
        return document.getElementById('filterPlatformSelfServe');
    }
    function platformFilterNonSelfServe() {
        return document.getElementById('filterPlatformNonSelfServe');
    }
    function platformFilterNonSelfServeList() {
        return document.getElementById('viewPlatformFilters');
    }
    function platformCheckboxes() {
        return [...document.querySelectorAll('.platformFilter')];
    }

    function domainFilterView() {
        return document.getElementById('viewDomainFilters');
    }
    function domainFilterCount() {
        return document.getElementById('domainFilterCount');
    }
    function domainCheckboxes() {
        return [...document.querySelectorAll('.domainFilter')];
    }
    function subdomainCheckboxes() {
        return [...document.querySelectorAll('.subDomainFilter')];
    }
    function domainAndSubdomainCheckboxes() {
        return [...domainCheckboxes(), ...subdomainCheckboxes()];
    }

    function hodFilterView() {
        return document.getElementById('viewHodFilters');
    }
    function hodFilterCount() {
        return document.getElementById('hodFilterCount');
    }
    function hodCheckboxes() {
        return [...document.querySelectorAll('.hodFilter')];
    }

    function statusFilterView() {
        return document.getElementById('viewStatusFilters');
    }
    function statusFilterCount() {
        return document.getElementById('statusFilterCount');
    }
    function statusCheckboxes() {
        return [...document.querySelectorAll('.statusFilter')];
    }

    function searchBox() {
        return document.getElementById('search');
    }
    function clearFiltersLink() {
        return document.getElementById('clearFilters');
    }
    function filterResultsPanel() {
        return document.getElementById('filterResultsPanel');
    }
    function resultsError() {
        return document.getElementById('resultsError');
    }
    async function runSearch(searchTerm, expectError = false) {
        searchBox().value = searchTerm;
        document.getElementById('search_button').click();

        const elSearchTerm = document.getElementById('searchResultsTerm');
        if (expectError) {
            await waitFor(() => isVisible(resultsError()), true);
        } else {
            await waitFor(() => elSearchTerm.textContent, searchTerm);
        }
    }
    function apiResults() {
        return document.getElementById('apiResults');
    }
    function getApiResultSize() {
        return document.getElementById('apiResultsSize');
    }
    function searchResultsPanel() {
        return document.getElementById('searchResultsPanel');
    }
    function searchResultsCount() {
        return document.getElementById('searchResultsCount');
    }
    function searchResultsTerm() {
        return document.getElementById('searchResultsTerm');
    }
    function resultsSuccess() {
        return document.getElementById('resultsSuccess');
    }
    function clearSearchLink() {
        return document.getElementById('clearSearch');
    }
    function viewPlatformFilters() {
        return document.getElementById('viewPlatformFilters');
    }
    function domainFilters() {
        return document.getElementById('domainFilters');
    }
    function hodFilters() {
        return document.getElementById('hodFilters');
    }
    // function statusFilters() {
    //     return document.getElementById('statusFilters');
    // }

    describe("when page first loads", () => {
        describe("and no inputs have any values", () => {
            beforeEach(() => {
                buildApiPanelsByCount(100);
                onPageShow();
            });
            it("platform filter state is correct", () => {
                expect(platformFilterSelfServe().checked).toBe(false);
                expect(platformFilterNonSelfServe().checked).toBe(false);
                expect(platformFilterNonSelfServeList().open).toBe(false);
                expect(platformCheckboxes().every(isVisible)).toBe(true);
                expect(platformCheckboxes().every(el => !el.checked)).toBe(true);
            });
            it("domain filter state is correct", () => {
                expect(domainFilterView().open).toBe(false);
                expect(domainFilterCount().textContent).toBe('0');
                expect(domainCheckboxes().every(isVisible)).toBe(true);
                expect(subdomainCheckboxes().every(el => !isVisible(el))).toBe(false);
                expect(domainAndSubdomainCheckboxes().every(el => !el.checked)).toBe(true);
            });
            it("hod filter state is correct", () => {
                expect(hodFilterView().open).toBe(false);
                expect(hodFilterCount().textContent).toBe('0');
                expect(hodCheckboxes().every(isVisible)).toBe(true);
                expect(hodCheckboxes().every(el => !el.checked)).toBe(true);
            });
            xit("status filter state is correct", () => {
                expect(statusFilterView().open).toBe(false);
                expect(statusFilterCount().textContent).toBe('0');
                expect(statusCheckboxes().every(isVisible)).toBe(true);
                expect(statusCheckboxes().every(el => !el.checked)).toBe(true);
            });
            it("search field state is correct", () => {
                expect(searchBox().value).toBe('');
            });
        });

        describe("state is correct if input values exist for", () => {
            beforeEach(() => {
                buildApiPanelsByCount(100);
            });

            it("platform self-serve filter", () => {
                platformFilterSelfServe().click();
                onPageShow();

                expect(getAttributeValuesForAllVisiblePanels('platform')).toEqual(new Set(['hip']));
            });
            it("platform non-self-serve filter", () => {
                platformFilterNonSelfServe().click();
                onPageShow();

                expect(getAttributeValuesForAllVisiblePanels('platform')).toEqual(new Set(['sdes', 'digi']));
            });
            it("platform non-self-serve item filter", () => {
                platformFilterNonSelfServe().click();
                platformCheckboxes().filter(el => el.value === 'digi')[0].click();
                onPageShow();

                expect(getAttributeValuesForAllVisiblePanels('platform')).toEqual(new Set(['digi']));
            });
            it("domain filter", () => {
                domainCheckboxes().filter(el => el.value === 'd1')[0].click();
                subdomainCheckboxes().filter(el => el.value === 'd1s1')[0].click();
                onPageShow();

                expect(getAttributeValuesForAllVisiblePanels('domain')).toEqual(new Set(['d1']));
                expect(getAttributeValuesForAllVisiblePanels('subdomain')).toEqual(new Set(['d1s1']));
            });
            it("hod filter", () => {
                hodCheckboxes().filter(el => el.value === 'ems')[0].click();
                onPageShow();

                expect(getAttributeValuesForAllVisiblePanels('hods')).toEqual(new Set(['ems', 'ems,internal,apim']));
            });
            xit("status filter", () => {
                statusCheckboxes().filter(el => el.value === 'ALPHA')[0].click();
                onPageShow();

                expect(getAttributeValuesForAllVisiblePanels('apistatus')).toEqual(new Set(['ALPHA']));
            });
            it("search field, and a search request is sent to the server", async () => {
                const searchTerm = 'abc',
                    searchResults = ['42','23','56'];
                setSearchResults(searchResults);
                searchBox().value = searchTerm;
                onPageShow();

                expect(fetch).toHaveBeenCalledWith(`apis/deep-search/${searchTerm}`);
                await waitFor(() => getAttributeValuesForAllVisiblePanelsAsArray('id').join(), ['42','23','56'].join(','));
            });
        });

        it("then panels are displayed in the default order", () => {
            buildApiPanelsByCount(100);
            onPageShow();
            expect(getAttributeValuesForAllVisiblePanelsAsArray('id')).toEqual(arrayFromTo(0, 14).map(i => i.toString()));
        });

        it("then only the first 15 results are displayed", () => {
            buildApiPanelsByCount(100);
            onPageShow();
            expect(getAttributeValuesForAllVisiblePanelsAsArray('id').length).toEqual(15);
        });

    });

    describe("pagination", () => {
        it("when 15 results are displayed then the pagination is not shown", () => {
            buildApiPanelsByCount(15);
            onPageShow();

            expect(paginationHelper.paginationIsAvailable()).toBe(false);
        });
        it("when 16 results are displayed then the pagination is shown", () => {
            buildApiPanelsByCount(16);
            onPageShow();

            expect(paginationHelper.paginationIsAvailable()).toBe(true);
        });
        it("when the next page is clicked then the results are updated correctly", () => {
            buildApiPanelsByCount(100);
            onPageShow();
            paginationHelper.clickNext();

            expect(paginationHelper.getCurrentPageNumber()).toBe(2);
            expect(getAttributeValuesForAllVisiblePanelsAsArray('id')).toEqual(arrayFromTo(15, 29).map(i => i.toString()));
        });
        it("when a filter is applied then the pagination is reset", () => {
            buildApiPanelsByCount(100);
            onPageShow();

            paginationHelper.clickNext();
            expect(paginationHelper.getCurrentPageNumber()).toBe(2);

            platformFilterSelfServe().click();
            expect(paginationHelper.getCurrentPageNumber()).toBe(1);
        });
    });

    describe("filtering", () => {
        beforeEach(() => {
            buildApiPanelsByCount(100);
            onPageShow();
        });

        it("when a platform filter is applied then the results are filtered correctly", () => {
            platformFilterSelfServe().click();
            expect(getAttributeValuesForAllVisiblePanels('platform')).toEqual(new Set(['hip']));
        });
        it("when a domain filter is applied then the results are filtered correctly", () => {
            domainCheckboxes().filter(el => el.value === 'd1')[0].click();
            expect(getAttributeValuesForAllVisiblePanels('domain')).toEqual(new Set(['d1']));
        });
        it("when a subdomain filter is applied then the results are filtered correctly", () => {
            domainCheckboxes().filter(el => el.value === 'd1')[0].click();
            subdomainCheckboxes().filter(el => el.value === 'd1s1')[0].click(); // deselecting d1s1
            expect(getAttributeValuesForAllVisiblePanels('subdomain')).toEqual(new Set(['d1s2', 'd1s3']));
        });
        it("when a hod filter is applied then the results are filtered correctly", () => {
            hodCheckboxes().filter(el => el.value === 'ems')[0].click();
            expect(getAttributeValuesForAllVisiblePanels('hods')).toEqual(new Set(['ems', 'ems,internal,apim']));
        });
        xit("when a status filter is applied then the results are filtered correctly", () => {
            statusCheckboxes().filter(el => el.value === 'ALPHA')[0].click();
            expect(getAttributeValuesForAllVisiblePanels('apistatus')).toEqual(new Set(['ALPHA']));
        });
        it("when multiple filters are applied then the results are filtered correctly", () => {
            platformFilterSelfServe().click();
            domainCheckboxes().filter(el => el.value === 'd1')[0].click();
            // statusCheckboxes().filter(el => el.value === 'ALPHA')[0].click();

            expect(getAttributeValuesForAllVisiblePanels('platform')).toEqual(new Set(['hip']));
            expect(getAttributeValuesForAllVisiblePanels('domain')).toEqual(new Set(['d1']));
            // expect(getAttributeValuesForAllVisiblePanels('apistatus')).toEqual(new Set(['ALPHA']));
        });
        it("when the filters are cleared then the results are updated correctly", () => {
            platformFilterSelfServe().click();
            clearFiltersLink().click();

            expect(getAttributeValuesForAllVisiblePanelsAsArray('id')).toEqual(arrayFromTo(0, 14).map(i => i.toString()));
        });
        it("when a filter is applied then the filter result panel is displayed", () => {
            expect(isVisible(filterResultsPanel())).toBe(false);
            platformFilterSelfServe().click();

            expect(isVisible(filterResultsPanel())).toBe(true);
        });
        it("when the filters are cleared then the filter result panel is hidden", () => {
            platformFilterSelfServe().click();
            expect(isVisible(filterResultsPanel())).toBe(true);

            clearFiltersLink().click();
            expect(isVisible(filterResultsPanel())).toBe(false);
        });
    });

    describe("when a search is performed", () => {
        beforeEach(() => {
            buildApiPanelsByCount(100);
            onPageShow();
            setSearchResults(['1', '2', '3']);
        });
        it("the filters should be disabled", () => {
            runSearch('abc');
            expect(platformCheckboxes().every(el => el.disabled)).toBe(true);
            expect(domainAndSubdomainCheckboxes().every(el => el.disabled)).toBe(true);
            expect(hodCheckboxes().every(el => el.disabled)).toBe(true);
            // expect(statusCheckboxes().every(el => el.disabled)).toBe(true);
        });
        it("the api results should be cleared", () => {
            runSearch('abc');
            expect(isVisible(apiResults())).toBe(false);
        });
        it("the result count should be removed", () => {
            runSearch('abc');
            expect(isVisible(getApiResultSize())).toBe(false);
        });
        it("the correct request is sent to the server", async () => {
            await runSearch('abc');
            expect(fetch).toHaveBeenCalledWith('apis/deep-search/abc');
        });
        it("special characters in the search term are encoded", async () => {
            await runSearch('"abc & def"');
            expect(fetch).toHaveBeenCalledWith('apis/deep-search/%22abc%20%26%20def%22');
        });
        it("and the search term matches the search currently displayed then a new request is not sent to the server", async () => {
            await runSearch('abc');
            fetch.calls.reset();
            await runSearch('abc');
            expect(fetch).not.toHaveBeenCalled();
        });
        it("and the search box is empty then a request is not sent to the server", async () => {
            await runSearch('');
            expect(fetch).not.toHaveBeenCalled();
        });
        it("and the search term is different to the search currently displayed then a request is sent to the server", async () => {
            await runSearch('abc');
            fetch.calls.reset();
            await runSearch('def');
            expect(fetch).toHaveBeenCalledWith('apis/deep-search/def');
        });

        describe("and the results have returned", () => {
            beforeEach(async () => {
                platformFilterNonSelfServe().click();
                domainCheckboxes().forEach(el => el.checked = true);
                hodCheckboxes().forEach(el => el.checked = true);
                // statusCheckboxes().forEach(el => el.checked = true);
            });
            async function search(searchResults = ['2', '1']) {
                setSearchResults(searchResults);
                await runSearch('abc');
            }
            it("the filters should be re-enabled", async () => {
                await search();

                expect(platformCheckboxes().every(el => !el.disabled)).toBe(true);
                expect(domainAndSubdomainCheckboxes().every(el => !el.disabled)).toBe(true);
                expect(hodCheckboxes().every(el => !el.disabled)).toBe(true);
                // expect(statusCheckboxes().every(el => !el.disabled)).toBe(true);
            });
            it("the filters should be cleared", async () => {
                await search();
                expect(platformFilterNonSelfServe().checked).toBe(false);
                expect(domainAndSubdomainCheckboxes().every(el => !el.checked)).toBe(true);
                expect(hodCheckboxes().every(el => !el.checked)).toBe(true);
                // expect(statusCheckboxes().every(el => !el.checked)).toBe(true);
            });
            it("the filter should be repopulated so that they match only the apis returned by the search", async () => {
                await search();
                expect(new Set(platformCheckboxes().filter(el => isVisible(el.parentElement) && !el.dataset.selfserve).map(el => el.value))).toEqual(new Set(['sdes', 'digi']));
                expect(new Set(domainAndSubdomainCheckboxes().filter(el => isVisible(el.parentElement)).map(el => el.value))).toEqual(new Set(['d1', 'd1s2', 'd1s3']));
                expect(new Set(hodCheckboxes().filter(el => isVisible(el.parentElement)).map(el => el.value))).toEqual(new Set(['ems', 'internal']));
                // expect(new Set(statusCheckboxes().filter(el => isVisible(el.parentElement)).map(el => el.value))).toEqual(new Set(['BETA', 'LIVE']));
            });
            it("only apis that matched the search are displayed, in the correct order", async () => {
                const searchResults = ['99', '6', '11', '80', '42', '43', '10', '65', '77', '33', '30', '1', '2', '3', '50', '51', '52'];
                await search(searchResults);
                expect(getAttributeValuesForAllVisiblePanelsAsArray('id')).toEqual(searchResults.slice(0, 15));
                paginationHelper.clickNext();
                expect(getAttributeValuesForAllVisiblePanelsAsArray('id')).toEqual(searchResults.slice(15, 17));
            });
            it("the result count should be updated", async () => {
                await search();
                expect(getApiResultSize().textContent).toBe('(2)');
            });
            it("the search results panel should be shown, populated with the search term and result count", async () => {
                await search();
                expect(isVisible(resultsSuccess())).toBe(true);
                expect(isVisible(resultsError())).toBe(false);
                expect(isVisible(searchResultsPanel())).toBe(true);
                expect(searchResultsCount().textContent).toBe('2');
                expect(searchResultsTerm().textContent).toBe('abc');
            });

        });
        describe("and the search did not match any apis", () => {
            beforeEach(async () => {
                setSearchResults([]);
                await runSearch('no matches');
            });
            it("all filters are hidden", () => {
                expect(isVisible(viewPlatformFilters())).toBe(false);
                expect(isVisible(domainFilters())).toBe(false);
                expect(isVisible(hodFilters())).toBe(false);
                // expect(isVisible(statusFilters())).toBe(false);
            });
            it("the result count is zero", () => {
                expect(getApiResultSize().textContent).toBe('(0)');
            });
            it("the search results panel is shown with the correct message", () => {
                expect(isVisible(resultsSuccess())).toBe(true);
                expect(isVisible(resultsError())).toBe(false);
                expect(isVisible(searchResultsPanel())).toBe(true);
                expect(searchResultsCount().textContent).toBe('0');
                expect(searchResultsTerm().textContent).toBe('no matches');
            });
        });

        describe("and the search fails", () => {
            beforeEach(async () => {
                setSearchError('error');
                await runSearch('abc', true);
            });
            it("all filters are hidden", () => {
                expect(isVisible(viewPlatformFilters())).toBe(false);
                expect(isVisible(domainFilters())).toBe(false);
                expect(isVisible(hodFilters())).toBe(false);
                // expect(isVisible(statusFilters())).toBe(false);
            });
            it("a result count of 0 is displayed", () => {
                expect(getApiResultSize().textContent).toBe('(0)');
            });
            it("no apis are shown in the results", () => {
                expect(getAttributeValuesForAllVisiblePanels('id')).toEqual(new Set());
            });
            it("an error message is displayed", () => {
                expect(isVisible(resultsSuccess())).toBe(false);
                expect(isVisible(resultsError())).toBe(true);
            });
        });
    });

    describe("when a search is cleared", () => {
        beforeEach(async () => {
            buildApiPanelsByCount(100);
            onPageShow();
            setSearchResults(['50', '9', '27', '13']);
            await runSearch('abc');
            clearSearchLink().click();
        });
        it("the search box is emptied", () => {
            expect(searchBox().value).toBe('');
        });
        it("the api results return to the default sort order", () => {
            expect(getAttributeValuesForAllVisiblePanelsAsArray('id')).toEqual(arrayFromTo(0, 14).map(i => i.toString()));
        });
        it("all filters are restored to their default state", () => {
            expect(platformCheckboxes().every(el => isVisible(el.parentElement))).toBe(true);
            expect(domainAndSubdomainCheckboxes().every(el => isVisible(el.parentElement))).toBe(true);
            expect(hodCheckboxes().every(el => isVisible(el.parentElement))).toBe(true);
            // expect(statusCheckboxes().every(el => isVisible(el.parentElement))).toBe(true);
        });
    });

});
