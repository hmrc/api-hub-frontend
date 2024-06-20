import {buildPaginator} from '../../app/assets/javascripts/paginationController.js';
import {JSDOM} from 'jsdom';
import {isVisible} from "./testUtils.js";

describe('paginationController', () => {
    let elContainer, paginator, document, paginationData;

    beforeEach(() => {
        const dom = (new JSDOM('<!DOCTYPE html><div id="pg"></div>'))
        document = dom.window.document;
        elContainer = document.getElementById('pg');
        document.body.appendChild(elContainer);
        globalThis.document = document;
        paginator = buildPaginator(elContainer, 10);
        paginator.onPaginationChanged(data => {
            paginationData = data;
        });
    });

    function buildItems(count) {
        return Array.from({length: count}, (o, i) => ({index: i}));
    }
    function clickNext(){
        document.querySelector('.govuk-pagination__next ').click();
    }

    describe("isPaginating", () => {
        it("when less than 1 page of items exist, then the pagination view is not visible", () => {
            paginator.initialise(buildItems(9));
            expect(paginationData.isPaginating).toBe(false);
            expect(isVisible(elContainer)).toBe(false);
        });

        it("when exactly 1 page of items exists, then the pagination view is not visible", () => {
            paginator.initialise(buildItems(10));
            expect(paginationData.isPaginating).toBe(false);
            expect(isVisible(elContainer)).toBe(false);
        });

        it("when 1 more than 1 page of items exists, then the pagination view is not visible", () => {
            paginator.initialise(buildItems(11));
            expect(paginationData.isPaginating).toBe(true);
            expect(isVisible(elContainer)).toBe(true);
        });
    });

    describe("item count", () => {
        it("item counts are correct when there is only one page of results", () => {
            paginator.initialise(buildItems(7));
            expect(paginationData.visibleItemCount).toBe(7);
            expect(paginationData.totalItemCount).toBe(7);
        });
        it("item counts are correct when there are multiple pages and we navigate through each of them", () => {
            paginator.initialise(buildItems(32));

            expect(paginationData.visibleItemCount).toBe(10);
            expect(paginationData.totalItemCount).toBe(32);

            clickNext();
            expect(paginationData.visibleItemCount).toBe(10);
            expect(paginationData.totalItemCount).toBe(32);

            clickNext();
            expect(paginationData.visibleItemCount).toBe(10);
            expect(paginationData.totalItemCount).toBe(32);

            clickNext();
            expect(paginationData.visibleItemCount).toBe(2); // final page, this count is different
            expect(paginationData.totalItemCount).toBe(32);
        });
    });

    describe('hiddenByPagination property', () => {
        function getHiddenItems(items) {
            return items.filter(item => item.hiddenByPagination);
        }
        function buildHiddenItems(startIndex, endIndex) {
            return Array.from({length: endIndex - startIndex + 1}, (o, i) => ({index: i + startIndex, hiddenByPagination: true}));
        }

        it("when there is only 1 page of results then no items are marked as hidden", () => {
            const items = buildItems(10);
            paginator.initialise(items);
            expect(getHiddenItems(items)).toEqual([]);
        });

        it("when there are multiple pages of results and we are on the first page the correct items are marked as hidden", () => {
            const items = buildItems(24);
            paginator.initialise(items);
            expect(getHiddenItems(items)).toEqual(buildHiddenItems(10, 23));
        });

        it("when there are multiple pages of results and we are on the second page the correct items are marked as hidden", () => {
            const items = buildItems(24);
            paginator.initialise(items);
            clickNext();
            expect(getHiddenItems(items)).toEqual([...buildHiddenItems(0, 9), ...buildHiddenItems(20, 23)]);
        });

        it("when there are multiple pages of results and we are on the last page the correct items are marked as hidden", () => {
            const items = buildItems(24);
            paginator.initialise(items);
            clickNext();
            clickNext();
            expect(getHiddenItems(items)).toEqual(buildHiddenItems(0, 19));
        });
    });
})
