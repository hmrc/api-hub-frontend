import {buildPaginator} from '../../app/assets/javascripts/pagination.js';
import {isVisible} from "./testUtils.js";
import {JSDOM} from 'jsdom';

describe('pagination', () => {
    let elContainer, paginator, document, newPageNumber;

    beforeEach(() => {
        const dom = (new JSDOM('<!DOCTYPE html><div id="pg"></div>'))
        document = dom.window.document;
        elContainer = document.getElementById('pg');
        document.body.appendChild(elContainer);
        globalThis.document = document;
        paginator = buildPaginator(elContainer);
        paginator.onNavigation(p => {
            newPageNumber = p;
        });
    });
    afterEach(() => {
        document.body.removeChild(elContainer);
    });

    it("paginator not visible if only 1 page of results", () => {
        paginator.render(1, 1);
        expect(isVisible(elContainer)).toBeFalse();
    });

    describe('Previous link', () => {
        function previousLink() {
            return elContainer.querySelector('.govuk-pagination__prev');
        }

        it("previous link not visible when on first page", () => {
            paginator.render(1, 10);
            expect(isVisible(previousLink())).toBeFalse();
        });

        it("previous link is visible when on second page", () => {
            paginator.render(2, 10);
            expect(isVisible(previousLink())).toBeTrue();
        });

        it("clicking previous link navigates to the previous page", () => {
            paginator.render(2, 10);
            previousLink().click();
            expect(newPageNumber).toBe(1);
        });
    });

    describe('Next link', () => {
        function nextLink() {
            return elContainer.querySelector('.govuk-pagination__next');
        }

        it("next link not visible when on last page", () => {
            paginator.render(5, 5);
            expect(isVisible(nextLink())).toBeFalse();
        });

        it("next link is visible when on second to last page", () => {
            paginator.render(4, 5);
            expect(isVisible(nextLink())).toBeTrue();
        });

        it("clicking next link navigates to the next page", () => {
            paginator.render(1, 5);
            nextLink().click();
            expect(newPageNumber).toBe(2);
        });
    });

    describe('Page number links', () => {
        function expectOnlyPageLinks(...expectedPagesVisible) {
            const pagesVisible = [...elContainer.querySelectorAll('.govuk-pagination__link[data-page]')].map(el => parseInt(el.dataset['page']));
            expect(pagesVisible).toEqual(expectedPagesVisible);
        }
        it("When 2 pages of items exist then links to both pages are visible ", () => {
            paginator.render(1, 2);
            expectOnlyPageLinks(1, 2);
        });
        it("When 3 pages of items exist then links to all pages are visible ", () => {
            paginator.render(1, 3);
            expectOnlyPageLinks(1, 2, 3);
        });
        describe("When many pages of items exist", () => {
            const pageCount = 10;
            it("and we are on page 1 then links to pages 1, 2 and the final page are visible", () => {
                paginator.render(1, pageCount);
                expectOnlyPageLinks(1, 2, pageCount);
            });
            it("and we are on page 2 then links to pages 1, 2, 3 and the final page are visible", () => {
                paginator.render(2, pageCount);
                expectOnlyPageLinks(1, 2, 3, pageCount);
            });
            it("and we are on page 3 then links to pages 1, 2, 3, 4 and the final page are visible", () => {
                paginator.render(3, pageCount);
                expectOnlyPageLinks(1, 2, 3, 4, pageCount);
            });
            it("and we are on page 4 then links to pages 1, 3, 4, 5 and the final page are visible", () => {
                paginator.render(4, pageCount);
                expectOnlyPageLinks(1, 3, 4, 5, pageCount);
            });
            it("and we are on page 9 then links to pages 1, 8, 9 and 10 are visible", () => {
                paginator.render(pageCount - 1, pageCount);
                expectOnlyPageLinks(1, pageCount - 2, pageCount - 1, pageCount);
            });
            it("and we are on page 10 then links to pages 1, 9 and 10 are visible", () => {
                paginator.render(pageCount, pageCount);
                expectOnlyPageLinks(1, pageCount - 1, pageCount);
            });
        });
        it("Clicking a page number link navigates to that page", () => {
            paginator.render(2, 5);
            elContainer.querySelector('.govuk-pagination__link[data-page="3"]').click();
            expect(newPageNumber).toBe(3);
        });
        it("Only the current page number is highlighted", () => {
            paginator.render(2, 5);
            const highlightedLinks = elContainer.querySelectorAll('.govuk-pagination__item--current');
            expect(highlightedLinks.length).toBe(1);
            expect(highlightedLinks[0].textContent.trim()).toBe('2');
        });
    });
})
