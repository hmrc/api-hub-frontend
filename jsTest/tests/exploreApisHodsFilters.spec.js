import {JSDOM} from 'jsdom';
import {buildHodsFilters} from "../../app/assets/javascripts/exploreApisHodsFilters.js";

describe('exploreApisHodsFilters', () => {
    let document, hodsFilters, apis;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="hodFilterCount"></div>
            <details id="viewHodFilters"><summary>View</summary></details>
            <div id="hodFilters">
                <div><input class="hodFilter" type="checkbox" value="h1"></div>
                <div><input class="hodFilter" type="checkbox" value="h2"></div>
                <div><input class="hodFilter" type="checkbox" value="h3"></div>
                <div><input class="hodFilter" type="checkbox" value="h4"></div>
            </div>
        `));
        document = dom.window.document;
        globalThis.document = document;

        apis = [
            {data: {hods: 'h1'}},
            {data: {hods: 'h2'}},
            {data: {hods: 'h3'}},
            {data: {hods: 'invalid'}},
            {data: {hods: 'invalid,h1'}},
            {data: {hods: ''}},
            {data: {hods: 'h1,h2,h3'}},
            {data: {hods: 'h1,h2'}},
        ].map(o => ({data: {hods: new Set(o.data.hods.split(',').filter(f=>f))}}));

        hodsFilters = buildHodsFilters();
    });

    function hodCheckbox(hod) {
        return document.querySelector(`.hodFilter[value="${hod}"]`);
    }

    describe("initialise", () => {
        it("removes checkboxes for hods not in use by any APIs",  () => {
            hodsFilters.initialise(apis);

            expect([...document.querySelectorAll('.hodFilter')].map(el => el.value)).toEqual(['h1', 'h2', 'h3']);
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
