import {JSDOM} from 'jsdom';
import {onLoad} from '../../app/assets/javascripts/apiDeployment.js';

describe('apiDeployment', () => {
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <select id="domain">
                <option hidden></option>
                <option value="1"></option>
                <option value="2"></option>
                <option value="3"></option>
            </select>
            <select id="subdomain">
                <option hidden></option>
                <option data-domain="1" value="1.1"></option>
                <option data-domain="1" value="1.2"></option>
                <option data-domain="2" value="2.1"></option>
                <option data-domain="3" value="3.1"></option>
                <option data-domain="3" value="3.2"></option>
                <option data-domain="3" value="3.3"></option>                
            </select>
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function getDomainValue() {
        return document.getElementById('domain').value;
    }
    function setDomainValue(value) {
        document.getElementById('domain').value = value;
        document.getElementById('domain').dispatchEvent(new Event('input'));
    }
    function getSubdomainValue() {
        return document.getElementById('subdomain').value;
    }
    function setSubdomainValue(value) {
        document.getElementById('subdomain').value = value;
    }
    function getSubdomainOptions() {
        return [...document.getElementById('subdomain').options].filter(el => ! el.hidden).map(el => el.value);
    }
    function isSubdomainEnabled() {
        return ! document.getElementById('subdomain').disabled;
    }

    describe("when page loads", () => {
        it("with no values selected, the subdomains list is disabled and no value is shown in either list",  () => {
            onLoad();

            expect(isSubdomainEnabled()).toBe(false);
            expect(getDomainValue()).toBe('');
            expect(getSubdomainValue()).toBe('');
        });

        it("with only domain selected, the subdomains list is enabled and values are filtered correctly",  () => {
            setDomainValue('1');
            onLoad();

            expect(isSubdomainEnabled()).toBe(true);
            expect(getDomainValue()).toBe('1');
            expect(getSubdomainValue()).toBe('');
            expect(getSubdomainOptions()).toEqual(['1.1', '1.2']);
        });

        it("with both values selected, the subdomains list is enabled and values are filtered correctly",  () => {
            setDomainValue('1');
            setSubdomainValue('1.1');
            onLoad();

            expect(isSubdomainEnabled()).toBe(true);
            expect(getDomainValue()).toBe('1');
            expect(getSubdomainValue()).toBe('1.1');
            expect(getSubdomainOptions()).toEqual(['1.1', '1.2']);
        });
    });

    describe("when domain changes", () => {
        it("and multiple subdomains match, then subdomains list is filtered correctly",  () => {
            onLoad();
            setDomainValue('3');

            expect(isSubdomainEnabled()).toBe(true);
            expect(getSubdomainOptions()).toEqual(['3.1', '3.2', '3.3']);
            expect(getSubdomainValue()).toBe('');
        });

        it("and one subdomain matches, then subdomains list is filtered correctly",  () => {
            onLoad();
            setDomainValue('2');

            expect(isSubdomainEnabled()).toBe(true);
            expect(getSubdomainOptions()).toEqual(['2.1']);
            expect(getSubdomainValue()).toBe('');
        });

        it("after subdomain has been selected, then subdomains are re-filtered and value is cleared",  () => {
            onLoad();
            setDomainValue('3');
            setSubdomainValue('3.2');
            setDomainValue('2');

            expect(isSubdomainEnabled()).toBe(true);
            expect(getSubdomainOptions()).toEqual(['2.1']);
            expect(getSubdomainValue()).toBe('');
        });
    });

});
