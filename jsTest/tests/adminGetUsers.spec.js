import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/adminGetUsers.js';

describe('adminManageApps', () => {
    const emails = "user1@example.com; user2@example.com;";
    let document, clipboardContents;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="copyButton" data-emails="${emails}"></div>
        `);
        document = dom.window.document;
        globalThis.document = document;
        Object.defineProperty(globalThis, 'navigator', {
           value: dom.window.navigator,
           writable: true
        });

        clipboardContents = null;
        Object.defineProperty(globalThis.navigator, 'clipboard', {
            value: {
                writeText: text => {
                    clipboardContents = text;
                    return Promise.resolve();
                }
            }
        });
    });

    it("when button is clicked then emails are written to clipboard",  () => {
        onDomLoaded();

        document.getElementById('copyButton').click();

        expect(clipboardContents).toEqual(emails);
    });

});
