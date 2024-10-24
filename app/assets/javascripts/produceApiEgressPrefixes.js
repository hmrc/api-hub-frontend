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
        elFormFields = document.getElementById('formFields'),
        elPrefixErrorMessage = document.createElement('p'),
        elMappingExistingErrorMessage = document.createElement('p'),
        elMappingReplacementErrorMessage = document.createElement('p'),
        errorMessageEnterValue = elFormFields.dataset.messageEnterValue,
        errorMessageStartWithSlash = elFormFields.dataset.messageForwardSlash;


    let onAddPrefixButtonClicked = noop,
        onAddMappingButtonClicked = noop,
        onRemovePrefixLinkClicked = noop,
        onRemoveMappingLinkClicked = noop;

    elPrefixErrorMessage.classList.add('govuk-error-message');
    elMappingExistingErrorMessage.classList.add('govuk-error-message');
    elMappingReplacementErrorMessage.classList.add('govuk-error-message');

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

    function setErrorMessage(elInput, elErrorMessage, error) {
        const elParent = elInput.parentElement;
        if (error) {
            elParent.insertBefore(elErrorMessage, elInput.nextSibling);
        } else if (elParent.contains(elErrorMessage)) {
            elParent.removeChild(elErrorMessage);
        }
        elErrorMessage.textContent = error;
        elInput.classList.toggle('govuk-input--error', !! error);
    }

    function errorMessage(elInput, elErrorMessage) {
        return {
            clear() {
                setErrorMessage(elInput, elErrorMessage);
            },
            showEnterValueMessage() {
                setErrorMessage(elInput, elErrorMessage, errorMessageEnterValue);
            },
            showStartWithSlashMessage() {
                setErrorMessage(elInput, elErrorMessage, errorMessageStartWithSlash);
            }
        };
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
        initialiseModel(model) {
            elFormFields.querySelectorAll('input[name=prefixes]').forEach(el => model.addPrefix(el.value));
            elFormFields.querySelectorAll('input[name=mappings]').forEach(el => {
                const [existing, replacement] = el.value.split('->');
                model.addMapping({existing, replacement});
            });
        },
        prefixErrorMessage : errorMessage(elPrefixInput, elPrefixErrorMessage),
        mappingExistingErrorMessage : errorMessage(elExistingInput, elMappingExistingErrorMessage),
        mappingReplacementErrorMessage : errorMessage(elReplacementInput, elMappingReplacementErrorMessage),
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

    view.initialiseModel(model);

    view.onAddPrefixButtonClicked(prefix => {
        const trimmedPrefix = prefix.trim();

        if (trimmedPrefix) {
            model.addPrefix(trimmedPrefix);
            view.clearPrefix();
            view.render(model);
            view.prefixErrorMessage.clear();
        } else {
            view.prefixErrorMessage.showEnterValueMessage();
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

        if (trimmedExisting) {
            view.mappingExistingErrorMessage.clear();
        } else {
            view.mappingExistingErrorMessage.showEnterValueMessage();
        }

        if (trimmedReplacement) {
            view.mappingReplacementErrorMessage.clear();
        } else {
            view.mappingReplacementErrorMessage.showEnterValueMessage();
        }
    });

    view.onRemoveMappingLinkClicked(modelIndex => {
        model.removeMappingByIndex(modelIndex);
        view.render(model);
    });

    view.render(model);
}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onDOMContentLoaded);
}
