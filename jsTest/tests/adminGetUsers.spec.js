import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/adminGetUsers.js';
import {buildFakeClipboard} from "./testUtils.js";

describe('adminManageApps', () => {
    const emails = "user1@example.com; user2@example.com;";
    let document, clipboard;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="copyButton" data-emails="${emails}"></div>
        `);
        document = dom.window.document;
        globalThis.document = document;
        clipboard = buildFakeClipboard(dom);
    });

    it("when button is clicked then emails are written to clipboard",  () => {
        onDomLoaded();

        document.getElementById('copyButton').click();

        expect(clipboard.getContents()).toEqual(emails);
    });

});
