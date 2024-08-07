import {setVisible, noop} from "./utils.js";

export function buildSearchResultPanel() {
    const elSearchResultsPanel = document.getElementById('searchResultsPanel'),
        elSearchResultsShowing = document.getElementById('searchResultsShowing'),
        elSearchResultsCount = document.getElementById('searchResultsCount'),
        elSearchResultsCountPlural = document.getElementById('searchResultsCountPlural'),
        elSearchResultsTerm = document.getElementById('searchResultsTerm'),
        elClearSearch = document.getElementById('clearSearch');

    let onClearSearchHandler = noop;
    elClearSearch.addEventListener('click', () => {
        onClearSearchHandler();
    });

    return {
        show(isShowing, resultCount, searchTerm) {
            setVisible(elSearchResultsShowing, isShowing);
            elSearchResultsCount.textContent = resultCount;
            elSearchResultsCountPlural.textContent = resultCount === 1 ? '' : 's';
            elSearchResultsTerm.textContent = searchTerm;
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
            elFilterResultsCountPlural.textContent = showingCount === 1 ? '' : 's';
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
