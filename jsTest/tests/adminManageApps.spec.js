import {JSDOM} from 'jsdom';
import {onDomLoaded} from '../../app/assets/javascripts/adminManageApps.js';
import {isVisible} from "./testUtils.js";

describe('adminManageApps', () => {
    let document;

    beforeEach(() => {
        const dom = (new JSDOM(`
            <!DOCTYPE html>
            <div id="appDetailPanels">
                <div class="hip-application"></div>
            </div>
            <div id="displayCountMessage">
                <span id="displayCount"></span>
                <span id="totalCount"></span>
            </div>
            <div id="pagination"></div>
        `));
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function paginationIsAvailable() {
        return isVisible(document.getElementById('pagination'));
    }
    function displayCountMessageIsVisible() {
        return isVisible(document.getElementById('displayCountMessage'));
    }



    it("if 20 applications are present on the page then all are visible and pagination is not available",  () => {
        buildAppPanels(20);

        onDomLoaded();

        expect(paginationIsAvailable()).toBeFalse();
        expect(displayCountMessageIsVisible()).toBeFalse();
    });

    it("if 21 applications are present on the page then only the first 20 are visible and pagination is available",  () => {
        buildAppPanels(21);

        onDomLoaded();

        expect(paginationIsAvailable()).toBeTrue();
        expect(displayCountMessageIsVisible()).toBeTrue();
    });


    });
});
