import {addIntersectionMethodToSet, noop, removeElement} from "./utils.js";

addIntersectionMethodToSet();

export function buildPlatformFilters() {
    const elViewNonSelfServePlatformFilters = document.getElementById('viewPlatformFilters'),
        elShowSelfServe = document.getElementById('filterPlatformSelfServe'),
        elShowNonSelfServe = document.getElementById('filterPlatformNonSelfServe'),
        selfServePlatformsInUseByApis = new Set(),
        nonSelfServePlatformsInUseByApis = new Set();

    let onFiltersChangedHandler = noop;

    function getPlatformFilterCheckboxes() {
        return [...document.querySelectorAll('input.platformFilter')];
    }
    function forEachPlatformFilterCheckbox(fn) {
        return getPlatformFilterCheckboxes().forEach(fn);
    }

    function collapseNonSelfServeFilterSection(isCollapsed) {
        if (isCollapsed) {
            elViewNonSelfServePlatformFilters.removeAttribute('open');
        } else {
            elViewNonSelfServePlatformFilters.setAttribute('open', 'open');
        }
    }

    function removeUnusedCheckboxes() {
        // Initially we have one checkbox for each platform, but we only want to show non-self-serve platforms that are in use by at least one API
        forEachPlatformFilterCheckbox(el => {
            const platform = el.value;
            if (! nonSelfServePlatformsInUseByApis.has(platform)) {
                removeElement(el.parentElement);
            }
        });
    }

    function setupCheckboxes(){
        forEachPlatformFilterCheckbox(el => {
            el.addEventListener('change', onFiltersChangedHandler);
        });

        elShowSelfServe.addEventListener('change', onFiltersChangedHandler);

        elShowNonSelfServe.addEventListener('change', event => {
            const showNonSelfServe = event.target.checked;
            collapseNonSelfServeFilterSection(!showNonSelfServe);
            if (showNonSelfServe) {
                forEachPlatformFilterCheckbox(el => el.checked = true);
            }

            onFiltersChangedHandler();
        });
    }

    function getSelected(){
        const showSelfServe = elShowSelfServe.checked,
            showNonSelfServe = elShowNonSelfServe.checked,
            selections = [];

        if (showSelfServe) {
            selections.push(...selfServePlatformsInUseByApis);
        }
        if (showNonSelfServe) {
            const checkedValues = getPlatformFilterCheckboxes().filter(el => el.checked).map(el => el.value);

            if (checkedValues.length === 0) {
                // If all checkboxes in the list are unchecked, then we should assume that the user wants to see all non-self-serve platforms
                selections.push(...nonSelfServePlatformsInUseByApis);
            } else {
                selections.push(...checkedValues);
            }
        }

        return new Set(selections);
    }

    return {
        initialise(apis) {
            const platformsInUseByApis= new Set(apis.map(api => api.data.platform)),
                allPlatformDetails = [...document.querySelectorAll('input.platformFilter')].map(el => ({code: el.value, isSelfServe: el.dataset.selfserve === 'true'})),
                allSelfServePlatforms = new Set(),
                allNonSelfServePlatforms = new Set();

            allPlatformDetails.forEach(platform => {
                if (platform.isSelfServe) {
                    allSelfServePlatforms.add(platform.code);
                } else {
                    allNonSelfServePlatforms.add(platform.code);
                }
            });

            allSelfServePlatforms.intersection(platformsInUseByApis).forEach(platform => selfServePlatformsInUseByApis.add(platform));
            allNonSelfServePlatforms.intersection(platformsInUseByApis).forEach(platform => nonSelfServePlatformsInUseByApis.add(platform));

            removeUnusedCheckboxes();
            setupCheckboxes();
        },
        onChange(handler) {
            onFiltersChangedHandler = () => {
                handler();
            };
        },
        clear() {
            elShowSelfServe.checked = true;
            elShowNonSelfServe.checked = false;
            collapseNonSelfServeFilterSection(true);
        },
        buildFilterFunction() {
            const selectedPlatforms = getSelected(),
                noPlatformsSelected = selectedPlatforms.size === 0;

            return data => noPlatformsSelected || selectedPlatforms.has(data.platform);
        }
    };
}
