import {buildPaginator} from './paginationController.js';
import {noop, normaliseText, setVisible} from "./utils.js";

function buildFilter(itemEls, elFilterInput, searchableAttributes) {
    let onFiltersChangedHandler = noop;

    elFilterInput.addEventListener('input', () => {
        const matchingEls = applyFilter();
        onFiltersChangedHandler(matchingEls);
    });

    const itemModels =  [...itemEls].map(el => {
        const itemModel = {el, hiddenByFilter: false};
        searchableAttributes.forEach(attr => {
           itemModel[attr] = el.dataset[attr];
        });
        return itemModel;
    });

    function applyFilter() {
        const normalisedFilterValue = normaliseText(elFilterInput.value);
        itemModels.forEach(itemModel => {
            itemModel.hiddenByFilter = ! searchableAttributes.some(attr => normaliseText(itemModel[attr]).includes(normalisedFilterValue));
            setVisible(itemModel.el, ! itemModel.hiddenByFilter);
        });

        return itemModels.filter(apiDetail => ! apiDetail.hiddenByFilter);
    }

    return {
        onChange: onFiltersChangedHandler
    };
}

export function onDomLoaded() {
    const paginator = buildPaginator(10),
        filter = buildFilter(
            document.querySelectorAll('#teamsTable .govuk-table__body .govuk-table__row'),
            document.getElementById('teamFilter'),
            ['name', 'id', 'emails']
        );

    filter.onChange(paginator.render);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
