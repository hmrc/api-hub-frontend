import {JSDOM} from 'jsdom';
import {buildStatusFilters} from "../../app/assets/javascripts/exploreApisStatusFilters.js";
import {isVisible} from "./testUtils.js";

describe('exploreApisStatusFilters', () => {
    let document, statusFilters, apis;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="statusFilters">
                <div><input class="statusFilter" type="checkbox" value="ALPHA"></div>
                <div><input class="statusFilter" type="checkbox" value="BETA"></div>
                <div><input class="statusFilter" type="checkbox" value="LIVE"></div>
                <div><input class="statusFilter" type="checkbox" value="DEPRECATED"></div>                
            </div>
            <div id="statusFilterCount"></div>
            <details id="viewStatusFilters"><summary></summary></details>            
        `));
        document = dom.window.document;
        globalThis.document = document;

        statusFilters = buildStatusFilters();

        apis = buildApis('ALPHA', 'BETA', 'LIVE', 'DEPRECATED');
    });

    function buildApis(...statuses) {
        return statuses.map(o => ({data: {apiStatus: o}}));
    }

    function statusCheckbox(status) {
        return document.querySelector(`.statusFilter[value="${status}"]`);
    }

    describe("initialise", () => {
        it("removes checkboxes for statuses not in use by any APIs",  () => {
            statusFilters.initialise(buildApis('ALPHA', 'BETA'));
            expect([...document.querySelectorAll('.statusFilter')].filter(el => isVisible(el.parentElement)).map(el => el.value)).toEqual(['ALPHA', 'BETA']);
        });

        it("after initialisation clicking a checkbox triggers the onChange handler", () => {
            let changeCount = 0;
            statusFilters.onChange(() => changeCount++);

            const elCheckbox = document.querySelector('.statusFilter');

            elCheckbox.click();
            expect(changeCount).toBe(0);

            statusFilters.initialise(apis);

            elCheckbox.click();
            expect(changeCount).toBe(1);
        });

        it("if no statuses are selected then the status filter section is collapsed",  () => {
            statusFilters.initialise(apis);
            expect(document.getElementById('viewStatusFilters').open).toBe(false);
        });

        it("if statuses are selected then the status filter section is open",  () => {
            document.querySelector('[value="ALPHA"]').click();
            statusFilters.initialise(apis);
            expect(document.getElementById('viewStatusFilters').open).toBe(true);
        });

        it("if no statuses are selected then the status filter count is zero",  () => {
            statusFilters.initialise(apis);
            expect(document.getElementById('statusFilterCount').textContent).toBe('0');
        });

        it("if statuses are selected then the status filter count is the number of selected statuses",  () => {
            document.querySelector('[value="ALPHA"]').click();
            document.querySelector('[value="BETA"]').click();
            statusFilters.initialise(apis);
            expect(document.getElementById('statusFilterCount').textContent).toBe('2');
        });

    });

    describe('syncWithApis', () => {
        it("when new APIs are added, hidden checkboxes are shown",  () => {
            statusFilters.initialise(buildApis('ALPHA', 'BETA', 'LIVE'));
            expect(isVisible(statusCheckbox('DEPRECATED').parentElement)).toBe(false);

            statusFilters.syncWithApis([...apis, {data: {apiStatus: 'DEPRECATED'}}]);

            expect(isVisible(statusCheckbox('DEPRECATED').parentElement)).toBe(true);
        });

        it("when old APIs are removed, visible checkboxes are hidden",  () => {
            statusFilters.initialise(apis);
            expect(isVisible(statusCheckbox('ALPHA').parentElement)).toBe(true);

            statusFilters.syncWithApis(apis.filter(api => api.data.apiStatus !== 'ALPHA'));

            expect(isVisible(statusCheckbox('ALPHA').parentElement)).toBe(false);
        });

        it("when no APIs are present the filter is hidden",  () => {
            statusFilters.initialise(apis);

            expect(isVisible(document.getElementById('statusFilters'))).toBe(true);
            statusFilters.syncWithApis([]);

            expect(isVisible(document.getElementById('statusFilters'))).toBe(false);
        });
    });
    
    describe("clear", () => {
        beforeEach(() => {
            statusFilters.initialise(apis);
        });

        it("unchecks all checkboxes", () => {
            const elCheckboxes = document.querySelectorAll('.statusFilter');
            elCheckboxes.forEach(el => {
                el.checked = true;
            });

            statusFilters.clear();

            elCheckboxes.forEach(el => {
                expect(el.checked).toBe(false);
            });
        });

        it("collapses the status filter section",  () => {
            document.getElementById('viewStatusFilters').setAttribute('open', 'open');
            statusFilters.clear();
            expect(document.getElementById('viewStatusFilters').open).toBe(false);
        });

        it("sets the status filter count to zero",  () => {
            document.querySelector('[value="ALPHA"]').click();
            statusFilters.clear();
            expect(document.getElementById('statusFilterCount').textContent).toBe('0');
        });

    });

    describe('the filter function', () => {
        let data;
        beforeEach(() => {
            statusFilters.initialise(apis);
            data = apis.map(api => api.data);
        });

        it("returns true for all items if no checkboxes are selected",  () => {
            let filterFunction = statusFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("returns true for all items if all checkboxes are selected",  () => {
            document.querySelectorAll('.statusFilter').forEach(el => el.checked = true);
            let filterFunction = statusFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("returns true only for matching items if no checkboxes are selected",  () => {
            document.querySelector('.statusFilter[value="ALPHA"]').checked = true;
            document.querySelector('.statusFilter[value="LIVE"]').checked = true;

            let filterFunction = statusFilters.buildFilterFunction();
            expect(data.map(filterFunction)).toEqual([true, false, true, false]);
        });
    });

});
