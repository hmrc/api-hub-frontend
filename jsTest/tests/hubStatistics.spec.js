import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/hubStatistics.js';
import {buildFakeClipboard, waitFor} from "./testUtils.js";

describe('hubStatistics', () => {
    let document, elTotalApis, elTotalInProduction, elListApisInProduction, elFetchTotalApisButton,
        elFetchProdApisButton, elFetchListOfApisInProdButton, clipboard;
    const statPlaceholder = '...', statError = 'Error';

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="statTotalApis">
                <span class="hip-stat-value"></span>
                <button></button>
            </div>
            <div id="statProdApis">
                <span class="hip-stat-value"></span>
                <button></button>
            </div>
            <div id="statProdList">
                <span class="hip-stat-value"></span>
                <button></button>
            </div>
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
        elTotalApis = document.querySelector('#statTotalApis .hip-stat-value');
        elTotalInProduction = document.querySelector('#statProdApis .hip-stat-value');
        elListApisInProduction = document.querySelector('#statProdList .hip-stat-value');
        elFetchTotalApisButton = document.querySelector('#statTotalApis button');
        elFetchProdApisButton = document.querySelector('#statProdApis button');
        elFetchListOfApisInProdButton = document.querySelector('#statProdList button');
        clipboard = buildFakeClipboard(dom);
    });

    it("when page is first displayed the stat fields are correctly displayed",  () => {
        onDomLoaded();

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elListApisInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
        expect(elFetchListOfApisInProdButton.disabled).toBe(false);
    });

    it("when the total apis fetch button is clicked the stats fields are updated correctly", () => {
        onDomLoaded();
        elFetchTotalApisButton.click();

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elListApisInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(true);
        expect(elFetchProdApisButton.disabled).toBe(true);
        expect(elFetchListOfApisInProdButton.disabled).toBe(false);
    });

    it("when the prod apis fetch button is clicked the stats fields are updated correctly", () => {
        onDomLoaded();
        elFetchProdApisButton.click();

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elListApisInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(true);
        expect(elFetchProdApisButton.disabled).toBe(true);
        expect(elFetchListOfApisInProdButton.disabled).toBe(false);
    });

    it("when the list prod apis fetch button is clicked the stats fields are updated correctly", () => {
        onDomLoaded();
        elFetchListOfApisInProdButton.click();

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elListApisInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
        expect(elFetchListOfApisInProdButton.disabled).toBe(true);
    });

    it("when the fetch response is received the stats fields are updated correctly", async () => {
        const totalApis = 10, totalInProduction = 5;
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.resolve({
            json: () => Promise.resolve({totalApis, totalInProduction})
        }));

        onDomLoaded();
        elFetchTotalApisButton.click();
        await waitFor(() => elTotalApis.textContent !== statPlaceholder, true);

        expect(elTotalApis.textContent).toBe(totalApis.toString());
        expect(elTotalInProduction.textContent).toBe(totalInProduction.toString());
        expect(elListApisInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
        expect(elFetchListOfApisInProdButton.disabled).toBe(false);
        expect(globalThis.fetch).toHaveBeenCalledWith('./statistics/apis-in-production');
    });

    it("when the fetch response returns an error the stats fields are updated correctly", async () => {
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.reject('error'));

        onDomLoaded();
        elFetchTotalApisButton.click();
        await waitFor(() => elTotalApis.textContent !== statPlaceholder, true);

        expect(elTotalApis.textContent).toBe(statError);
        expect(elTotalInProduction.textContent).toBe(statError);
        expect(elListApisInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
        expect(elFetchListOfApisInProdButton.disabled).toBe(false);
    });

    it("when the list prod apis fetch response is received the stats fields are updated correctly", async () => {
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.resolve({
            json: () => Promise.resolve(["api1", "api2"])
        }));

        onDomLoaded();
        elFetchListOfApisInProdButton.click();
        await waitFor(() => elListApisInProduction.textContent !== statPlaceholder, true);

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elListApisInProduction.textContent).toBe('Copy');
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
        expect(elFetchListOfApisInProdButton.disabled).toBe(false);
        expect(globalThis.fetch).toHaveBeenCalledWith('./statistics/list-apis-in-production');
    });

    it("when the list prod apis fetch response returns an error the stats fields are updated correctly", async () => {
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.reject('error'));

        onDomLoaded();
        elFetchListOfApisInProdButton.click();
        await waitFor(() => elListApisInProduction.textContent !== statPlaceholder, true);

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elListApisInProduction.textContent).toBe(statError);
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
        expect(elFetchListOfApisInProdButton.disabled).toBe(false);
    });

    it("when the list prod apis call succeeds and we click the Copy button the correct value is copied", async () => {
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.resolve({
            json: () => Promise.resolve(["api1", "api2"])
        }));

        onDomLoaded();
        elFetchListOfApisInProdButton.click();
        await waitFor(() => elListApisInProduction.textContent !== statPlaceholder, true);
        document.querySelector('#statProdList a').click();
        expect(clipboard.getContents()).toBe('api1\napi2');

    });
});
