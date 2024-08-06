export function buildModel(apiDetailPanels) {
    const apis =  apiDetailPanels.map(el => ({
            data: {
                apiStatus: el.dataset['apistatus'],
                domain: el.dataset['domain'],
                subdomain: el.dataset['subdomain'],
                hods: new Set(el.dataset['hods'].split(',').filter(h => h)),
                platform: el.dataset['platform']
            },
            el,
            hiddenByFilters: false,
            hiddenBySearch: false,
            hiddenByPagination: false,
            get visible() {
                return !this.hiddenByFilters && !this.hiddenBySearch && !this.hiddenByPagination;
            },
            get includeInResults() {
                return !this.hiddenByFilters && !this.hiddenBySearch;
            }
        })),
        elToApiLookup = new Map(apis.map(apiDetail => [apiDetail.el, apiDetail]));

    return {
        apis,
        getApiForElement(el) {
            return elToApiLookup.get(el);
        },
        get resultCount() {
            return apis.filter(apiDetail => apiDetail.includeInResults).length;
        },
        currentSearchText: null
    };
}
