import {JSDOM} from 'jsdom';
import {buildStatusFilters} from "../../app/assets/javascripts/hipApisStatusFilters.js";

describe('hipApisStatusFilters', () => {
    let document, statusFilters;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="statusFilters">
                <input class="govuk-checkboxes__input" type="checkbox" value="ALPHA">
                <input class="govuk-checkboxes__input" type="checkbox" value="BETA">
                <input class="govuk-checkboxes__input" type="checkbox" value="LIVE">
                <input class="govuk-checkboxes__input" type="checkbox" value="DEPRECATED">                
            </div>
        `));
        document = dom.window.document;
        globalThis.document = document;

        statusFilters = buildStatusFilters();
    });

    it("after initialisation clicking a checkbox triggers the onChange handler",  () => {
        let changeCount = 0;
        statusFilters.onChange(() => changeCount++);

        const elCheckbox = document.querySelector('.govuk-checkboxes__input');

        elCheckbox.click();
        expect(changeCount).toBe(0);

        statusFilters.initialise();

        elCheckbox.click();
        expect(changeCount).toBe(1);
    });

    it("clear() unchecks all checkboxes",  () => {
        const elCheckboxes = document.querySelectorAll('.govuk-checkboxes__input');
        elCheckboxes.forEach(el => {
            el.checked = true;
        });

        statusFilters.clear();

        elCheckboxes.forEach(el => {
            expect(el.checked).toBe(false);
        });
    });

    describe('the filter function', () => {
        const data = [
            {apiStatus: "ALPHA"},
            {apiStatus: "BETA"},
            {apiStatus: "LIVE"},
            {apiStatus: "DEPRECATED"},
        ];

        it("returns true for all items if no checkboxes are selected",  () => {
            let filterFunction = statusFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("returns true for all items if all checkboxes are selected",  () => {
            document.querySelectorAll('.govuk-checkboxes__input').forEach(el => el.checked = true);
            let filterFunction = statusFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("returns true only for matching items if no checkboxes are selected",  () => {
            document.querySelector('.govuk-checkboxes__input[value="ALPHA"]').checked = true;
            document.querySelector('.govuk-checkboxes__input[value="LIVE"]').checked = true;

            let filterFunction = statusFilters.buildFilterFunction();
            expect(data.map(filterFunction)).toEqual([true, false, true, false]);
        });
    });

});
