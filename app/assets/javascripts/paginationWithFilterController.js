import {buildPaginator} from './paginationController.js';
import {noop, normaliseText, setVisible} from "./utils.js";

export function onPageShow(panelAttribute, countAttribute, filterAttribute, paginationAttribute) {
    const view = (() => {
        const detailsPanelEls = Array.from(document.querySelectorAll(`[${panelAttribute}]`)),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elCount = document.querySelector(`[${countAttribute}]`),
            elNameFilter = document.getElementById('nameFilter');

        let onFiltersChangedHandler = noop;

        elNameFilter ? elNameFilter.addEventListener('input', () => {
            onFiltersChangedHandler();
        }) : noop;

        return {
            get detailPanels() {
                return [...detailsPanelEls];
            },
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            setPanelVisibility(panels) {
                panels.forEach(panel => {
                    setVisible(panel.el, !panel.hiddenByFilter);
                });
            },
            get nameFilterValue() {
                return elNameFilter.value;
            },
            toggleNoResultsPanel(visible) {
                setVisible(elNoResultsPanel, visible);
            },
            panelFilters(panel) {
                return Array.from(panel.dataset[filterAttribute].replaceAll(" ", "").split(","));
            },
            get paginationSize() {
                return parseInt(
                    document.querySelector(`[${paginationAttribute}]`).getAttribute(paginationAttribute)
                );
            },
            set resultCount(value) {
                elCount.textContent = value;
            }
        };
    })();

    if (view.detailPanels?.length > 0) {
        const details = view.detailPanels.map(el => {
            const panelFilters = view.panelFilters(el);
            const filterValues = Object.fromEntries(panelFilters.map(f => [f, el.dataset[f]]));
            return (Object.assign({
                el,
                hiddenByFilter: false,
                filters: panelFilters,
            }, filterValues));
        });
        const paginator = buildPaginator(view.paginationSize);

        function applyFilter() {
            const normalisedNameFilterValue = normaliseText(view.nameFilterValue);
            details.forEach(detail => {
                detail.hiddenByFilter = !detail.filters.some(filter =>
                    normaliseText(detail[filter]).includes(normalisedNameFilterValue)
                );
            });

            const filteredPanels = details.filter(detail => ! detail.hiddenByFilter);
            view.setPanelVisibility(details);
            paginator.render(filteredPanels.map(panel => panel.el));

            const resultCount = filteredPanels.length;
            view.toggleNoResultsPanel(resultCount === 0);
            view.resultCount = resultCount;
        }

        view.onFiltersChanged(() => {
            applyFilter();
        });

        paginator.render(details.map(o => o.el));
        applyFilter();
    }
}

if (typeof window !== 'undefined') {
    window.addEventListener(
        "pageshow",
        () => onPageShow(
                'data-has-pagination',
                'data-paginator-count',
                'filterBy',
                'data-pagination-size'
            )
    );
}