import {JSDOM} from "jsdom";
import {onPageShow} from "../../app/assets/javascripts/produceApiDomain.js";
import {isVisible} from "./testUtils.js";

describe('onPageShow', () => {
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <form>
                <input type="radio" name="domain" value="1">
                <input type="radio" name="subDomain" value="1.1" data-domain="1" data-basepath="/1/1.1">
                <input type="radio" name="subDomain" value="1.2" data-domain="1" data-basepath="/1/1.2">
                <input type="radio" name="domain" value="2">
                <input type="radio" name="subDomain" value="2.1" data-domain="2" data-basepath="/2/2.1">
            </form>
            <div id="basePathContainer">
                <div id="basePathValue"></div>
            </div>
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function selectValues(domainValue, subDomainValue) {
        const elDomain = document.querySelector(`input[name="domain"][value="${domainValue}"]`),
            elSubDomain = document.querySelector(`input[name="subDomain"][value="${subDomainValue}"]`);

        if (elDomain) {
            elDomain.checked = true;
        }

        if (elSubDomain) {
            elSubDomain.checked = true;
            elSubDomain.dispatchEvent(new Event('click', { bubbles: true }));
        }
    }

    function getBasePath() {
        const elBasePathContainer = document.getElementById('basePathContainer'),
            elBasePathValue = document.getElementById('basePathValue');
        if (isVisible(elBasePathContainer)) {
            return elBasePathValue.innerHTML;
        }
    }

    function isBasePathDisplayed() {
        const elBasePathContainer = document.getElementById('basePathContainer');
        return isVisible(elBasePathContainer);
    }

    it("when no domains or subdomains are selected, then the base path is not shown",  () => {
        onPageShow();

        expect(isBasePathDisplayed()).toBeFalse();
    });

    it("when a domain but no subdomain is selected, then the base path is not shown",  () => {
        onPageShow();

        selectValues("1", null);

        expect(isBasePathDisplayed()).toBeFalse();
    });

    it("when both a domain and a matching subdomain are selected, then the correct base path is shown",  () => {
        onPageShow();

        selectValues("1", "1.2");

        expect(isBasePathDisplayed()).toBeTrue();
        expect(getBasePath()).toBe("/1/1.2");
    });

    it("when a domain and a subdomain are selected but don't match, then the base path is not shown",  () => {
        onPageShow();

        selectValues("1", "2.1");

        expect(isBasePathDisplayed()).toBeFalse();
    });

    it("when a domain and subdomain are pre-selected, then the base path is shown when the page loads",  () => {
        selectValues("1", "1.2");

        onPageShow();

        expect(isBasePathDisplayed()).toBeTrue();
        expect(getBasePath()).toBe("/1/1.2");
    });
});
