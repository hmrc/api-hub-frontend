import {addIntersectionMethodToSet, noop, setVisible} from "./utils.js";

addIntersectionMethodToSet();

export function buildHodsFilters() {
    const hodFilterEls = [],
        elHodFiltersContainer = document.getElementById('hodFilters'),
        elHodFilterCount = document.getElementById('hodFilterCount'),
        elViewHodFilters = document.getElementById('viewHodFilters');

    let onFiltersChangedHandler = noop;

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

    function setCheckboxVisibility(apiHods) {
        hodFilterEls.length = 0;
        document.querySelectorAll('input.hodFilter').forEach(elHodsCheckbox => {
            const hod = elHodsCheckbox.value,
                hodInUse = apiHods.has(hod);
            setVisible(elHodsCheckbox.parentElement, hodInUse);
            if (hodInUse) {
                hodFilterEls.push(elHodsCheckbox);
            }
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
            document.querySelectorAll('input.hodFilter')
                .forEach(elHodCheckbox => {
                    elHodCheckbox.addEventListener('change', () => {
                        onFiltersChangedHandler();
                    });
                });
            this.syncWithApis(apis);
        },
        syncWithApis(apis) {
            const apiHods = getHodsInUseByApis(apis);
            setCheckboxVisibility(apiHods);

            const anyHodsInUse = hodFilterEls.length > 0;
            setVisible(elHodFiltersContainer, anyHodsInUse);
            if (anyHodsInUse) {
                const anyHodsSelected = hodFilterEls.some(el => el.checked);
                collapseHodFilterSection(!anyHodsSelected);
                updateHodFilterCount();
            }
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
