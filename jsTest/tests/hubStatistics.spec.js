import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/hubStatistics.js';
import {waitFor} from "./testUtils.js";

describe('hubStatistics', () => {
    let document, elTotalApis, elTotalInProduction, elFetchTotalApisButton, elFetchProdApisButton;
    const statPlaceholder = '...', statError = 'Error';

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="statTotalApis">
                <span class="hip-stat-number"></span>
                <button></button>
            </div>
            <div id="statProdApis">
                <span class="hip-stat-number"></span>
                <button></button>
            </div>
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
        elTotalApis = document.querySelector('#statTotalApis .hip-stat-number');
        elTotalInProduction = document.querySelector('#statProdApis .hip-stat-number');
        elFetchTotalApisButton = document.querySelector('#statTotalApis button');
        elFetchProdApisButton = document.querySelector('#statProdApis button');
    });

    it("when page is first displayed the stat fields are correctly displayed",  () => {
        onDomLoaded();

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
    });

    it("when the total apis fetch button is clicked the stats fields are updated correctly", () => {
        onDomLoaded();
        elFetchTotalApisButton.click();

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(true);
        expect(elFetchProdApisButton.disabled).toBe(true);
    });

    it("when the prod apis fetch button is clicked the stats fields are updated correctly", () => {
        onDomLoaded();
        elFetchProdApisButton.click();

        expect(elTotalApis.textContent).toBe(statPlaceholder);
        expect(elTotalInProduction.textContent).toBe(statPlaceholder);
        expect(elFetchTotalApisButton.disabled).toBe(true);
        expect(elFetchProdApisButton.disabled).toBe(true);
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
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
    });

    it("when the fetch response returns an error the stats fields are updated correctly", async () => {
        globalThis.fetch = jasmine.createSpy('fetch').and.returnValue(Promise.reject('error'));

        onDomLoaded();
        elFetchTotalApisButton.click();
        await waitFor(() => elTotalApis.textContent !== statPlaceholder, true);

        expect(elTotalApis.textContent).toBe(statError);
        expect(elTotalInProduction.textContent).toBe(statError);
        expect(elFetchTotalApisButton.disabled).toBe(false);
        expect(elFetchProdApisButton.disabled).toBe(false);
    });
});
