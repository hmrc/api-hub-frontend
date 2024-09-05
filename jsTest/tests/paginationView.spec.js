import {buildPaginationView} from '../../app/assets/javascripts/paginationView.js';
import {paginationHelper, paginationContainerHtml, isVisible} from "./testUtils.js";
import {JSDOM} from 'jsdom';

describe('paginationView', () => {
    let paginationView, document;

    beforeEach(() => {
        const dom = new JSDOM(`<!DOCTYPE html><div id="panels"></div>${paginationContainerHtml}`);
        document = dom.window.document;
        globalThis.document = document;
        paginationView = buildPaginationView();
    });

    it("paginationView not visible if only 1 page of results", () => {
        paginationView.render(1, 1);
        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
    });

    describe('Previous link', () => {
        it("previous link not visible when on first page", () => {
            paginationView.render(1, 3);
            expect(isVisible(paginationHelper.previousLink())).toBeFalse();
        });

        it("previous link is visible when on second page", () => {
            paginationView.render(2, 3);
            expect(isVisible(paginationHelper.previousLink())).toBeTrue();
        });
    });

    describe('Next link', () => {
        it("next link not visible when on last page", () => {
            paginationView.render(3, 3);
            expect(isVisible(paginationHelper.nextLink())).toBeFalse();
        });

        it("next link is visible when on second to last page", () => {
            paginationView.render(2, 3);
            expect(isVisible(paginationHelper.nextLink())).toBeTrue();
        });
    });

    describe('Page number links', () => {
        it("When 2 pages of items exist then links to both pages are visible ", () => {
            paginationView.render(1, 2);
            expect(paginationHelper.getPageNumberLinks()).toEqual([1, 2]);
        });
        it("When 3 pages of items exist then links to all pages are visible ", () => {
            paginationView.render(1, 3);
            expect(paginationHelper.getPageNumberLinks()).toEqual([1, 2, 3]);
        });
        describe("When many pages of items exist", () => {
            const pageCount = 10;
            it("and we are on page 1 then links to pages 1, 2 and the final page are visible", () => {
                paginationView.render(1, pageCount);
                expect(paginationHelper.getPageNumberLinks()).toEqual([1, 2, pageCount]);
            });
            it("and we are on page 2 then links to pages 1, 2, 3 and the final page are visible", () => {
                paginationView.render(2, pageCount);
                expect(paginationHelper.getPageNumberLinks()).toEqual([1, 2, 3, pageCount]);
            });
            it("and we are on page 3 then links to pages 1, 2, 3, 4 and the final page are visible", () => {
                paginationView.render(3, pageCount);
                expect(paginationHelper.getPageNumberLinks()).toEqual([1, 2, 3, 4, pageCount]);
            });
            it("and we are on page 4 then links to pages 1, 3, 4, 5 and the final page are visible", () => {
                paginationView.render(4, pageCount);
                expect(paginationHelper.getPageNumberLinks()).toEqual([1, 3, 4, 5, pageCount]);
            });
            it("and we are on page 9 then links to pages 1, 8, 9 and 10 are visible", () => {
                paginationView.render(pageCount - 1, pageCount);
                expect(paginationHelper.getPageNumberLinks()).toEqual([1, pageCount - 2, pageCount - 1, pageCount]);
            });
            it("and we are on page 10 then links to pages 1, 9 and 10 are visible", () => {
                paginationView.render(pageCount, pageCount);
                expect(paginationHelper.getPageNumberLinks()).toEqual([1, pageCount - 1, pageCount]);
            });
        });
        it("Only the current page number is highlighted", () => {
            paginationView.render(2, 5);
            expect(paginationHelper.getCurrentPageLinkNumber()).toBe(2);
        });
    });
});
