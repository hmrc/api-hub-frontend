import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/adminManageApps.js';
import {isVisible} from "./testUtils.js";

describe('adminManageApps', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="appDetailPanels">
                <div class="hip-application"></div>
            </div>
            <div id="displayCountMessage">
                <span id="displayCount"></span>
                <span id="totalCount"></span>
            </div>
            <div id="pagination"></div>
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function paginationIsAvailable() {
        return isVisible(document.getElementById('pagination'));
    }
    function displayCountMessageIsVisible() {
        return isVisible(document.getElementById('displayCountMessage'));
    }
    function getDisplayCount() {
        return Number(document.getElementById('displayCount').textContent);
    }
    function getTotalCount() {
        return Number(document.getElementById('totalCount').textContent);
    }
    function getVisibleAppIndexes() {
        return Array.from(document.querySelectorAll('.hip-application'))
            .filter(el => !el.classList.contains('govuk-!-display-none'))
            .map(el => Number(el.dataset.index));
    }
    function arrayFromTo(from, to) {
        return Array.from({length: to - from + 1}, (_, i) => i + from);
    }
    function getPaginationPageLink(pageNumber) {
        return document.querySelector(`#pagination a[data-page="${pageNumber}"]`);
    }

    function buildAppPanels(count) {
        document.getElementById('appDetailPanels').innerHTML = Array.from(
            {length: count},
            (_, i) => `<div class="hip-application" data-index="${i+1}">App ${i+1}</div>`
        ).join('');
    }

    it("if 20 applications are present on the page then all are visible and pagination is not available",  () => {
        buildAppPanels(20);

        onDomLoaded();

        expect(paginationIsAvailable()).toBeFalse();
        expect(displayCountMessageIsVisible()).toBeFalse();
    });

    it("if 21 applications are present on the page then only the first 20 are visible and pagination is available",  () => {
        buildAppPanels(21);

        onDomLoaded();

        expect(paginationIsAvailable()).toBeTrue();
        expect(displayCountMessageIsVisible()).toBeTrue();
    });

    it("when the page loads only the first 20 applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();

        expect(getDisplayCount()).toBe(20);
        expect(getTotalCount()).toBe(101);
        expect(getVisibleAppIndexes()).toEqual(arrayFromTo(1, 20));
    });

    it("when we navigate to the second page the correct applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();
        getPaginationPageLink(2).click();

        expect(getDisplayCount()).toBe(20);
        expect(getTotalCount()).toBe(101);
        expect(getVisibleAppIndexes()).toEqual(arrayFromTo(21, 40));
    });

    it("when we navigate to the final page the correct applications are visible and the display message is correct",  () => {
        buildAppPanels(101);

        onDomLoaded();
        getPaginationPageLink(6).click();

        expect(getDisplayCount()).toBe(1);
        expect(getTotalCount()).toBe(101);
        expect(getVisibleAppIndexes()).toEqual(arrayFromTo(101, 101));
    });
});
