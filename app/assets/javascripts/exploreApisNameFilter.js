import {noop} from "./utils.js";

export function buildNameFilter() {
    const elFilterText = document.getElementById('nameFilter');

    let onFiltersChangedHandler = noop;

    function normalise(value) {
        return value.trim().toLowerCase();
    }

    return {
        initialise() {
            elFilterText.addEventListener('input', onFiltersChangedHandler);
        },
        onChange(handler) {
            onFiltersChangedHandler = handler;
        },
        clear() {
            elFilterText.value = '';
        },
        buildFilterFunction() {
            const normalisedValue = normalise(elFilterText.value);

            return data => data.apiName.includes(normalisedValue);
        }
    };
}
