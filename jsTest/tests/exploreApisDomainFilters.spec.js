import {JSDOM} from 'jsdom';
import {buildDomainFilters} from "../../app/assets/javascripts/exploreApisDomainFilters.js";

describe('exploreApisDomainFilters', () => {
    let document, domainFilters, apis;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="domainFilterCount"></div>
            <details id="viewDomainFilters"><summary>View</summary></details>
            <div id="domainFilters">
                <div><input class="domainFilter" type="checkbox" value="d1"></div>
                <div class="subdomainCheckboxes" data-domain="d1">
                    <div><input class="subDomainFilter" type="checkbox" value="d1s1" data-domain="d1"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d1s2" data-domain="d1"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d1s3" data-domain="d1"></div>
                </div>
                <div><input class="domainFilter" type="checkbox" value="d2"></div>
                <div class="subdomainCheckboxes" data-domain="d2">
                    <div><input class="subDomainFilter" type="checkbox" value="d2s1" data-domain="d2"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d2s2" data-domain="d2"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d2s3" data-domain="d2"></div>
                </div>
                <div><input class="domainFilter" type="checkbox" value="d3"></div>
                <div class="subdomainCheckboxes" data-domain="d3">
                    <div><input class="subDomainFilter" type="checkbox" value="d3s1" data-domain="d3"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d3s2" data-domain="d3"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d3s3" data-domain="d3"></div>
                </div>
                <div><input class="domainFilter" type="checkbox" value="d4"></div>
                <div class="subdomainCheckboxes" data-domain="d4">
                    <div><input class="subDomainFilter" type="checkbox" value="d4s1" data-domain="d4"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d4s2" data-domain="d4"></div>
                    <div><input class="subDomainFilter" type="checkbox" value="d4s3" data-domain="d4"></div>
                </div>
            </div>
        `));
        document = dom.window.document;
        globalThis.document = document;

        apis = [
            {data: {domain: 'd1', subdomain: 'd1s1'}},
            {data: {domain: 'd1', subdomain: 'd1s2'}},
            {data: {domain: 'd1', subdomain: 'd1s3'}},
            {data: {domain: 'd2', subdomain: 'd2s1'}},
            {data: {domain: 'd3', subdomain: ''}},
            {data: {domain: '', subdomain: 'd3s1'}},
            {data: {domain: 'dx', subdomain: 'dxs1'}},
            {data: {domain: '', subdomain: ''}},
        ];

        domainFilters = buildDomainFilters();
    });

    function domainCheckbox(domain) {
        return document.querySelector(`.domainFilter[value="${domain}"]`);
    }

    function subdomainCheckbox(subDomain) {
        return document.querySelector(`.subDomainFilter[value="${subDomain}"]`);
    }

    describe("initialise", () => {
        it("removes checkboxes for domains not in use by any APIs",  () => {
            domainFilters.initialise(apis);

            expect([...document.querySelectorAll('.domainFilter')].map(el => el.value)).toEqual(['d1', 'd2', 'd3']);
            expect([...document.querySelectorAll('.subDomainFilter')].map(el => el.value)).toEqual(['d1s1', 'd1s2', 'd1s3', 'd2s1']);
        });

        it("after initialisation clicking a checkbox triggers the onChange handler",  () => {
            let changeCount = 0;
            domainFilters.onChange(() => changeCount++);

            const elDomainCheckbox = document.querySelector('.domainFilter'),
                elSubdomainCheckbox = document.querySelector('.domainFilter');

            elDomainCheckbox.click();
            elSubdomainCheckbox.click();
            expect(changeCount).toBe(0);

            domainFilters.initialise(apis);

            elDomainCheckbox.click();
            expect(changeCount).toBe(1);
            elSubdomainCheckbox.click();
            expect(changeCount).toBe(2);
        });

        it("checking a domain checkbox displays and checks all associated subdomain checkboxes",  () => {
            domainFilters.initialise(apis);

            domainCheckbox('d1').click();
            expect(domainCheckbox('d1').checked).toBe(true);

            expect(subdomainCheckbox('d1s1').checked).toBe(true);
            expect(subdomainCheckbox('d1s2').checked).toBe(true);
            expect(subdomainCheckbox('d1s3').checked).toBe(true);

            expect(document.querySelector('.subdomainCheckboxes[data-domain="d1"]').style.display).toBe('block');
        });

        it("unchecking a domain checkbox hides and unchecks all associated subdomain checkboxes",  () => {
            domainFilters.initialise(apis);

            domainCheckbox('d1').click();
            expect(domainCheckbox('d1').checked).toBe(true);
            domainCheckbox('d1').click();
            expect(domainCheckbox('d1').checked).toBe(false);

            expect(subdomainCheckbox('d1s1').checked).toBe(false);
            expect(subdomainCheckbox('d1s2').checked).toBe(false);
            expect(subdomainCheckbox('d1s3').checked).toBe(false);

            expect(document.querySelector('.subdomainCheckboxes[data-domain="d1"]').style.display).toBe('none');
        });

        it("if no domains are selected then the domain filter section is collapsed",  () => {
            domainFilters.initialise(apis);
            expect(document.getElementById('viewDomainFilters').open).toBe(false);
        });

        it("if domains are selected then the domain filter section is open",  () => {
            domainCheckbox('d1').click();
            domainFilters.initialise(apis);
            expect(document.getElementById('viewDomainFilters').open).toBe(true);
        });

        it("if no domains are selected then the domain filter count is zero",  () => {
            domainFilters.initialise(apis);
            expect(document.getElementById('domainFilterCount').textContent).toBe('0');
        });

        it("if domains are selected then the domain filter count is the number of selected domains",  () => {
            domainCheckbox('d1').click();
            domainFilters.initialise(apis);
            expect(document.getElementById('domainFilterCount').textContent).toBe('1');
        });
    });

    describe("clear", () => {
        beforeEach(() => {
            domainFilters.initialise(apis);
        });

        it("clears all domain and subdomain checkboxes",  () => {
            domainCheckbox('d1').click();
            domainCheckbox('d2').click();
            domainCheckbox('d3').click();

            domainFilters.clear();

            expect(document.querySelectorAll('.domainFilter:checked').length).toBe(0);
            expect(document.querySelectorAll('.subDomainFilter:checked').length).toBe(0);
        });

        it("hides all subdomain checkboxes",  () => {
            domainCheckbox('d1').click();
            domainCheckbox('d2').click();
            domainCheckbox('d3').click();

            domainFilters.clear();

            expect([...document.querySelectorAll('.subdomainCheckboxes')].map(el => el.style.display)).toEqual(['none', 'none', 'none']);
        });

        it("collapses the domain filter section",  () => {
            document.getElementById('viewDomainFilters').setAttribute('open', 'open');
            domainFilters.clear();
            expect(document.getElementById('viewDomainFilters').open).toBe(false);
        });

        it("sets the domain filter count to zero",  () => {
            domainCheckbox('d1').click();
            domainFilters.clear();
            expect(document.getElementById('domainFilterCount').textContent).toBe('0');
        });
    });

    describe("the filter function", () => {
        let data;
        beforeEach(() => {
            domainFilters.initialise(apis);
            data = apis.map(api => api.data);
        });

        it("returns true for all items if no checkboxes are selected",  () => {
            let filterFunction = domainFilters.buildFilterFunction();
            expect(data.every(filterFunction)).toBe(true);
        });

        it("returns true for items with valid domain/subdomain if all checkboxes are selected",  () => {
            document.querySelectorAll('input').forEach(el => el.checked = true);
            let filterFunction = domainFilters.buildFilterFunction();
            expect(data.map(filterFunction)).toEqual([true, true, true, true, true, false, false, false]);
        });

        it("returns true for all items with a particular domain if none of its subdomain checkboxes are selected",  () => {
            domainCheckbox('d1').click();
            subdomainCheckbox('d1s1').click();
            subdomainCheckbox('d1s2').click();
            subdomainCheckbox('d1s3').click();

            let filterFunction = domainFilters.buildFilterFunction();
            ['d1s1', 'd1s2', 'd1s3', '', 'anything'].forEach(subdomain => {
                expect(filterFunction({domain: 'd1', subdomain})).toBe(true);
            });
        });
    });

});
