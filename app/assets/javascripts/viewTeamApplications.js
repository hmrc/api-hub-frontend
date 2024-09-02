import {buildPaginator} from './paginationController.js';
import {noop, normaliseText, setVisible} from "./utils.js";

export function onPageShow() {
    const view = (() => {
        const appDetailPanelEls = Array.from(document.querySelectorAll('#appDetailPanels .hip-app')),
            elNoResultsPanel = document.getElementById('noResultsPanel'),
            elAppCount = document.getElementById('appCount'),
            elNameFilter = document.getElementById('appFilter');

        let onFiltersChangedHandler = noop;

        elNameFilter.addEventListener('input', () => {
            onFiltersChangedHandler();
        });

        return {
            get appDetailPanels() {
                return [...appDetailPanelEls];
            },
            onFiltersChanged(handler) {
                onFiltersChangedHandler = handler;
            },
            setAppPanelVisibility(apps) {
                apps.forEach(appDetail => {
                    setVisible(appDetail.el, !appDetail.hiddenByFilter);
                });
            },
            get nameFilterValue() {
                return elNameFilter.value;
            },
            toggleNoResultsPanel(visible) {
                setVisible(elNoResultsPanel, visible);
            },
            set resultCount(value) {
                elAppCount.textContent = value;
            }
        };
    })();

    if (view.appDetailPanels.length > 0) {
        const appPanels = view.appDetailPanels.map(el => ({
            el,
            appName: el.dataset['appName'],
            appId: el.dataset['appId'],
            hiddenByFilter: false
        }));

        const paginator = buildPaginator(20);

        function applyFilter() {
            const normalisedNameFilterValue = normaliseText(view.nameFilterValue);
            appPanels.forEach(appDetail => {
                appDetail.hiddenByFilter =
                    !(normaliseText(appDetail.appName).includes(normalisedNameFilterValue)
                        || normaliseText(appDetail.appId).includes(normalisedNameFilterValue))
            });

            const filteredPanels = appPanels.filter(appDetail => ! appDetail.hiddenByFilter);
            view.setAppPanelVisibility(appPanels);
            paginator.render(filteredPanels.map(panel => panel.el));

            const resultCount = filteredPanels.length;
            view.toggleNoResultsPanel(resultCount === 0);
            view.resultCount = resultCount;
        }

        view.onFiltersChanged(() => {
            applyFilter();
        });

        paginator.render(appPanels.map(o => o.el));
        applyFilter();
    }
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}