import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/adminManageApps.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('adminManageApps', () => {
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="appDetailPanels">
                <div class="hip-application"></div>
            </div>
            <input id="appFilter" type="text">
            <div id="noResultsPanel"></div>
            <div id="appCount"></div>
            ${paginationContainerHtml}
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildAppPanels(count, prefix = '', dataIndex = 0) {
        const content = Array.from(
                {length: count},
                (_, i) => `<div class="hip-application" data-index="${i+1+dataIndex}" data-app-name="${prefix}App ${i+1+dataIndex}" data-app-id="${prefix}AppId${i+1+dataIndex}">${prefix}App ${i+1+dataIndex}</div>`
            ).join('');
        const htmlElement = document.getElementById('appDetailPanels');
        if (dataIndex === 0) {
            htmlElement.innerHTML = content;
        } else {
            htmlElement.insertAdjacentHTML('beforeend', content);
        }
    }

    function enterAppFilterText(value) {
        document.getElementById('appFilter').value = value;
        document.getElementById('appFilter').dispatchEvent(new Event('input'));
    }

    it("if 20 apps are present on the page then all are visible and pagination is not available",  () => {
        buildAppPanels(20);

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
    });

    it("if 21 apps are present on the page then only the first 20 are visible and pagination is available",  () => {
        buildAppPanels(21);

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
        expect(paginationHelper.getVisiblePanelIndexes('.hip-application')).toEqual(arrayFromTo(1, 20));
    });

    it("when the user enters some filter text then only the apps that match the name filter are shown",  () => {
        const prefixSearch = 'Foo';
        buildAppPanels(100, prefixSearch);
        buildAppPanels(100, 'Bar', 100);

        onPageShow();
        enterAppFilterText(`${prefixSearch}App`);

        expect(paginationHelper.getVisiblePanelData('.hip-application', 'appName').map(o => o.appName)).toEqual(
            [...Array(20).keys()].map(i => `${prefixSearch}App ${i+1}`)
        );
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
    });

    it("when the user enters some filter text then only the apps that match the id filter are shown",  () => {
        const prefixSearch = 'Foo';
        buildAppPanels(100, prefixSearch);
        buildAppPanels(100, 'Bar', 100);

        onPageShow();
        enterAppFilterText(`${prefixSearch}AppId`);

        expect(paginationHelper.getVisiblePanelData('.hip-application', 'appId').map(o => o.appId)).toEqual(
           [...Array(20).keys()].map(i => `${prefixSearch}AppId${i+1}`)
        );
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
    });

});
