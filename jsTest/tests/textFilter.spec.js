import {JSDOM} from "jsdom";
import {isVisible, paginationContainerHtml} from "./testUtils.js";
import {buildTextFilter, dataAttribute} from "../../app/assets/javascripts/textFilter.js";

describe('textFilter', () => {
    let document, elFilter, elItems;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="items"></div>
            <input id="filter" type="text">
            ${paginationContainerHtml}
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
        elFilter = document.getElementById('filter');
        elItems = document.getElementById('items');
    });

    function enterFilterText(value) {
        elFilter.value = value;
        elFilter.dispatchEvent(new Event('input'));
    }

    function buildItemElement(attrValues) {
        const el = document.createElement('div');
        el.className = 'item';
        Object.entries(attrValues).forEach(([key, value]) => el.setAttribute(`data-${key}`, `${value}`));
        return el;
    }

    describe('dataAttribute', () => {
        it("includesTheFilterText matches values correctly ", () => {
            const el = buildItemElement({name: 'apple'});
            [
                ['apple', true],
                ['app', true],
                ['pple', true],
                ['banana', false],
                ['Apple', false]
            ].forEach(([filterText, expected]) => {
                expect(dataAttribute('name').includesTheFilterText()(filterText, el)).toBe(expected);
            })
        });
        it("startsWithTheFilterText matches values correctly ", () => {
            const el = buildItemElement({name: 'apple'});
            [
                ['apple', true],
                ['app', true],
                ['pple', false],
                ['banana', false],
                ['Apple', false]
            ].forEach(([filterText, expected]) => {
                expect(dataAttribute('name').startsWithTheFilterText()(filterText, el)).toBe(expected);
            })
        });
        it("whenNormalised transforms values correctly ", () => {
            [
                ['applE', true],
                ['  APPles ', true],
                [' grEen AppLes ', true],
                ['banana', false],
                ['apple', true]
            ].forEach(([attributeValue, expected]) => {
                const el = buildItemElement({name: attributeValue});
                expect(dataAttribute('name').whenNormalised().includesTheFilterText()('apple', el)).toBe(expected);
            })
        });
        it("whenSplitBy transforms values correctly ", () => {
            [
                ['apple', 'apple@example.com', true],
                ['apple', 'apple@example.com,pear@example.com', true],
                ['pear', 'apple@example.com,pear@example.com', true],
                ['banana', 'apple@example.com,pear@example.com', false],
            ].forEach(([filterText, attributeValue, expected]) => {
                const el = buildItemElement({name: attributeValue});
                expect(dataAttribute('name').whenSplitBy(',').startsWithTheFilterText()(filterText, el)).toBe(expected);
            })
        });
    });

    describe('buildTextFilter', () => {
        let filter, itemElements;
        beforeEach(() => {
            itemElements = [
                buildItemElement({'name': 'apple', 'colour': 'red'}),
                buildItemElement({'name': 'banana', 'colour': 'yellow'}),
                buildItemElement({'name': 'pear', 'colour': 'green'}),
            ];
            itemElements.forEach(el => elItems.appendChild(el));

            filter = buildTextFilter(document.querySelectorAll('.item'), elFilter, [
                dataAttribute('name').includesTheFilterText(),
                dataAttribute('colour').startsWithTheFilterText()
            ]);
        });

        [
            ['apple', [true, false, false]],
            ['nope', [false, false, false]],
            ['a', [true, true, true]],
            ['yell', [false, true, false]],
            ['ell', [false, false, false]],
        ].forEach(([filterText, expected]) => {
            it(`filtering by '${filterText}' sets item visibility to [${expected}]`, () => {
                enterFilterText(filterText);
                expect(itemElements.map(isVisible)).toEqual(expected);
            })
        });

    });
})
