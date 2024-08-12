import {setVisible, noop, pluralise} from "./utils.js";

export function buildSearchResultPanel() {
    const elSearchResultsPanel = document.getElementById('searchResultsPanel'),
        elSearchResultsShowing = document.getElementById('searchResultsShowing'),
        elSearchResultsCount = document.getElementById('searchResultsCount'),
        elSearchResultsCountPlural = document.getElementById('searchResultsCountPlural'),
        elSearchResultsTerm = document.getElementById('searchResultsTerm'),
        elClearSearch = document.getElementById('clearSearch'),
        elSuccessDetails = document.getElementById('resultsSuccess'),
        elErrorDetails = document.getElementById('resultsError');

    let onClearSearchHandler = noop;
    elClearSearch.addEventListener('click', () => {
        onClearSearchHandler();
    });

    return {
        showSuccess(isShowing, resultCount, searchTerm) {
            setVisible(elErrorDetails, false);
            setVisible(elSuccessDetails, true);
            setVisible(elSearchResultsShowing, isShowing);
            elSearchResultsCount.textContent = resultCount;
            elSearchResultsCountPlural.textContent = pluralise('', resultCount);
            elSearchResultsTerm.textContent = searchTerm;
            setVisible(elSearchResultsPanel, true);
        },
        showError() {
            setVisible(elErrorDetails, true);
            setVisible(elSuccessDetails, false);
            setVisible(elSearchResultsPanel, true);
        },
        hide() {
            setVisible(elSearchResultsPanel, false);
        },
        onClear(handler) {
            onClearSearchHandler = handler;
        }
    }
}

export function buildFilterResultPanel() {
    const elFilterResultsPanel = document.getElementById('filterResultsPanel'),
        elFilterResultsCount = document.getElementById('filterResultsCount'),
        elFilterResultsCountPlural = document.getElementById('filterResultsCountPlural'),
        elFilterResultsHiddenCount = document.getElementById('filterResultsHiddenCount'),
        elFilterResultsSingleApi = document.getElementById('filterResultsSingleApi'),
        elFilterResultsMultipleApis = document.getElementById('filterResultsMultipleApis'),
        elClearFilters = document.getElementById('clearFilters');

    let onClearFiltersHandler = noop;
    elClearFilters.addEventListener('click', () => {
        onClearFiltersHandler();
    });

    return {
        show(showingCount, hiddenCount) {
            elFilterResultsCount.textContent = showingCount;
            elFilterResultsCountPlural.textContent = pluralise('', showingCount);
            elFilterResultsHiddenCount.textContent = hiddenCount;
            setVisible(elFilterResultsSingleApi, hiddenCount === 1);
            setVisible(elFilterResultsMultipleApis, hiddenCount !== 1);
            setVisible(elFilterResultsPanel, true);
        },
        hide() {
            setVisible(elFilterResultsPanel, false);
        },
        onClear(handler) {
            onClearFiltersHandler = handler;
        }
    };
}
