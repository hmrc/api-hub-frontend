import {buildPaginator} from '../../app/assets/javascripts/paginationController.js';
import {JSDOM} from 'jsdom';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('paginationController', () => {
    let paginator, document;

    beforeEach(() => {
        const dom = new JSDOM(`<!DOCTYPE html><div id="panels"></div>${paginationContainerHtml}`);
        document = dom.window.document;
        globalThis.document = document;
        paginator = buildPaginator(10);
    });

    function buildItems(count) {
        document.getElementById('panels').innerHTML = Array.from({length: count}, (o, i) => `<div data-index="${i}"></div>`).join('');
        return [...document.querySelectorAll('#panels div')];
    }

    describe("isPaginating", () => {
        it("when less than 1 page of items exist, then the pagination view is not visible", () => {
            paginator.render(buildItems(9));
            expect(paginationHelper.paginationIsAvailable()).toBe(false);
        });

        it("when exactly 1 page of items exists, then the pagination view is not visible", () => {
            paginator.render(buildItems(10));
            expect(paginationHelper.paginationIsAvailable()).toBe(false);
        });

        it("when 1 more than 1 page of items exists, then the pagination view is not visible", () => {
            paginator.render(buildItems(11));
            expect(paginationHelper.paginationIsAvailable()).toBe(true);
        });
    });

    describe("item count", () => {
        it("item counts are correct when there is only one page of results", () => {
            paginator.render(buildItems(7));
            expect(paginationHelper.getShowingCount()).toBe(7);
            expect(paginationHelper.getTotalCount()).toBe(7);
        });
        it("item counts are correct when there are multiple pages and we navigate through each of them", () => {
            paginator.render(buildItems(32));

            expect(paginationHelper.getShowingCount()).toBe(10);
            expect(paginationHelper.getTotalCount()).toBe(32);

            paginationHelper.clickNext();
            expect(paginationHelper.getShowingCount()).toBe(10);
            expect(paginationHelper.getTotalCount()).toBe(32);

            paginationHelper.clickNext();
            expect(paginationHelper.getShowingCount()).toBe(10);
            expect(paginationHelper.getTotalCount()).toBe(32);

            paginationHelper.clickNext();
            expect(paginationHelper.getShowingCount()).toBe(2); // final page, this count is different
            expect(paginationHelper.getTotalCount()).toBe(32);
        });
        it("item count is correct on the last page when it has exactly 1 page worth of items", () => {
            paginator.render(buildItems(20));

            expect(paginationHelper.getShowingCount()).toBe(10);
            expect(paginationHelper.getTotalCount()).toBe(20);

            paginationHelper.clickNext();
            expect(paginationHelper.getShowingCount()).toBe(10);
            expect(paginationHelper.getTotalCount()).toBe(20);
        });
    });

    describe('item visibility', () => {
        it("when there is only 1 page of results then all items are visible", () => {
            const items = buildItems(10);
            paginator.render(items);
            expect(paginationHelper.getVisiblePanelIndexes('#panels div')).toEqual(arrayFromTo(0, 9));
        });

        it("when there are multiple pages of results and we are on the first page the correct items are marked as hidden", () => {
            const items = buildItems(24);
            paginator.render(items);
            expect(paginationHelper.getVisiblePanelIndexes('#panels div')).toEqual(arrayFromTo(0, 9));
        });

        it("when there are multiple pages of results and we are on the second page the correct items are marked as hidden", () => {
            const items = buildItems(24);
            paginator.render(items);
            paginationHelper.clickNext();
            expect(paginationHelper.getVisiblePanelIndexes('#panels div')).toEqual(arrayFromTo(10, 19));
        });

        it("when there are multiple pages of results and we are on the last page the correct items are marked as hidden", () => {
            const items = buildItems(24);
            paginator.render(items);
            paginationHelper.clickNext();
            paginationHelper.clickNext();
            expect(paginationHelper.getVisiblePanelIndexes('#panels div')).toEqual(arrayFromTo(20, 23));
        });
    });

    describe('navigation', () => {
        it("clicking the next link navigates to the next page", () => {
            const items = buildItems(30);
            paginator.render(items);

            expect(paginationHelper.getCurrentPageNumber()).toBe(1);

            paginationHelper.clickNext();
            expect(paginationHelper.getCurrentPageNumber()).toBe(2);

            paginationHelper.clickNext();
            expect(paginationHelper.getCurrentPageNumber()).toBe(3);
        });

        it("clicking the previous link navigates to the previous page", () => {
            const items = buildItems(30);
            paginator.render(items);
            paginationHelper.clickNext();
            paginationHelper.clickNext();

            expect(paginationHelper.getCurrentPageNumber()).toBe(3);

            paginationHelper.clickPrevious();
            expect(paginationHelper.getCurrentPageNumber()).toBe(2);

            paginationHelper.clickPrevious();
            expect(paginationHelper.getCurrentPageNumber()).toBe(1);
        });

        it("clicking page number links navigates to the correct page", () => {
            const items = buildItems(30);
            paginator.render(items);

            paginationHelper.getPaginationPageLink(2).click();
            expect(paginationHelper.getCurrentPageNumber()).toBe(2);

            paginationHelper.getPaginationPageLink(3).click();
            expect(paginationHelper.getCurrentPageNumber()).toBe(3);

            paginationHelper.getPaginationPageLink(1).click();
            expect(paginationHelper.getCurrentPageNumber()).toBe(1);
        });
    });
});
