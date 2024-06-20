import {buildPaginationView} from './paginationView.js';
import {noop} from "./utils.js";

export const HIDDEN_BY_PAGINATION = 'hiddenByPagination';

export function buildPaginator(elNavContainer, itemsPerPage) {
    const view = buildPaginationView(elNavContainer),
        model = {};

    let paginationChangedHandler = noop;

    view.onNavigation(pageNumber => {
        applyPagination(pageNumber);
    });

    function applyPagination(currentPage) {
        view.render(currentPage, Math.ceil(model.items.length / itemsPerPage));

        const startIndex = (currentPage - 1) * itemsPerPage,
            endIndex = startIndex + itemsPerPage;

        model.items.forEach((item, index) => {
            item[HIDDEN_BY_PAGINATION] = index < startIndex || index >= endIndex;
        });
        paginationChangedHandler({
            isPaginating: view.isVisible,
            visibleItemCount: model.items.filter(item => !item[HIDDEN_BY_PAGINATION]).length,
            totalItemCount: model.items.length
        });
    }

    return {
        initialise(items) {
            model.items = items;
            applyPagination(1);
        },
        onPaginationChanged(handler) {
            paginationChangedHandler = handler;
        }
    };
}