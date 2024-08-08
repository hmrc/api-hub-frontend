import {addIntersectionMethodToSet, noop, setVisible} from "./utils.js";

addIntersectionMethodToSet();

export function buildPlatformFilters() {
    const nonSelfServePlatformFilterEls = [],
        elViewNonSelfServePlatformFilters = document.getElementById('viewPlatformFilters'),
        elShowSelfServe = document.getElementById('filterPlatformSelfServe'),
        elShowNonSelfServe = document.getElementById('filterPlatformNonSelfServe'),
        selfServePlatformsInUseByApis = new Set(),
        nonSelfServePlatformsInUseByApis = new Set();

    let onFiltersChangedHandler = noop;

    function collapseNonSelfServeFilterSection(isCollapsed) {
        if (isCollapsed) {
            elViewNonSelfServePlatformFilters.removeAttribute('open');
        } else {
            elViewNonSelfServePlatformFilters.setAttribute('open', 'open');
        }
    }

    function setNonSelfServeCheckboxVisibility() {
        // Initially we have one checkbox for each platform, but we only want to show non-self-serve platforms that are in use by at least one API
        nonSelfServePlatformFilterEls.length = 0;
        document.querySelectorAll('input.platformFilter').forEach(elPlatformCheckbox => {
            const platform = elPlatformCheckbox.value,
                platformInUse = nonSelfServePlatformsInUseByApis.has(platform);
            setVisible(elPlatformCheckbox.parentElement, platformInUse);
            if (platformInUse) {
                nonSelfServePlatformFilterEls.push(elPlatformCheckbox);
            }
        });
    }

    function syncFiltersWithNonSelfServeCheckbox(checkAllFilters) {
        const showNonSelfServe = elShowNonSelfServe.checked;
        collapseNonSelfServeFilterSection(!showNonSelfServe);
        if (showNonSelfServe && checkAllFilters) {
            nonSelfServePlatformFilterEls.forEach(el => el.checked = true);
        }
    }

    function getSelected(){
        const showSelfServe = elShowSelfServe.checked,
            showNonSelfServe = elShowNonSelfServe.checked,
            selections = [];

        if (showSelfServe) {
            selections.push(...selfServePlatformsInUseByApis);
        }
        if (showNonSelfServe) {
            const checkedValues = nonSelfServePlatformFilterEls.filter(el => el.checked).map(el => el.value);
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
            document.querySelectorAll('input.platformFilter').forEach(el => {
                el.addEventListener('change', () => onFiltersChangedHandler());
            });

            elShowSelfServe.addEventListener('change', () => onFiltersChangedHandler());

            elShowNonSelfServe.addEventListener('change', () => {
                syncFiltersWithNonSelfServeCheckbox(true);
                onFiltersChangedHandler();
            });

            /* When we return to the page via Back button make sure the non-self-serve filter section is collapsed/expanded
            as appropriate but don't automatically check all the checkboxes, we want the state to match what it was before
            the user navigated away */
            syncFiltersWithNonSelfServeCheckbox(false);

            this.syncWithApis(apis);
        },
        syncWithApis(apis) {
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

            selfServePlatformsInUseByApis.clear();
            allSelfServePlatforms.intersection(platformsInUseByApis).forEach(platform => selfServePlatformsInUseByApis.add(platform));

            nonSelfServePlatformsInUseByApis.clear();
            allNonSelfServePlatforms.intersection(platformsInUseByApis).forEach(platform => nonSelfServePlatformsInUseByApis.add(platform));

            setNonSelfServeCheckboxVisibility();
            syncFiltersWithNonSelfServeCheckbox(false);

            elShowSelfServe.disabled = selfServePlatformsInUseByApis.size === 0;
            elShowNonSelfServe.disabled = nonSelfServePlatformsInUseByApis.size === 0;
        },
        onChange(handler) {
            onFiltersChangedHandler = () => {
                handler();
            };
        },
        clear() {
            elShowSelfServe.checked = false;
            elShowNonSelfServe.checked = false;
            syncFiltersWithNonSelfServeCheckbox();
        },
        buildFilterFunction() {
            const selectedPlatforms = getSelected(),
                noPlatformsSelected = selectedPlatforms.size === 0;

            return data => noPlatformsSelected || selectedPlatforms.has(data.platform);
        }
    };
}
