import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/testApimEndpoints.js';
import {buildFakeClipboard, isVisible, waitFor} from "./testUtils.js";

describe('testApimEndpoints', () => {
    let document, clipboard, elSelectEnvironment, elSelectEndpoint, elResponseContainer;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <form>
                <fieldset id="apimRequestInputs">
                    <select id="selectEnvironment">
                        <option value="">Select something</option>
                        <option value="test">Test</option>                    
                    </select>
                    <div class="govuk-form-group">
                        <select id="selectEndpoint">
                            <option value="">Select something</option>
                            <option value="endpointNoParams" data-param-names>No params</option>
                            <option value="endpointParams" data-param-names="p1,p2">With params</option>
                        </select>
                    </div>
                    <div id="parameterInputs"></div>
                </fieldset>
            </form>
            <button id="submit"></button>
            <div id="apimResponseContainer">
                <button id="copyApimResponse"></button>
                <pre id="apimResponse"></pre>
            </div>
        `);
        document = dom.window.document;
        elSelectEnvironment = document.getElementById('selectEnvironment');
        elSelectEndpoint = document.getElementById('selectEndpoint');
        elResponseContainer = document.getElementById('apimResponseContainer');
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
        clipboard = buildFakeClipboard(dom);
    });

    function selectEnvironment(index) {
        elSelectEnvironment.selectedIndex = index;
        elSelectEnvironment.dispatchEvent(new Event('input'));
    }
    function selectEndpoint(index) {
        elSelectEndpoint.selectedIndex = index;
        elSelectEndpoint.dispatchEvent(new Event('input'));
    }
    function environmentListVisible() {
        return isVisible(elSelectEndpoint.closest('.govuk-form-group'));
    }
    function submitButtonEnabled() {
        return !document.getElementById('submit').disabled;
    }
    function clickSubmit() {
        document.getElementById('submit').click();
    }
    function getParameterNames() {
        return [...document.getElementById('parameterInputs').querySelectorAll('input')].map(el => el.name);
    }
    function enterParamValue(index, value) {
        const el = document.getElementById('parameterInputs').querySelectorAll('input')[index];
        el.value = value;
    }

    it("when page is first displayed environment selection list is visible",  () => {
        onDomLoaded();

        expect(isVisible(elSelectEnvironment)).toBe(true);
        expect(environmentListVisible()).toBe(false);
        expect(submitButtonEnabled()).toBe(false);
    });

    it("when an environment is selected the endpoints list becomes visible",  () => {
        onDomLoaded();

        selectEnvironment(1);

        expect(environmentListVisible()).toBe(true);
        expect(submitButtonEnabled()).toBe(false);
    });

    it("when an environment is de-selected the endpoints list becomes invisible",  () => {
        onDomLoaded();

        selectEnvironment(0);

        expect(environmentListVisible()).toBe(false);
        expect(submitButtonEnabled()).toBe(false);
    });

    it("when an endpoint without parameters is selected then no inputs are visible",  () => {
        onDomLoaded();

        selectEnvironment(1);
        selectEndpoint(1);

        expect(getParameterNames()).toEqual([]);
        expect(submitButtonEnabled()).toBe(true);
    });

    it("when an endpoint with parameters is selected then the correct inputs are visible",  () => {
        onDomLoaded();

        selectEnvironment(1);
        selectEndpoint(2);

        expect(getParameterNames()).toEqual(['p1', 'p2']);
        expect(submitButtonEnabled()).toBe(true);
    });

    it("when the submit button is clicked the correct request is sent to the server and the response is displayed",  async () => {
        const serverResponse = "hello";
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.resolve({
            ok: true,
            text: () => Promise.resolve(serverResponse)
        }));

        onDomLoaded();

        selectEnvironment(1);
        selectEndpoint(2);
        enterParamValue(0, "v1");
        enterParamValue(1, "v2");
        clickSubmit();

        expect(submitButtonEnabled()).toBe(false);

        await waitFor(() => isVisible(elResponseContainer), true);

        expect(document.getElementById('apimResponse').innerText).toBe(serverResponse);
        expect(globalThis.fetch).toHaveBeenCalledWith('test-apim-endpoints/test/endpointParams/v1,v2');
        expect(submitButtonEnabled()).toBe(true);
    });

    it("when the server responds with an error, then the error message is displayed",  async () => {
        const serverResponse = "bork";
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.resolve({
            ok: false,
            status: 400,
            statusText: "Bad Request",
            text: () => Promise.resolve(serverResponse)
        }));

        onDomLoaded();

        selectEnvironment(1);
        selectEndpoint(1);
        clickSubmit();

        await waitFor(() => isVisible(elResponseContainer), true);

        const displayedMessage = document.getElementById('apimResponse').innerText;
        expect(displayedMessage).toContain("Hub endpoint returned HTTP error: 400 - Bad Request");
        expect(displayedMessage).toContain("Request not submitted to APIM");
        expect(displayedMessage).toContain(serverResponse);
    });
});
