import {noop} from "./utils.js";

// Modern browsers have Set.intersection but NodeJS only added it fairly recently so we need to provide a polyfill for the build pipeline
if (! Set.prototype.intersection) {
    Set.prototype.intersection = function(that) {
        return new Set([...that].filter(x => this.has(x)));
    }
}

export function buildHodsFilters() {
    const hodFilterEls = [],
        elHodFilterCount = document.getElementById('hodFilterCount'),
        elViewHodFilters = document.getElementById('viewHodFilters');

    let onFiltersChangedHandler = noop;

    function removeElement(el) {
        el.parentElement.removeChild(el);
    }

    function updateHodFilterCount() {
        const selectionCount = document.querySelectorAll('#hodFilters input:checked').length;
        elHodFilterCount.textContent = '' + selectionCount;
    }

    function collapseHodFilterSection(isCollapsed) {
        if (isCollapsed) {
            elViewHodFilters.removeAttribute('open');
        } else {
            elViewHodFilters.setAttribute('open', 'open');
        }
    }

    function getHodsInUseByApis(apis) {
        const hods = new Set();
        apis
            .filter(apiDetail => apiDetail.data.hods)
            .forEach(apiDetail => {
                apiDetail.data.hods.forEach(hod => hods.add(hod));
            });

        return hods;
    }

    function removeUnusedCheckboxes(apiHods) {
        document.querySelectorAll('input.hodFilter').forEach(elHodsCheckbox => {
            const hod = elHodsCheckbox.value;
            if (! apiHods.has(hod)) {
                removeElement(elHodsCheckbox.parentElement);
            }
        });
    }

    function setupCheckboxes() {
        document.querySelectorAll('input.hodFilter').forEach(elHodCheckbox => {
            hodFilterEls.push(elHodCheckbox);
            elHodCheckbox.addEventListener('change', () => {
                onFiltersChangedHandler();
            });
        });
    }

    function getSelected() {
        return new Set (
            hodFilterEls
                .filter(el => el.checked)
                .map(el => el.value)
        );
    }

    return {
        initialise(apis) {
            const apiHods = getHodsInUseByApis(apis);
            removeUnusedCheckboxes(apiHods);
            setupCheckboxes();

            const anyHodsSelected = hodFilterEls.some(el => el.checked);
            collapseHodFilterSection(!anyHodsSelected);
            updateHodFilterCount();
        },
        onChange(handler) {
            onFiltersChangedHandler = () => {
                updateHodFilterCount();
                handler();
            };
        },
        clear() {
            hodFilterEls.forEach(el => {
                el.checked = false;
            });
            collapseHodFilterSection(true);
            updateHodFilterCount();
        },
        buildFilterFunction() {
            const selectedHods = getSelected(),
                noHodsSelected = selectedHods.size === 0;

            return data => noHodsSelected || selectedHods.intersection(data.hods).size > 0;
        }
    };
}
