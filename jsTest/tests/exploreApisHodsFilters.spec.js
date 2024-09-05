import {JSDOM} from 'jsdom';
import {buildHodsFilters} from "../../app/assets/javascripts/exploreApisHodsFilters.js";
import {isVisible} from "./testUtils.js";

describe('exploreApisHodsFilters', () => {
    let document, hodsFilters, apis;

    function buildApis(...hods) {
        return hods.map(o => ({data: {hods: new Set(o.split(',').filter(f=>f))}}));
    }

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="hodFilterCount"></div>
            <details id="viewHodFilters"><summary>View</summary></details>
            <div id="hodFilters">
                <div><input class="hodFilter" type="checkbox" value="h1"></div>
                <div><input class="hodFilter" type="checkbox" value="h2"></div>
                <div><input class="hodFilter" type="checkbox" value="h3"></div>
                <div><input class="hodFilter" type="checkbox" value="h4"></div>
            </div>
        `);
        document = dom.window.document;
        globalThis.document = document;

        apis = buildApis('h1', 'h2', 'h3', 'invalid', 'invalid,h1', '', 'h1,h2,h3', 'h1,h2');
        hodsFilters = buildHodsFilters();
    });

    function hodCheckbox(hod) {
        return document.querySelector(`.hodFilter[value="${hod}"]`);
    }

    describe("initialise", () => {
        it("removes checkboxes for hods not in use by any APIs",  () => {
            hodsFilters.initialise(apis);

            expect([...document.querySelectorAll('.hodFilter')].filter(el => isVisible(el.parentElement)).map(el => el.value)).toEqual(['h1', 'h2', 'h3']);
        });

        it("should hide the entire filter if no hods in use by any APIs",  () => {
            expect(isVisible(document.getElementById('hodFilters'))).toEqual(true);

            hodsFilters.initialise([{data: {hods: ''}}, {data: {hods: ''}}]);

            expect(isVisible(document.getElementById('hodFilters'))).toEqual(false);
        });

        it("should hide the entire filter if no APIs are present",  () => {
            expect(isVisible(document.getElementById('hodFilters'))).toEqual(true);

            hodsFilters.initialise([]);

            expect(isVisible(document.getElementById('hodFilters'))).toEqual(false);
        });

        it("after initialisation clicking a checkbox triggers the onChange handler",  () => {
            let changeCount = 0;
            hodsFilters.onChange(() => changeCount++);

            const elHodCheckbox = document.querySelector('.hodFilter');

            elHodCheckbox.click();
            expect(changeCount).toBe(0);

            hodsFilters.initialise(apis);

            elHodCheckbox.click();
            expect(changeCount).toBe(1);
        });

        it("if no hods are selected then the hods filter section is collapsed",  () => {
            hodsFilters.initialise(apis);
            expect(document.getElementById('viewHodFilters').open).toBe(false);
        });

        it("if hods are selected then the hod filter section is open",  () => {
            hodCheckbox('h1').click();
            hodsFilters.initialise(apis);
            expect(document.getElementById('viewHodFilters').open).toBe(true);
        });

        it("if no hods are selected then the hod filter count is zero",  () => {
            hodsFilters.initialise(apis);
            expect(document.getElementById('hodFilterCount').textContent).toBe('0');
        });

        it("if hods are selected then the hod filter count is the number of selected hods",  () => {
            hodCheckbox('h1').click();
            hodsFilters.initialise(apis);
            expect(document.getElementById('hodFilterCount').textContent).toBe('1');
        });
    });

    describe('syncWithApis', () => {
        it("when new APIs are added, hidden checkboxes are shown",  () => {
            hodsFilters.initialise(apis);
            expect(isVisible(hodCheckbox('h4').parentElement)).toBe(false);

            hodsFilters.syncWithApis([...apis, {data: {hods: new Set(['h4'])}}]);

            expect(isVisible(hodCheckbox('h4').parentElement)).toBe(true);
        });

        it("when old APIs are removed, visible checkboxes are hidden",  () => {
            hodsFilters.initialise(apis);
            expect(isVisible(hodCheckbox('h1').parentElement)).toBe(true);

            hodsFilters.syncWithApis(apis.filter(api => !api.data.hods.has('h1')));

            expect(isVisible(hodCheckbox('h1').parentElement)).toBe(false);
        });

        it("when no APIs are present the filter is hidden",  () => {
            hodsFilters.initialise(apis);

            expect(isVisible(document.getElementById('hodFilters'))).toBe(true);
            hodsFilters.syncWithApis([]);

            expect(isVisible(document.getElementById('hodFilters'))).toBe(false);
        });
    });
    
    describe("clear", () => {
        beforeEach(() => {
            hodsFilters.initialise(apis);
        });

        it("clears all hods checkboxes",  () => {
            hodCheckbox('h1').click();
            hodCheckbox('h2').click();
            hodCheckbox('h3').click();

            hodsFilters.clear();

            expect(document.querySelectorAll('.hodFilter:checked').length).toBe(0);
        });

        it("collapses the hod filter section",  () => {
            document.getElementById('viewHodFilters').setAttribute('open', 'open');
            hodsFilters.clear();
            expect(document.getElementById('viewHodFilters').open).toBe(false);
        });

        it("sets the hod filter count to zero",  () => {
            hodCheckbox('h1').click();
            hodsFilters.clear();
            expect(document.getElementById('hodFilterCount').textContent).toBe('0');
        });
    });

    describe("the filter function", () => {
        let data;
        beforeEach(() => {
            hodsFilters.initialise(apis);
            data = apis.map(api => api.data);
        });

        it("returns true for all items if no checkboxes are selected",  () => {
            let filterFunction = hodsFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("returns true for items with at least 1 valid hod if all checkboxes are selected",  () => {
            document.querySelectorAll('input').forEach(el => el.checked = true);
            let filterFunction = hodsFilters.buildFilterFunction();
            expect(data.map(filterFunction)).toEqual([true, true, true, false, true, false, true, true]);
        });

        it("returns true for items with matching hod if single selection is made",  () => {
            hodCheckbox('h1').click();
            let filterFunction = hodsFilters.buildFilterFunction();
            expect(data.map(filterFunction)).toEqual([true, false, false, false, true, false, true, true]);
        });

        it("returns true for items with matching hods if multiple selection is made",  () => {
            hodCheckbox('h1').click();
            hodCheckbox('h2').click();
            let filterFunction = hodsFilters.buildFilterFunction();
            expect(data.map(filterFunction)).toEqual([true, true, false, false, true, false, true, true]);
        });
    });
});
