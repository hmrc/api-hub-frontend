
export function buildDomainFilters() {
    const domainFilterEls = [],
        subdomainFilterEls = [],
        elDomainFilterCount = document.getElementById('domainFilterCount'),
        elViewDomainFilters = document.getElementById('viewDomainFilters');

    let onFiltersChangedHandler = () => {};

    function toggleSubdomainCheckboxes(domain, visible) {
        document.querySelector(`.subdomainCheckboxes[data-domain="${domain}"]`).style.display = visible ? 'block' : 'none';
    }

    function removeElement(el) {
        el.parentElement.removeChild(el);
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

    function removeUnusedCheckboxes(apiDomains) {
        document.querySelectorAll('input.domainFilter').forEach(elDomainCheckbox => {
            const domain = elDomainCheckbox.value;
            if (! apiDomains.some(api => api.domain === domain)) {
                removeElement(elDomainCheckbox.parentElement);
                removeElement(document.querySelector(`.subdomainCheckboxes[data-domain="${domain}"]`));
            }
        });

        document.querySelectorAll('input.subDomainFilter').forEach(elSubDomainCheckbox => {
            const subDomain = elSubDomainCheckbox.value,
                domain = elSubDomainCheckbox.dataset['domain'];
            if (! apiDomains.some(api => api.domain === domain && api.subdomain === subDomain)) {
                removeElement(elSubDomainCheckbox.parentElement);
            }
        });
    }

    function setupCheckboxes() {
        document.querySelectorAll('input.domainFilter').forEach(elDomainCheckbox => {
            const domain = elDomainCheckbox.value;
            domainFilterEls.push(elDomainCheckbox);
            elDomainCheckbox.addEventListener('change', () => {
                const isDomainSelected = elDomainCheckbox.checked;
                subdomainFilterEls
                    .filter(elSubdomainCheckbox => elSubdomainCheckbox.dataset['domain'] === domain)
                    .forEach(elSubdomainCheckbox => {
                        elSubdomainCheckbox.checked = isDomainSelected;
                    });
                toggleSubdomainCheckboxes(domain, isDomainSelected);
                onFiltersChangedHandler();
            });
            toggleSubdomainCheckboxes(domain, elDomainCheckbox.checked);
        });

        document.querySelectorAll('input.subDomainFilter').forEach(elSubDomainCheckbox => {
            subdomainFilterEls.push(elSubDomainCheckbox);
            elSubDomainCheckbox.addEventListener('change', () => {
                onFiltersChangedHandler();
            });
        });
    }

    return {
        initialiseFromApis(apis) {
            const apiDomains = getDomainsInUseByApis(apis);
            removeUnusedCheckboxes(apiDomains);
            setupCheckboxes();

            const anyDomainsSelected = domainFilterEls.some(el => el.checked);
            collapseDomainFilterSection(!anyDomainsSelected);
            updateDomainFilterCount();
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
            document.querySelectorAll('.subdomainCheckboxes').forEach(el => el.style.display = 'none');
            collapseDomainFilterSection(true);
            updateDomainFilterCount();
        },
        getSelected() {
            const selections = {};
            domainFilterEls.filter(el => el.checked).forEach(el => {
                const domain = el.value;
                selections[domain] = subdomainFilterEls
                    .filter(el => el.dataset['domain'] === domain && el.checked)
                    .map(el => el.value);
            });
            return selections;
        }
    };
}
