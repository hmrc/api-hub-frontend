import {buildPaginationView} from './paginationView.js';
import {setVisible} from "./utils.js";

export const HIDDEN_BY_PAGINATION = 'hiddenByPagination';

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

    function defaultItemVisibilityHandler(itemsVisibility) {
        itemsVisibility.forEach(([item, isVisible]) => {
            setVisible(item, isVisible);
        });
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