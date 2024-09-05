import {noop, setVisible} from "./utils.js";

export function buildDomainFilters() {
    const domainFilterEls = [],
        subdomainFilterEls = [],
        elDomainFiltersContainer = document.getElementById('domainFilters'),
        elDomainFilterCount = document.getElementById('domainFilterCount'),
        elViewDomainFilters = document.getElementById('viewDomainFilters');

    let onFiltersChangedHandler = noop;

    function toggleSubdomainCheckboxes(domain, visible) {
        setVisible(document.querySelector(`.subdomainCheckboxes[data-domain="${domain}"]`), visible);
    }

    function updateDomainFilterCount() {
        const selectionCount = document.querySelectorAll('#domainFilters input:checked').length;
        elDomainFilterCount.textContent = '' + selectionCount;
    }

    function collapseDomainFilterSection(isCollapsed) {
        if (isCollapsed) {
            elViewDomainFilters.removeAttribute('open');
        } else {
            elViewDomainFilters.setAttribute('open', 'open');
        }
    }

    function getDomainsInUseByApis(apis) {
        const apiDomainLookup = {};
        apis
            .filter(apiDetail => apiDetail.data.domain || apiDetail.data.subdomain)
            .forEach(apiDetail => {
                const key = `${apiDetail.data.domain}/${apiDetail.data.subdomain}`;
                apiDomainLookup[key] = {
                    domain: apiDetail.data.domain,
                    subdomain: apiDetail.data.subdomain
                };
            });

        return Object.values(apiDomainLookup);
    }

    function setCheckboxVisibility(apiDomains) {
        subdomainFilterEls.length = 0;
        document.querySelectorAll('input.subDomainFilter').forEach(elSubDomainCheckbox => {
            const subDomain = elSubDomainCheckbox.value,
                domain = elSubDomainCheckbox.dataset.domain,
                subDomainInUse = apiDomains.some(api => api.domain === domain && api.subdomain === subDomain);

            setVisible(elSubDomainCheckbox.parentElement, subDomainInUse);
            if (subDomainInUse) {
                subdomainFilterEls.push(elSubDomainCheckbox);
            }
        });

        domainFilterEls.length = 0;
        document.querySelectorAll('input.domainFilter').forEach(elDomainCheckbox => {
            const domain = elDomainCheckbox.value,
                domainInUse = apiDomains.some(api => api.domain === domain);

            setVisible(elDomainCheckbox.parentElement, domainInUse);

            if (domainInUse) {
                domainFilterEls.push(elDomainCheckbox);
            }
            toggleSubdomainCheckboxes(domain, elDomainCheckbox.checked);
        });
    }

    function getSelected() {
        const selections = {};
        domainFilterEls.filter(el => el.checked).forEach(el => {
            const domain = el.value;
            selections[domain] = subdomainFilterEls
                .filter(el => el.dataset.domain === domain && el.checked)
                .map(el => el.value);
        });
        return selections;
    }

    return {
        initialise(apis) {
            document.querySelectorAll('input.domainFilter').forEach(elDomainCheckbox => {
                const domain = elDomainCheckbox.value;
                elDomainCheckbox.addEventListener('change', () => {
                    const isDomainSelected = elDomainCheckbox.checked;
                    subdomainFilterEls
                        .filter(elSubdomainCheckbox => elSubdomainCheckbox.dataset.domain === domain)
                        .forEach(elSubdomainCheckbox => {
                            elSubdomainCheckbox.checked = isDomainSelected;
                        });
                    toggleSubdomainCheckboxes(domain, isDomainSelected);
                    onFiltersChangedHandler();
                });
            });

            document.querySelectorAll('input.subDomainFilter').forEach(elSubDomainCheckbox => {
                elSubDomainCheckbox.addEventListener('change', () => {
                    onFiltersChangedHandler();
                });
            });
            this.syncWithApis(apis);
        },
        syncWithApis(apis) {
            const apiDomains = getDomainsInUseByApis(apis);
            setCheckboxVisibility(apiDomains);

            const anyDomainsInUse = domainFilterEls.length > 0;
            setVisible(elDomainFiltersContainer, anyDomainsInUse);
            if (anyDomainsInUse) {
                const anyDomainsSelected = domainFilterEls.some(el => el.checked);
                collapseDomainFilterSection(!anyDomainsSelected);
                updateDomainFilterCount();
            }
        },
        onChange(handler) {
            onFiltersChangedHandler = () => {
                updateDomainFilterCount();
                handler();
            };
        },
        clear() {
            domainFilterEls.forEach(el => {
                el.checked = false;
            });
            subdomainFilterEls.forEach(el => {
                el.checked = false;
            });
            document.querySelectorAll('.subdomainCheckboxes').forEach(el => setVisible(el, false));
            collapseDomainFilterSection(true);
            updateDomainFilterCount();
        },
        buildFilterFunction() {
            const selectedDomains = getSelected(),
                noDomainsSelected = Object.keys(selectedDomains).length === 0;

            return data => {
                if (noDomainsSelected) {
                    return true;
                } else if (selectedDomains[data.domain]) {
                    const noSubDomainsSelected = selectedDomains[data.domain].length === 0;
                    return noSubDomainsSelected || selectedDomains[data.domain].includes(data.subdomain);
                }
                return false;
            };
        }
    };
}
