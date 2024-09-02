import {buildPaginationView} from './paginationView.js';
import {noop, normaliseText, setVisible} from "./utils.js";

export const HIDDEN_BY_PAGINATION = 'hiddenByPagination';

function defaultItemVisibilityHandler(itemsVisibility) {
    itemsVisibility.forEach(([item, isVisible]) => {
        setVisible(item, isVisible);
    });
}

export function buildPaginator(itemsPerPage, itemVisibilityHandler = defaultItemVisibilityHandler) {
    const view = buildPaginationView(),
        model = {};

    view.onNextLinkClick(() => {
        if (model.currentPage < model.totalPages) {
            model.currentPage++;
            applyPagination();
        }
    });

    view.onPreviousLinkClick(() => {
        if (model.currentPage > 1) {
            model.currentPage--;
            applyPagination();
        }
    });

    view.onPageNumberLinkClick(pageNumber => {
        if (pageNumber > 0 && pageNumber <= model.totalPages && pageNumber !== model.currentPage) {
            model.currentPage = pageNumber;
            applyPagination();
        }
    });

    function applyPagination() {
        const onLastPage = model.currentPage === model.totalPages,
            visibleItemsCount = onLastPage ? model.items.length - itemsPerPage * (model.totalPages - 1) : itemsPerPage,
            totalItemsCount = model.items.length;

        view.render(model.currentPage, model.totalPages, visibleItemsCount, totalItemsCount);

        const startIndex = (model.currentPage - 1) * itemsPerPage,
            endIndex = startIndex + itemsPerPage;

        itemVisibilityHandler(model.items.map((item, index) => [item, index >= startIndex && index < endIndex]));
    }

    return {
        render(items) {
            model.items = items;
            model.currentPage = 1;
            model.totalPages = Math.ceil(model.items.length / itemsPerPage);

            applyPagination();
        }
    };
}

export function onPageShow(panelSelector, countId, filterFields, paginationSize) {
    const view = (() => {
        const apiDetailPanelEls = Array.from(document.querySelectorAll(panelSelector)),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elApiCount = document.getElementById(countId),
            elNameFilter = document.getElementById('nameFilter');

        let onFiltersChangedHandler = noop;

        elNameFilter ? elNameFilter.addEventListener('input', () => {
            onFiltersChangedHandler();
        }) : noop;

        return {
            get apiDetailPanels() {
                return [...apiDetailPanelEls];
            },
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            setApiPanelVisibility(apis) {
                apis.forEach(apiDetail => {
                    setVisible(apiDetail.el, !apiDetail.hiddenByFilter);
                });
            },
            get nameFilterValue() {
                return elNameFilter.value;
            },
            toggleNoResultsPanel(visible) {
                setVisible(elNoResultsPanel, visible);
            },
            set resultCount(value) {
                elApiCount.textContent = value;
            }
        };
    })();

    if (view.apiDetailPanels?.length > 0) {
        const apiPanels = view.apiDetailPanels.map(el => (Object.assign({
                el,
                hiddenByFilter: false
            }, Object.fromEntries(filterFields.map(f => [f, el.dataset[f]]))
        )));
        const paginator = buildPaginator(paginationSize);

        function applyFilter() {
            const normalisedNameFilterValue = normaliseText(view.nameFilterValue);
            apiPanels.forEach(apiDetail => {
                apiDetail.hiddenByFilter = !filterFields.some(filter =>
                    normaliseText(apiDetail[filter]).includes(normalisedNameFilterValue)
                );
            });

            const filteredPanels = apiPanels.filter(apiDetail => ! apiDetail.hiddenByFilter);
            view.setApiPanelVisibility(apiPanels);
            paginator.render(filteredPanels.map(panel => panel.el));

            const resultCount = filteredPanels.length;
            view.toggleNoResultsPanel(resultCount === 0);
            view.resultCount = resultCount;
        }

        view.onFiltersChanged(() => {
            applyFilter();
        });

        paginator.render(apiPanels.map(o => o.el));
        applyFilter();
    }
}

if (typeof window !== 'undefined') {
    window.addEventListener(
        "pageshow",
        () => onPageShow('#appDetailPanels .hip-application', 'appCount', ['applicationName', 'applicationId'], 2)
    );
}