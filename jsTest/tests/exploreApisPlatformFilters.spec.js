import {JSDOM} from 'jsdom';
import {buildPlatformFilters} from "../../app/assets/javascripts/exploreApisPlatformFilters.js";
import {isVisible} from "./testUtils.js";

describe('exploreApisPlatformFilters', () => {
    let document, platformFilters, apis;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <input id="filterPlatformSelfServe" type="checkbox" checked="checked">
            <label for="filterPlatformSelfServe"><span data-count="0"></span></label>
            <input id="filterPlatformNonSelfServe" type="checkbox">
            <label for="filterPlatformNonSelfServe"><span data-count="0"></span></label>
            <details id="viewPlatformFilters">
                <div id="eisFilters"></div>
                <div>
                    <input class="platformFilter" type="checkbox" id="filter_HIP" value="hip" data-selfserve="true">
                    <label for="filter_HIP"><span data-count></span></label>
                </div>
                <div>
                    <input class="platformFilter" type="checkbox" id="filter_API_PLATFORM" value="api_platform">
                    <label for="filter_API_PLATFORM"><span data-count></span></label>
                </div>
                <div>
                    <input class="platformFilter" type="checkbox" id="filter_SDES" value="sdes">
                    <label for="filter_SDES"><span data-count></span></label>
                </div>
                <div>
                    <input class="platformFilter" type="checkbox" id="filter_CMA" value="cma">
                    <label for="filter_CMA"><span data-count></span></label>
                </div>
                <div id="nonEISFilters"></div>
                <div>
                    <input class="platformFilter" type="checkbox" id="filter_CIP" value="cip">
                    <label for="filter_CIP"><span data-count></span></label>
                </div>
            </details>
        `);
        document = dom.window.document;
        globalThis.document = document;

        apis = [
            {data: {}}
        ];

        platformFilters = buildPlatformFilters();
    });

    function getNonSelfServeCheckboxValues() {
        return [...document.querySelectorAll('.platformFilter')].filter(el => isVisible(el.parentElement)).map(el => el.value);
    }
    function selfServeApiCheckbox() {
        return document.getElementById('filterPlatformSelfServe');
    }
    function nonSelfServeApiCheckbox() {
        return document.getElementById('filterPlatformNonSelfServe');
    }
    function platformCheckbox(platform) {
        return document.querySelector(`input[value="${platform}"]`);
    }
    function buildApisWithPlatforms(...platforms) {
        return platforms.map((platform, index) => ({data: {platform, index}}));
    }
    function listOfPlatformFiltersIsVisible() {
        return document.getElementById('viewPlatformFilters').open;
    }
    function eisFiltersTitle() {
        return document.getElementById('eisFilters');
    }
    function nonEISFiltersTitle() {
        return document.getElementById('nonEISFilters');
    }
    function filterCount(elementId){
        return document.querySelector(`[for=${elementId}]`).querySelector('[data-count]').dataset.count;
    }
    function selfServeCount(){
        return filterCount("filterPlatformSelfServe");
    }
    function nonSelfServeCount(){
        return filterCount("filterPlatformNonSelfServe");
    }
    function platformCount(platform){
        return filterCount(`filter_${platform.toUpperCase()}`);
    }

    describe("initialise", () => {
        it("removes checkboxes for platforms not in use by any APIs, and for self-serve APIs",  () => {
            expect(getNonSelfServeCheckboxValues()).toEqual(['hip', 'api_platform', 'sdes', 'cma', 'cip']);

            const apis = buildApisWithPlatforms('sdes', 'sdes', 'cma', 'hip', 'cip');
            platformFilters.initialise(apis);

            expect(getNonSelfServeCheckboxValues()).toEqual(['sdes', 'cma', 'cip']);
        });

        it("after initialisation clicking any of the checkboxes triggers the onChange handler",  () => {
            let changeCount = 0;
            platformFilters.onChange(() => changeCount++);

            selfServeApiCheckbox().click();
            nonSelfServeApiCheckbox().click();
            platformCheckbox('hip').click();
            platformCheckbox('api_platform').click();
            platformCheckbox('sdes').click();
            platformCheckbox('cma').click();

            expect(changeCount).toBe(0);

            const apis = buildApisWithPlatforms('sdes', 'sdes', 'cma', 'hip');
            platformFilters.initialise(apis);

            selfServeApiCheckbox().click();
            expect(changeCount).toBe(1);

            nonSelfServeApiCheckbox().click();
            expect(changeCount).toBe(2);

            platformCheckbox('sdes').click();
            expect(changeCount).toBe(3);

            platformCheckbox('cma').click();
            expect(changeCount).toBe(4);
        });

        it("the list of platform filters is toggled on and off by the correct checkbox",  () => {
            platformFilters.initialise(buildApisWithPlatforms('sdes', 'sdes', 'cma', 'hip'));

            expect(listOfPlatformFiltersIsVisible()).toBe(false);

            nonSelfServeApiCheckbox().click();
            expect(listOfPlatformFiltersIsVisible()).toBe(true);

            nonSelfServeApiCheckbox().click();
            expect(listOfPlatformFiltersIsVisible()).toBe(false);
        });

        it("the platform filters are all checked each time the list is displayed",  () => {
            platformFilters.initialise(buildApisWithPlatforms('sdes', 'api_platform', 'cma', 'hip'));

            nonSelfServeApiCheckbox().click();
            expect(platformCheckbox('sdes').checked).toBe(true);
            expect(platformCheckbox('api_platform').checked).toBe(true);
            expect(platformCheckbox('cma').checked).toBe(true);

            platformCheckbox('api_platform').click();
            expect(platformCheckbox('api_platform').checked).toBe(false);

            nonSelfServeApiCheckbox().click();
            nonSelfServeApiCheckbox().click();
            expect(platformCheckbox('api_platform').checked).toBe(true);
        });
    });

    describe('syncWithApis', () => {
        let apis;
        beforeEach(() => {
            apis = buildApisWithPlatforms('sdes', 'sdes', 'hip', 'cip');
        });

        it("when new APIs are added, hidden checkboxes are shown",  () => {
            platformFilters.initialise(apis);
            expect(isVisible(platformCheckbox('cma').parentElement)).toBe(false);

            platformFilters.syncWithApis([...apis, {data: {platform: 'cma', isSelfServe: 'true'}}]);

            expect(isVisible(platformCheckbox('cma').parentElement)).toBe(true);
            expect(selfServeCount()).toEqual('1');
            expect(nonSelfServeCount()).toEqual('4');
            expect(platformCount('sdes')).toEqual('2');
            expect(platformCount('cma')).toEqual('1');
            expect(platformCount('cip')).toEqual('1');
        });

        it("when there is only EIS managed APIs, only EIS checkboxes are shown",  () => {
            platformFilters.initialise(apis);
            expect(isVisible(platformCheckbox('cma').parentElement)).toBe(false);

            platformFilters.syncWithApis([{data: {platform: 'cma', isEISManaged: true}}]);

            expect(isVisible(platformCheckbox('cma').parentElement)).toBe(true);
            expect(isVisible(eisFiltersTitle())).toBe(true);
            expect(isVisible(nonEISFiltersTitle())).toBe(false);
            expect(selfServeCount()).toEqual('0');
            expect(nonSelfServeCount()).toEqual('1');
            expect(platformCount('cma')).toEqual('1');
            expect(platformCount('sdes')).toEqual('0');
            expect(platformCount('cip')).toEqual('0');
        });

        it("when there is only non EIS managed APIs, only non EIS checkboxes are shown",  () => {
            platformFilters.initialise(apis);
            expect(isVisible(platformCheckbox('cma').parentElement)).toBe(false);

            platformFilters.syncWithApis([{data: {platform: 'cma'}}]);

            expect(isVisible(platformCheckbox('cma').parentElement)).toBe(true);
            expect(isVisible(eisFiltersTitle())).toBe(false);
            expect(isVisible(nonEISFiltersTitle())).toBe(true);
            expect(selfServeCount()).toEqual('0');
            expect(nonSelfServeCount()).toEqual('1');
            expect(platformCount('cma')).toEqual('1');
            expect(platformCount('sdes')).toEqual('0');
            expect(platformCount('cip')).toEqual('0');
        });

        it("when old APIs are removed, visible checkboxes are hidden",  () => {
            platformFilters.initialise(apis);
            expect(isVisible(platformCheckbox('sdes').parentElement)).toBe(true);

            platformFilters.syncWithApis(apis.filter(api => api.data.platform !== 'sdes'));

            expect(isVisible(platformCheckbox('sdes').parentElement)).toBe(false);
        });

        it("when no APIs are present the filter is disabled",  () => {
            platformFilters.initialise(apis);

            expect(document.getElementById('filterPlatformSelfServe').disabled).toBe(false);
            expect(document.getElementById('filterPlatformNonSelfServe').disabled).toBe(false);

            platformFilters.syncWithApis([]);

            expect(document.getElementById('filterPlatformSelfServe').disabled).toBe(true);
            expect(document.getElementById('filterPlatformNonSelfServe').disabled).toBe(true);
        });
    });
    
    describe("clear", () => {
        beforeEach(() => {
            platformFilters.initialise(buildApisWithPlatforms('sdes', 'api_platform', 'cma', 'hip'));
        });

        it("resets top-level checkboxes",  () => {
            expect(selfServeApiCheckbox().checked).toBe(true);
            expect(nonSelfServeApiCheckbox().checked).toBe(false);

            platformFilters.clear();

            expect(selfServeApiCheckbox().checked).toBe(false);
            expect(nonSelfServeApiCheckbox().checked).toBe(false);

            selfServeApiCheckbox().click();
            nonSelfServeApiCheckbox().click();

            expect(selfServeApiCheckbox().checked).toBe(true);
            expect(nonSelfServeApiCheckbox().checked).toBe(true);

            platformFilters.clear();

            expect(selfServeApiCheckbox().checked).toBe(false);
            expect(nonSelfServeApiCheckbox().checked).toBe(false);
        });

        it("collapses non-self-serve filter list",  () => {
            expect(listOfPlatformFiltersIsVisible()).toBe(false);

            platformFilters.clear();

            expect(listOfPlatformFiltersIsVisible()).toBe(false);

            nonSelfServeApiCheckbox().click();
            expect(listOfPlatformFiltersIsVisible()).toBe(true);

            platformFilters.clear();

            expect(listOfPlatformFiltersIsVisible()).toBe(false);
        });
    });

    describe("the filter function", () => {
        let data;

        beforeEach(() => {
            const apis = buildApisWithPlatforms('sdes', 'api_platform', 'cma', 'hip', 'sdes', 'api_platform', 'cma', 'hip');
            platformFilters.initialise(apis);
            data = apis.map(api => api.data);
        });

        it("if self-serve/non-self-serve and all individual platform filters are checked then all apis are shown",  () => {
            nonSelfServeApiCheckbox().click();

            expect(selfServeApiCheckbox().checked).toBe(true);
            expect(nonSelfServeApiCheckbox().checked).toBe(true);
            expect(platformCheckbox('api_platform').checked).toBe(true);
            expect(platformCheckbox('sdes').checked).toBe(true);
            expect(platformCheckbox('cma').checked).toBe(true);

            let filterFunction = platformFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("if neither self-serve/non-self-serve are checked then all apis are shown",  () => {
            selfServeApiCheckbox().click();

            expect(selfServeApiCheckbox().checked).toBe(false);
            expect(nonSelfServeApiCheckbox().checked).toBe(false);
            expect(listOfPlatformFiltersIsVisible()).toBe(false);

            let filterFunction = platformFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("if self-serve/non-self-serve are both checked and none of the platform filters are checked then all apis are shown",  () => {
            nonSelfServeApiCheckbox().click();
            platformCheckbox('api_platform').click();
            platformCheckbox('sdes').click();
            platformCheckbox('cma').click();

            expect(selfServeApiCheckbox().checked).toBe(true);
            expect(nonSelfServeApiCheckbox().checked).toBe(true);
            expect(platformCheckbox('api_platform').checked).toBe(false);
            expect(platformCheckbox('sdes').checked).toBe(false);
            expect(platformCheckbox('cma').checked).toBe(false);

            let filterFunction = platformFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("if self-serve only is checked then only self-serve apis are shown",  () => {
            expect(selfServeApiCheckbox().checked).toBe(true);
            expect(nonSelfServeApiCheckbox().checked).toBe(false);
            expect(listOfPlatformFiltersIsVisible()).toBe(false);

            let filterFunction = platformFilters.buildFilterFunction();
            expect(data.filter(d => d.platform === 'hip').every(filterFunction)).toBe(true);
            expect(data.filter(d => d.platform !== 'hip').every(filterFunction)).toBe(false);
        });

        it("if non-self-serve and all platform filters are checked then only non-self-serve apis are shown",  () => {
            selfServeApiCheckbox().click();
            nonSelfServeApiCheckbox().click();

            expect(selfServeApiCheckbox().checked).toBe(false);
            expect(nonSelfServeApiCheckbox().checked).toBe(true);
            expect(listOfPlatformFiltersIsVisible()).toBe(true);

            let filterFunction = platformFilters.buildFilterFunction();
            expect(data.filter(d => d.platform === 'hip').every(filterFunction)).toBe(false);
            expect(data.filter(d => d.platform !== 'hip').every(filterFunction)).toBe(true);
        });

        it("if not all platform filters are checked then only matching apis are shown",  () => {
            function dataForPlatform(platform) {
                return data.filter(d => d.platform === platform);
            }
            selfServeApiCheckbox().click();
            nonSelfServeApiCheckbox().click();
            platformCheckbox('api_platform').click();

            expect(selfServeApiCheckbox().checked).toBe(false);
            expect(nonSelfServeApiCheckbox().checked).toBe(true);
            expect(platformCheckbox('sdes').checked).toBe(true);
            expect(platformCheckbox('api_platform').checked).toBe(false);
            expect(platformCheckbox('cma').checked).toBe(true);

            let filterFunction;

            filterFunction = platformFilters.buildFilterFunction();
            expect(dataForPlatform('hip').every(filterFunction)).toBe(false);
            expect(dataForPlatform('sdes').every(filterFunction)).toBe(true);
            expect(dataForPlatform('api_platform').every(filterFunction)).toBe(false);
            expect(dataForPlatform('cms').every(filterFunction)).toBe(true);

            platformCheckbox('sdes').click();

            filterFunction = platformFilters.buildFilterFunction();
            expect(dataForPlatform('hip').every(filterFunction)).toBe(false);
            expect(dataForPlatform('sdes').every(filterFunction)).toBe(false);
            expect(dataForPlatform('api_platform').every(filterFunction)).toBe(false);
            expect(dataForPlatform('cms').every(filterFunction)).toBe(true);
        });
    });
});
