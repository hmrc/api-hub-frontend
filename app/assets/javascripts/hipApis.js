import {buildPaginator} from './pagination.js';

export function onPageShow() {

    const view = (() => {
        const statusFilterEls = Array.from(document.querySelectorAll('#statusFilters .govuk-checkboxes__input')),
            apiDetailPanelEls = Array.from(document.querySelectorAll('#apiList .api-panel')),
            elSearchResultsSize = document.getElementById('searchResultsSize'),
            elPaginationContainer = document.getElementById('pagination'),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elResetFiltersLink = document.getElementById('resetFilters'),
            elNoResultsResetFiltersLink = document.getElementById('noResultsClearFilters'),
            paginator = buildPaginator(elPaginationContainer),
            domainsView = (() => {
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

                return {
                    setFilterVisibility(apis) {
                        const apiDomainLookup = {};
                        apis
                            .filter(apiDetail => apiDetail.data.domain || apiDetail.data.subdomain)
                            .forEach(apiDetail => {
                                const key = `${apiDetail.data.domain}/${apiDetail.data.subdomain}`;
                                apiDomainLookup[key] = apiDetail.data;
                            });

                        const apiDomains = Object.values(apiDomainLookup);
                        document.querySelectorAll('input.domainFilter').forEach(elDomainCheckbox => {
                            const domain = elDomainCheckbox.value;
                            if (apiDomains.some(api => api.domain === domain)) {
                                domainFilterEls.push(elDomainCheckbox);
                                elDomainCheckbox.addEventListener('change', () => {
                                    const domain = elDomainCheckbox.value,
                                        domainSelected = elDomainCheckbox.checked;
                                    subdomainFilterEls
                                        .filter(elSubdomainCheckbox => elSubdomainCheckbox.dataset['domain'] === domain)
                                        .forEach(elSubdomainCheckbox => {
                                            elSubdomainCheckbox.checked = domainSelected;
                                        });
                                    toggleSubdomainCheckboxes(domain, domainSelected);
                                    onFiltersChangedHandler();
                                });
                                toggleSubdomainCheckboxes(domain, elDomainCheckbox.checked);

                            } else {
                                removeElement(elDomainCheckbox.parentElement);
                                removeElement(document.querySelector(`.subdomainCheckboxes[data-domain="${domain}"]`));
                            }
                        });

                        document.querySelectorAll('input.subDomainFilter').forEach(elSubDomainCheckbox => {
                            const subDomain = elSubDomainCheckbox.value,
                                domain = elSubDomainCheckbox.dataset['domain'];
                            if (apiDomains.some(api => api.domain === domain && api.subdomain === subDomain)) {
                                subdomainFilterEls.push(elSubDomainCheckbox);
                                elSubDomainCheckbox.addEventListener('change', () => {
                                    onFiltersChangedHandler();
                                });
                            } else {
                                removeElement(elSubDomainCheckbox.parentElement);
                            }
                        });

                        const anyDomainsSelected = domainFilterEls.some(el => el.checked);
                        collapseDomainFilterSection(!anyDomainsSelected);

                        updateDomainFilterCount();
                    },
                    onFiltersChanged(handler) {
                        onFiltersChangedHandler = () => {
                            updateDomainFilterCount();
                            handler();
                        };
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
                    },
                    clearAllFilters() {
                        domainFilterEls.forEach(el => {
                            el.checked = false;
                        });
                        subdomainFilterEls.forEach(el => {
                            el.checked = false;
                        });
                        document.querySelectorAll('.subdomainCheckboxes').forEach(el => el.style.display = 'none');
                        collapseDomainFilterSection(true);
                        updateDomainFilterCount();
                    }
                };
            })();

        let externalFilterChangeHandler = () => {};
        function onFiltersChanged() {
            externalFilterChangeHandler();
        }

        statusFilterEls.forEach(elCheckbox => {
            elCheckbox.addEventListener('change', onFiltersChanged);
        });
        domainsView.onFiltersChanged(() => {
            console.log(domainsView.getSelected());
            onFiltersChanged();
        });

        function clearAllFilters() {
            statusFilterEls.forEach(el => {
                el.checked = false;
            });
            domainsView.clearAllFilters();
            onFiltersChanged();
        }

        elResetFiltersLink.addEventListener('click', clearAllFilters);
        elNoResultsResetFiltersLink.addEventListener('click', clearAllFilters);

        return {
            onFiltersChanged(handler) {
                externalFilterChangeHandler = handler;
            },
            onPaginationChanged(handler) {
                paginator.onNavigation(pageNumber => {
                    handler(pageNumber);
                });
            },
            getStatusFilterValues() {
                return statusFilterEls.filter(el => el.checked).map(el => el.value);
            },
            getDomainFilterValues() {
                return domainsView.getSelected();
            },
            getApiDetailPanels() {
                return apiDetailPanelEls;
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    apiDetail.el.style.display = apiDetail.visible ? 'block' : 'none';
                });
            },
            setFilterVisibility(apis) {
                domainsView.setFilterVisibility(apis);
            },
            setResultCount(count) {
                elSearchResultsSize.textContent = count;
            },
            setPagination(currentPage, totalPages) {
                paginator.render(currentPage, totalPages);
            },
            toggleNoResultsPanel(visible) {
                elNoResultsPanel.style.display = visible ? 'block' : 'none';
            },
            clearAllFilters() {
                statusFilterEls.forEach(el => {
                    el.checked = false;
                });
                domainsView.clearAllFilters();
            }
        };
    })();

    function buildFilterFunctions() {
        function buildApiStatusFilterFunction() {
            const selectedStatuses = new Set(view.getStatusFilterValues());
            return data => selectedStatuses.size === 0 || selectedStatuses.has(data.apiStatus);
        }

        function buildDomainFilterFunction() {
            const selectedDomains = view.getDomainFilterValues(),
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
        // add new filters here...

        return [buildApiStatusFilterFunction(), buildDomainFilterFunction()];
    }

    const model = {
        apis: view.getApiDetailPanels().map(el => ({
            data: {
                apiStatus: el.dataset['apistatus'],
                domain: el.dataset['domain'],
                subdomain: el.dataset['subdomain']
                // add other properties that we want to filter on here...
            },
            el,
            hiddenByFilters: false,
            hiddenByPagination: false,
            get visible() {
                return !this.hiddenByFilters && !this.hiddenByPagination;
            }
        })),
        get resultCount() {
            return this.apis.filter(apiDetail => ! apiDetail.hiddenByFilters).length;
        },
        pagination: {
            currentPage: 1,
            get totalPages() {
                return Math.ceil(model.resultCount / this.itemsPerPage);
            },
            get itemsPerPage() {
                return 15;
            }
        }
    };

    function setPaginationPageNumber(pageNumber) {
        model.pagination.currentPage = pageNumber;

        const {currentPage, itemsPerPage, totalPages} = model.pagination,
            startIndex = (currentPage - 1) * itemsPerPage,
            endIndex = startIndex + itemsPerPage;

        model.apis
            .filter(apiDetail => ! apiDetail.hiddenByFilters)
            .forEach((apiDetail, index) => {
                apiDetail.hiddenByPagination = index < startIndex || index >= endIndex;
            });

        view.setApiPanelVisibility(model.apis);
        view.setPagination(currentPage, totalPages);
    }

    view.onPaginationChanged(setPaginationPageNumber);

    function applyFilters() {
        const filterFns = buildFilterFunctions();
        model.apis.forEach(apiDetail => {
            apiDetail.hiddenByFilters = ! filterFns.every(fn => fn(apiDetail.data));
        });

        setPaginationPageNumber(1);

        view.setResultCount(model.resultCount);
        view.toggleNoResultsPanel(model.resultCount === 0);
    }

    view.onFiltersChanged(() => {
        applyFilters();
    });

    view.setFilterVisibility(model.apis);

    applyFilters();
}

if (typeof window !== 'undefined') {
    window.addEventListener('pageshow', onPageShow);
}
