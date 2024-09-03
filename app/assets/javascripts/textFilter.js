import {noop, normaliseText, setVisible} from "./utils.js";

export function dataAttribute(attr) {
    function dataAttributeTester(transformFns) {
        function transform(initValue, fnTest) {
            return transformFns.reduce((values, fnTransform) => values.flatMap(fnTransform), [initValue]).some(fnTest)
        }

        return {
            whenNormalised() {
                return dataAttributeTester([...transformFns, val => [normaliseText(val)]]);
            },
            whenSplitBy(separator) {
                return dataAttributeTester([...transformFns, text => text.split(separator)]);
            },
            includesTheFilterText() {
                return (filterText, el) => {
                    return transform(el.dataset[attr], value => value.includes(filterText));
                }
            },
            startsWithTheFilterText() {
                return (filterText, el) => {
                    return transform(el.dataset[attr], value => value.startsWith(filterText));
                }
            },
        }
    }

    return dataAttributeTester([]);
}

export function buildTextFilter(itemEls, elFilterInput, valueFns) {
    let onFiltersChangedHandler = noop;

    elFilterInput.addEventListener('input', applyFilter);

    const itemModels =  [...itemEls].map(el => {
        return {el, hiddenByFilter: false};
    });

    function applyFilter() {
        itemModels.forEach(itemModel => {
            itemModel.hiddenByFilter = ! valueFns.some(fn => fn(elFilterInput.value, itemModel.el));
            setVisible(itemModel.el, ! itemModel.hiddenByFilter);
        });

        const matchingElements = itemModels
            .filter(itemModel => ! itemModel.hiddenByFilter)
            .map(itemModel => itemModel.el);

        onFiltersChangedHandler(matchingElements);
    }

    return {
        onChange(handler) {
            onFiltersChangedHandler = handler;
            applyFilter();
        }
    };
}