import {JSDOM} from 'jsdom';
import {buildFilterResultPanel, buildSearchResultPanel} from "../../app/assets/javascripts/exploreApisResultPanels.js";
import {isVisible} from "./testUtils.js";

describe('exploreApisResultPanels', () => {
    let panel, document;
    describe('buildSearchResultPanel', () => {
        beforeEach(() => {
            const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="searchResultsPanel" class="govuk-!-display-none">
                <p id="resultsSuccess">
                    <span id="searchResultsShowing">Showing</span>
                    <span id="searchResultsCount">0</span> result<span id="searchResultsCountPlural">s</span>
                    for the search
                    <span id="searchResultsTerm"></span>
                </p>
                <p id="resultsError"></p>
                <a id="clearSearch"></a>
            </div>`);
            document = dom.window.document;
            globalThis.document = document;
            panel = buildSearchResultPanel();
        });

        function searchResultsPanel() {
            return document.getElementById('searchResultsPanel');
        }
        function resultsSuccess() {
            return document.getElementById('resultsSuccess');
        }
        function searchResultsShowing() {
            return document.getElementById('searchResultsShowing');
        }
        function panelContent() {
            let visibleContent = resultsSuccess().textContent;
            if (!isVisible(searchResultsShowing())) {
                visibleContent = visibleContent.replace("Showing", "");
            }
            return visibleContent.replace(/\s+/g, ' ').trim();
        }
        function resultsError() {
            return document.getElementById('resultsError');
        }

        it("the panel is not shown by default", () => {
            expect(isVisible(searchResultsPanel())).toBe(false);
        });

        describe('showSuccess', () => {
            it("shows the panel", () => {
                panel.showSuccess(true, 1, "NPS");
                expect(isVisible(searchResultsPanel())).toBe(true);
                expect(isVisible(resultsSuccess())).toBe(true);
                expect(isVisible(resultsError())).toBe(false);
            });
            it("shows the correct caption when isShowing is true", () => {
                panel.showSuccess(true, 1, "NPS");
                expect(panelContent()).toBe("Showing 1 result for the search NPS");
            });
            it("shows the correct caption when isShowing is false", () => {
                panel.showSuccess(false, 1, "NPS");
                expect(panelContent()).toBe("1 result for the search NPS");
            });
            it("shows the correct caption when there are 0 results", () => {
                panel.showSuccess(true, 0, "NPS");
                expect(panelContent()).toBe("Showing 0 results for the search NPS");
            });
            it("shows the correct caption when there are multiple results", () => {
                panel.showSuccess(true, 10, "NPS");
                expect(panelContent()).toBe("Showing 10 results for the search NPS");
            });
            it("followed by hide() hides the panel", () => {
                panel.showSuccess(true, 1, "NPS");
                panel.hide();
                expect(isVisible(searchResultsPanel())).toBe(false);
            });
        });

        describe("showError", () => {
            it("shows the panel", () => {
                panel.showError();
                expect(isVisible(searchResultsPanel())).toBe(true);
                expect(isVisible(resultsSuccess())).toBe(false);
                expect(isVisible(resultsError())).toBe(true);
            });
            it("followed by hide() hides the panel", () => {
                panel.showError();
                panel.hide();
                expect(isVisible(searchResultsPanel())).toBe(false);
            });
        });

        it("the onClear handler is called when the clearSearch link is clicked", () => {
            const handler = jasmine.createSpy('handler');
            panel.onClear(handler);
            document.getElementById('clearSearch').click();
            expect(handler).toHaveBeenCalled();
        });
    });

    describe('buildFilterResultPanel', () => {
        beforeEach(() => {
            const dom = new JSDOM(`
            <!DOCTYPE html>
                <div id="filterResultsPanel" class="govuk-!-display-none">
                    <span class="call-out-type govuk-!-font-weight-regular">
                        Your results are being filtered, showing <span id="filterResultsCount" class="govuk-!-font-weight-bold">0</span> result<span id="filterResultsCountPlural">s</span>.
                    </span>
                    <span id="filterResultsHiddenCount" class="govuk-!-font-weight-bold">0</span>
                    <span id="filterResultsSingleApi">API is</span>
                    <span id="filterResultsMultipleApis">APIs are</span>
                    being hidden by your filters.
                    <a id="clearFilters"></a>
                </div>`);
            document = dom.window.document;
            globalThis.document = document;
            panel = buildFilterResultPanel();
        });

        function filterResultsPanel() {
            return document.getElementById('filterResultsPanel');
        }
        function filterResultsSingleApi() {
            return document.getElementById('filterResultsSingleApi');
        }
        function filterResultsMultipleApis() {
            return document.getElementById('filterResultsMultipleApis');
        }
        function panelContents() {
            let visibleContent = filterResultsPanel().textContent;
            if (isVisible(filterResultsSingleApi())) {
                visibleContent = visibleContent.replace("APIs are", "");
            }
            if (isVisible(filterResultsMultipleApis())) {
                visibleContent = visibleContent.replace("API is", "");
            }
            return visibleContent.replace(/\s+/g, ' ').trim();
        }

        describe('show', () => {
            it("shows the panel", () => {
                panel.show(1, 2);
                expect(isVisible(filterResultsPanel())).toBe(true);
            });
            it("shows the correct caption when there is 1 result and 1 hidden", () => {
                panel.show(1, 1);
                expect(panelContents()).toBe("Your results are being filtered, showing 1 result. 1 API is being hidden by your filters.");
            });
            it("shows the correct caption when there are multiple results and multiple hidden", () => {
                panel.show(10, 5);
                expect(panelContents()).toBe("Your results are being filtered, showing 10 results. 5 APIs are being hidden by your filters.");
            });
            it("followed by hide() hides the panel", () => {
                panel.show(1, 2);
                panel.hide();
                expect(isVisible(filterResultsPanel())).toBe(false);
            });
        });

        it("the onClear handler is called when the clearFilters link is clicked", () => {
            const handler = jasmine.createSpy('handler');
            panel.onClear(handler);
            document.getElementById('clearFilters').click();
            expect(handler).toHaveBeenCalled();
        });
    });

});
