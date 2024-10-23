import {noop, setVisible} from "./utils.js";

function buildView() {
    const elAddPrefixButton = document.getElementById('addPrefix'),
        elPrefixInput = document.getElementById('prefix'),
        elPrefixesTable = document.getElementById('prefixesTable'),
        elPrefixesTableBody = elPrefixesTable.querySelector('tbody'),
        elAddMappingButton = document.getElementById('addMapping'),
        elExistingInput = document.getElementById('existing'),
        elReplacementInput = document.getElementById('replacement'),
        elMappingsTable = document.getElementById('mappingsTable'),
        elMappingsTableBody = elMappingsTable.querySelector('tbody'),
        elFormFields = document.getElementById('formFields');

    let onAddPrefixButtonClicked = noop,
        onAddMappingButtonClicked = noop,
        onRemovePrefixLinkClicked = noop,
        onRemoveMappingLinkClicked = noop;

    elAddPrefixButton.addEventListener('click', () => {
        onAddPrefixButtonClicked(elPrefixInput.value);
    });

    elAddMappingButton.addEventListener('click', () => {
        const existing = elExistingInput.value,
            replacement = elReplacementInput.value;
        onAddMappingButtonClicked({existing, replacement});
    });

    function buildTableRow() {
        const elRow = document.createElement('tr');
        elRow.classList.add('govuk-table__row');
        return elRow;
    }
    function addTableCellToRow(elParentRow) {
        const elCell = document.createElement('td');
        elCell.classList.add('govuk-table__cell');
        elParentRow.appendChild(elCell);
        return elCell;
    }
    function buildRemoveLink(modelIndex) {
        const elLink = document.createElement('a');
        elLink.classList.add('hip-js-link');
        elLink.textContent = 'Remove';
        return elLink;
    }

    function buildPrefixListRow(prefix, modelIndex) {
        const elRow = buildTableRow(),
            elRemoveLink = buildRemoveLink();

        addTableCellToRow(elRow).textContent = prefix;
        addTableCellToRow(elRow).appendChild(elRemoveLink);

        elRemoveLink.addEventListener('click', () => {
            onRemovePrefixLinkClicked(modelIndex);
        });

        return elRow;
    }

    function buildMappingRow(mapping, modelIndex) {
        const {existing, replacement} = mapping,
            elRow = buildTableRow(),
            elRemoveLink = buildRemoveLink();

        addTableCellToRow(elRow).textContent = existing;
        addTableCellToRow(elRow).textContent = replacement;
        addTableCellToRow(elRow).appendChild(elRemoveLink);

        elRemoveLink.addEventListener('click', () => {
            onRemoveMappingLinkClicked(modelIndex);
        });

        return elRow;
    }

    function buildPrefixFormField(prefix) {
        const elField = document.createElement('input');
        elField.type = 'hidden';
        elField.name = 'prefixes[]';
        elField.value = prefix;
        return elField;
    }

    function buildMappingFormField(mapping) {
        const elField = document.createElement('input');
        elField.type = 'hidden';
        elField.name = 'mappings[]';
        elField.value = `${mapping.existing}->${mapping.replacement}`; //TODO change me
        return elField;
    }

    return {
        onAddPrefixButtonClicked(handler) {
            onAddPrefixButtonClicked = handler;
        },
        onRemovePrefixLinkClicked(handler) {
            onRemovePrefixLinkClicked = handler;
        },
        clearPrefix() {
            elPrefixInput.value = '';
        },
        onAddMappingButtonClicked(handler) {
            onAddMappingButtonClicked = handler;
        },
        onRemoveMappingLinkClicked(handler) {
            onRemoveMappingLinkClicked = handler;
        },
        clearMapping() {
            elExistingInput.value = '';
            elReplacementInput.value = '';
        },
        render(model) {
            elFormFields.innerHTML = '';

            setVisible(elPrefixesTable, model.prefixes.length);
            elPrefixesTableBody.innerHTML = '';
            model.prefixes.forEach((prefix, i) => {
                const elRow = buildPrefixListRow(prefix, i);
                elPrefixesTableBody.appendChild(elRow);

                const elPrefixFormField = buildPrefixFormField(prefix);
                elFormFields.appendChild(elPrefixFormField);
            });

            setVisible(elMappingsTable, model.mappings.length);
            elMappingsTableBody.innerHTML = '';
            model.mappings.forEach((mapping, i) => {
                const elRow = buildMappingRow(mapping, i);
                elMappingsTableBody.appendChild(elRow);

                const elMappingFormField = buildMappingFormField(mapping);
                elFormFields.appendChild(elMappingFormField);
            });
        }
    };
}

function buildModel() {
    const prefixes = [],
        mappings = [];

    return {
        addPrefix(prefix) {
            prefixes.push(prefix);
        },
        removePrefixByIndex(index) {
            prefixes.splice(index, 1);
        },
        addMapping(mapping) {
            mappings.push(mapping);
        },
        removeMappingByIndex(index) {
            mappings.splice(index, 1);
        },
        get prefixes() {
            return prefixes;
        },
        get mappings() {
            return mappings;
        }
    };
}

export function onDOMContentLoaded(){
    const view= buildView(),
        model = buildModel();

    view.onAddPrefixButtonClicked(prefix => {
        const trimmedPrefix = prefix.trim();

        if (trimmedPrefix) {
            model.addPrefix(trimmedPrefix);
            view.clearPrefix();
            view.render(model);
        }
    });

    view.onRemovePrefixLinkClicked(modelIndex => {
        model.removePrefixByIndex(modelIndex);
        view.render(model);
    });

    view.onAddMappingButtonClicked(mapping => {
        const {existing, replacement} = mapping,
            trimmedExisting = existing.trim(),
            trimmedReplacement = replacement.trim();

        if (trimmedExisting && trimmedReplacement) {
            model.addMapping({existing: trimmedExisting, replacement: trimmedReplacement});
            view.clearMapping();
            view.render(model);
        }
    })

    view.onRemoveMappingLinkClicked(modelIndex => {
        model.removeMappingByIndex(modelIndex);
        view.render(model);
    });

    view.render(model);

}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onDOMContentLoaded);
}
