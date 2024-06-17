import {buildPaginator} from './pagination.js';
import {setVisible} from "./utils.js";

export function onDomLoaded() {
    const view = (() => {
        const appDetailPanelEls = Array.from(document.querySelectorAll('#appDetailPanels .hip-application')),
            elPaginationContainer = document.getElementById('pagination'),
            elDisplayCountMessage = document.getElementById('displayCountMessage'),
            elDisplayCountShowing = document.getElementById('displayCount'),
            elDisplayCountTotal = document.getElementById('totalCount'),
            paginator = buildPaginator(elPaginationContainer);

        return {
            getAppDetailPanels() {
                return appDetailPanelEls;
            },
            setPagination(currentPage, totalPages) {
                paginator.render(currentPage, totalPages);
            },
            onPaginationChanged(handler) {
                paginator.onNavigation(pageNumber => {
                    handler(pageNumber);
                });
            },
            setAppPanelVisibility(apps) {
                apps.forEach(appDetail => {
                    setVisible(appDetail.el, appDetail.visible);
                });
            },
            setDisplayCountVisibility(isVisible) {
                setVisible(elDisplayCountMessage, isVisible);
            },
            setDisplayCount(showingCount, totalCount) {
                elDisplayCountShowing.textContent = showingCount;
                elDisplayCountTotal.textContent = totalCount;
            }
        };
    })();

    const model = {
        apps: view.getAppDetailPanels().map(el => ({
            el,
            hiddenByPagination: false,
            get visible() {
                return !this.hiddenByPagination;
            }
        })),
        get totalAppCount() {
            return this.apps.length;
        },
        get visibleAppCount() {
            return this.apps.filter(appDetail => appDetail.visible).length;
        },
        pagination: {
            currentPage: 1,
            get totalPages() {
                return Math.ceil(model.totalAppCount / this.itemsPerPage);
            },
            get itemsPerPage() {
                return 20;
            }
        }
    };

    function updateDisplayCount() {
        view.setDisplayCountVisibility(model.pagination.totalPages > 1);
        view.setDisplayCount(model.visibleAppCount, model.totalAppCount);
    }

    function setPaginationPageNumber(pageNumber) {
        model.pagination.currentPage = pageNumber;

        const {currentPage, itemsPerPage, totalPages} = model.pagination,
            startIndex = (currentPage - 1) * itemsPerPage,
            endIndex = startIndex + itemsPerPage;

        model.apps
            .forEach((appDetail, index) => {
                appDetail.hiddenByPagination = index < startIndex || index >= endIndex;
            });

        view.setAppPanelVisibility(model.apps);
        view.setPagination(currentPage, totalPages);
    }

    setPaginationPageNumber(1);
    updateDisplayCount();

    view.onPaginationChanged(pageNumber => {
        setPaginationPageNumber(pageNumber);
        updateDisplayCount();
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
