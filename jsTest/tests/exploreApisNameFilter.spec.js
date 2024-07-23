import {JSDOM} from 'jsdom';
import {buildNameFilter} from "../../app/assets/javascripts/exploreApisNameFilter.js";

describe('exploreApisNameFilters', () => {
    let nameFilter, apis, elNameFilter;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <input id="nameFilter">
        `));
        globalThis.document = dom.window.document;
        globalThis.Event = dom.window.Event;
        elNameFilter = document.getElementById('nameFilter');

        apis = [
            {data: {apiName: 'api 1'}},
            {data: {apiName: 'api 2'}},
            {data: {apiName: '  api 3 '}},
            {data: {apiName: 'new api 4!'}},
            {data: {apiName: 'api 100'}},
        ];

        nameFilter = buildNameFilter();
    });

    function enterFilterText(value) {
        elNameFilter.value = value;
        elNameFilter.dispatchEvent(new Event('input'));
    }

    describe("initialise", () => {
        it("after initialisation entering a value triggers the onChange handler",  () => {
            let changeCount = 0;
            nameFilter.onChange(() => changeCount++);

            enterFilterText('api');
            expect(changeCount).toBe(0);

            nameFilter.initialise(apis);

            enterFilterText('api 1');
            expect(changeCount).toBe(1);
        });
    });

    describe("clear", () => {
        beforeEach(() => {
            nameFilter.initialise(apis);
        });

        it("empties the filter box",  () => {
            enterFilterText('api');

            nameFilter.clear();

            expect(elNameFilter.value).toBe('');
        });
    });

    describe("the filter function", () => {
        let data;
        beforeEach(() => {
            nameFilter.initialise(apis);
            data = apis.map(api => api.data);
        });

        it("returns true for all items if no filter text is entered",  () => {
            let filterFunction = nameFilter.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("returns true for all items if text matching all names is entered",  () => {
            enterFilterText('api');
            let filterFunction = nameFilter.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("ignores case when comparing filter value",  () => {
            enterFilterText('ApI');
            let filterFunction = nameFilter.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("only matches apis that match the filter value",  () => {
            enterFilterText('1');
            let filterFunction = nameFilter.buildFilterFunction();
            expect(data.map(d => ({match: filterFunction(d), d: d.apiName}))).toEqual([
                {match: true, d: 'api 1'},
                {match: false, d: 'api 2'},
                {match: false, d: '  api 3 '},
                {match: false, d: 'new api 4!'},
                {match: true, d: 'api 100'},
            ]);
        });
    });
});
