import {noop, normaliseText} from "./utils.js";

export function buildSearch() {
    const elSearchInputBox = document.getElementById('search'),
        elSearchButton = document.getElementById('search_button'),
        elSearchForm = document.getElementById('deepSearch');

    let onSearchHandler = noop;

    return {
        initialise(model) {
            function triggerSearch(e) {
                e.preventDefault();

                const searchTerm = elSearchInputBox.value,
                    normalisedSearchTerm = normaliseText(searchTerm);

                if (normalisedSearchTerm && normaliseText !== model.currentSearchText) {
                    onSearchHandler(model.currentSearchText = searchTerm);
                }
            }

            elSearchForm.addEventListener('submit', triggerSearch);
            elSearchButton.addEventListener('click', triggerSearch);
        },
        onSearch(handler) {
            onSearchHandler = handler;
        }
    };
}
