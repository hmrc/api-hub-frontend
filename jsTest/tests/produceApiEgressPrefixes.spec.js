import {JSDOM} from "jsdom";
import {onDOMContentLoaded} from "../../app/assets/javascripts/produceApiEgressPrefixes.js";

describe('onDOMContentLoaded', () => {
    const errMsgEnterValue = 'Enter a value',
        errMsgForwardSlash = 'Start with a forward slash';
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="prefixContainer">
                <button id="addPrefix">
                <input id="prefix">
                <table id="prefixesTable">
                    <tbody></tbody>
                </table>
            </div>
            <div id="mappingContainer">
                <button id="addMapping">
                <input id="existing">
                <input id="replacement">
                <table id="mappingsTable">
                    <tbody></tbody>
                </table>
            </div>
            <div id="formFields" data-message-enter-value="${errMsgEnterValue}" data-message-forward-slash="${errMsgForwardSlash}"></div>
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    describe("prefixes", () => {
        function getPrefixErrorMessage() {
            const el = document.getElementById('prefixContainer').querySelector('.govuk-error-message');
            if (el) {
                return el.textContent;
            }
        }
        function enterPrefix(prefix) {
            document.getElementById('prefix').value = prefix;
            document.getElementById('addPrefix').dispatchEvent(new Event('click'));
        }
        function getAddedPrefixes() {
            return Array.from(document.getElementById('prefixesTable').querySelectorAll('tr'))
                .map(elTr => elTr.firstChild.textContent);
        }
        function getHiddenFormFieldPrefixes() {
            return Array.from(document.getElementById('formFields').querySelectorAll('input[name="prefixes[]"]')).map(el => el.value);
        }
        function expectPrefixes(...prefixes) {
            expect(getAddedPrefixes()).toEqual(prefixes);
            expect(getHiddenFormFieldPrefixes()).toEqual(prefixes);
        }
        function clickRemovePrefixLink(index) {
            const elRemoveLink = document.getElementById('prefixesTable').querySelectorAll('tr')[index].querySelector('a');
            elRemoveLink.dispatchEvent(new Event('click'));
        }

        it("when valid prefixes are added then they appear in the list in the correct order",  () => {
            onDOMContentLoaded();

            enterPrefix('/prefix1');
            enterPrefix('/prefix2');
            enterPrefix('/prefix3');

            expectPrefixes('/prefix1', '/prefix2', '/prefix3');
        });

        it("when prefixes are removed then the list is updated correctly",  () => {
            onDOMContentLoaded();

            enterPrefix('/prefix1');
            enterPrefix('/prefix2');
            enterPrefix('/prefix3');

            clickRemovePrefixLink(1);
            expectPrefixes('/prefix1', '/prefix3');
            clickRemovePrefixLink(0);
            expectPrefixes('/prefix3');
            clickRemovePrefixLink(0);
            expectPrefixes();
        });

        it("when we try to add an empty prefix then the correct error is displayed",  () => {
            onDOMContentLoaded();

            enterPrefix('');
            expect(getPrefixErrorMessage()).toEqual(errMsgEnterValue);
            expectPrefixes();

            enterPrefix('   ');
            expect(getPrefixErrorMessage()).toEqual(errMsgEnterValue);
            expectPrefixes();
        });

        it("when we try to add a prefix that does not start with a / then the correct error is displayed",  () => {
            onDOMContentLoaded();

            enterPrefix('prefix');
            expect(getPrefixErrorMessage()).toEqual(errMsgForwardSlash);
            expectPrefixes();

            enterPrefix('some/prefix');
            expect(getPrefixErrorMessage()).toEqual(errMsgForwardSlash);
            expectPrefixes();
        });
    });

    describe("mappings", () => {
        function getMappingErrorMessage() {
            const el = document.getElementById('mappingContainer').querySelector('.govuk-error-message');
            if (el) {
                return el.textContent;
            }
        }
        function enterMapping(existing, replacement) {
            document.getElementById('existing').value = existing;
            document.getElementById('replacement').value = replacement;
            document.getElementById('addMapping').dispatchEvent(new Event('click'));
        }
        function getAddedMappings() {
            return Array.from(document.getElementById('mappingsTable').querySelectorAll('tr'))
                .map(elTr => [...elTr.querySelectorAll('td')].slice(0,2).map(elTd => elTd.textContent));
        }
        function getHiddenFormFieldMappings() {
            return Array.from(document.getElementById('formFields').querySelectorAll('input[name="mappings[]"]')).map(el => el.value.split('->'));
        }
        function expectMappings(...mappings) {
            expect(getAddedMappings()).toEqual(mappings);
            expect(getHiddenFormFieldMappings()).toEqual(mappings);
        }
        function clickRemoveMappingLink(index) {
            const elRemoveLink = document.getElementById('mappingsTable').querySelectorAll('tr')[index].querySelector('a');
            elRemoveLink.dispatchEvent(new Event('click'));
        }

        it("when valid mappings are added then they appear in the list in the correct order",  () => {
            onDOMContentLoaded();

            enterMapping('/existing1', '/replacement1');
            enterMapping('/existing2', '/replacement2');
            enterMapping('/existing3', '/replacement3');

            expectMappings(
                ['/existing1', '/replacement1'],
                ['/existing2', '/replacement2'],
                ['/existing3', '/replacement3']
            );
        });

        it("when mappings are removed then the list is updated correctly",  () => {
            onDOMContentLoaded();

            enterMapping('/existing1', '/replacement1');
            enterMapping('/existing2', '/replacement2');
            enterMapping('/existing3', '/replacement3');

            clickRemoveMappingLink(1);
            expectMappings(
                ['/existing1', '/replacement1'],
                ['/existing3', '/replacement3']
            );
            clickRemoveMappingLink(0);
            expectMappings(
                ['/existing3', '/replacement3']
            );
            clickRemoveMappingLink(0);
            expectMappings();
        });

        it("when we try to add an empty mapping then the correct error is displayed",  () => {
            onDOMContentLoaded();

            enterMapping('', '');
            expect(getMappingErrorMessage()).toEqual(errMsgEnterValue);
            expectMappings();

            enterMapping('', '/valid');
            expect(getMappingErrorMessage()).toEqual(errMsgEnterValue);
            expectMappings();

            enterMapping('/valid', '');
            expect(getMappingErrorMessage()).toEqual(errMsgEnterValue);
            expectMappings();

            enterMapping('   ', '   ');
            expect(getMappingErrorMessage()).toEqual(errMsgEnterValue);
            expectMappings();
        });

        it("when we try to add a mapping that does not start with a / then the correct error is displayed",  () => {
            onDOMContentLoaded();

            enterMapping('existing', 'replacement');
            expect(getMappingErrorMessage()).toEqual(errMsgForwardSlash);
            expectMappings();

            enterMapping('existing', '/replacement');
            expect(getMappingErrorMessage()).toEqual(errMsgForwardSlash);
            expectMappings();

            enterMapping('/existing', 'replacement');
            expect(getMappingErrorMessage()).toEqual(errMsgForwardSlash);
            expectMappings();
        });
    });

});
