import {noop, normaliseText} from "./utils.js";

export function buildSearch() {
    const elSearchInputBox = document.getElementById('search'),
        elSearchButton = document.getElementById('search_button'),
        elSearchForm = document.getElementById('deepSearch');

    let onSearchHandler = noop;

    return {
        initialise() {
            function triggerSearch(e) {
                e.preventDefault();

                const searchTerm = elSearchInputBox.value,
                    normalisedSearchTerm = normaliseText(searchTerm);

                if (normalisedSearchTerm) {
                    onSearchHandler(searchTerm);
                }
            }

            elSearchForm.addEventListener('submit', triggerSearch);
            elSearchButton.addEventListener('click', triggerSearch);
        },
        onSearch(handler) {
            onSearchHandler = handler;
        },
        clear() {
            elSearchInputBox.value = '';
        },
        get searchTerm() {
            return elSearchInputBox.value;
        }
    };
}
