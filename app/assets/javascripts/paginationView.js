import {setVisible, noop} from "./utils.js";

export function buildPaginationView() {
    const elContainer = document.getElementById('paginationContainer'),
        elList = elContainer.querySelector('.govuk-pagination__list'),
        elPrev = elContainer.querySelector('.govuk-pagination__prev'),
        elNext = elContainer.querySelector('.govuk-pagination__next'),
        elPageLink = elContainer.querySelector('.govuk-pagination__item'),
        elPageLinkCurrent = elContainer.querySelector('.govuk-pagination__item--current'),
        elEllipsis = elContainer.querySelector('.govuk-pagination__item--ellipses'),
        elDisplayCount = elContainer.querySelector('.hip-pagination__showing-count'),
        elTotalCount = elContainer.querySelector('.hip-pagination__total-count');

    elList.removeChild(elPageLink);
    elList.removeChild(elPageLinkCurrent);
    elList.removeChild(elEllipsis);

    function buildPageNumberLink(pageNumber, isCurrentPage) {
        const elLinkBox = (isCurrentPage ? elPageLinkCurrent :  elPageLink).cloneNode(true),
            elLink = elLinkBox.querySelector('.govuk-pagination__link');
        elLink.dataset.page = pageNumber;
        elLink.textContent = pageNumber;
        elLink.setAttribute('aria-label', `Page ${pageNumber}`);
        return elLinkBox;
    }

    function buildEllipsis() {
        return elEllipsis.cloneNode(true);
    }

    let pageNumberClickHandler = noop,
        previousLinkClickHandler = noop,
        nextLinkClickHandler = noop;

    elList.addEventListener('click', event => pageNumberClickHandler(Number(event.target.dataset.page)));
    elPrev.addEventListener('click', event => previousLinkClickHandler(event));
    elNext.addEventListener('click', event => nextLinkClickHandler(event));

    return {
        render(currentPage, totalPages, visibleItemsCount, totalItemsCount) {
            setVisible(elContainer, totalPages > 1);
            setVisible(elPrev, currentPage > 1);
            setVisible(elNext, currentPage < totalPages);

            let lastItemWasEllipsis = false;
            elList.innerHTML = '';
            for (let i = 1; i <= totalPages; i++) {
                // We always show links to the first and last pages, and to the current page and its immediate neighbours
                if (i === 1 || i === totalPages || Math.abs(i - currentPage) < 2) {
                    elList.appendChild(buildPageNumberLink(i, i === currentPage));
                    lastItemWasEllipsis = false;
                } else if (! lastItemWasEllipsis) {
                    elList.appendChild(buildEllipsis());
                    lastItemWasEllipsis = true;
                }
            }

            elDisplayCount.textContent = visibleItemsCount;
            elTotalCount.textContent = totalItemsCount;
        },
        onNextLinkClick(handler) {
            nextLinkClickHandler = handler;
        },
        onPreviousLinkClick(handler) {
            previousLinkClickHandler = handler;
        },
        onPageNumberLinkClick(handler) {
            pageNumberClickHandler = handler;
        }
    };
}
