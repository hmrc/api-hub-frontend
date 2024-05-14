export function buildPaginator(elContainer) {
    const noop = () => {};

    function buildView() {
        elContainer.classList.add('govuk-pagination');
        elContainer.setAttribute('aria-label', 'Pagination');

        const elPrev = document.createElement('div');
        elPrev.classList.add('govuk-pagination__prev');
        elPrev.innerHTML = `
            <a class="govuk-link govuk-pagination__link" href="#" rel="prev">
              <svg class="govuk-pagination__icon govuk-pagination__icon--prev" xmlns="http://www.w3.org/2000/svg" height="13" width="15" aria-hidden="true" focusable="false" viewBox="0 0 15 13">
                <path d="m6.5938-0.0078125-6.7266 6.7266 6.7441 6.4062 1.377-1.449-4.1856-3.9768h12.896v-2h-12.984l4.2931-4.293-1.414-1.414z"></path>
              </svg>
              <span class="govuk-pagination__link-title">
                Previous<span class="govuk-visually-hidden"> page</span>
              </span>
            </a>`;

        const elNext = document.createElement('div');
        elNext.classList.add('govuk-pagination__next');
        elNext.innerHTML = `
            <a class="govuk-link govuk-pagination__link" href="#" rel="next">
              <span class="govuk-pagination__link-title">
                Next<span class="govuk-visually-hidden"> page</span>
              </span>
              <svg class="govuk-pagination__icon govuk-pagination__icon--next" xmlns="http://www.w3.org/2000/svg" height="13" width="15" aria-hidden="true" focusable="false" viewBox="0 0 15 13">
                <path d="m8.107-0.0078125-1.4136 1.414 4.2926 4.293h-12.986v2h12.896l-4.1855 3.9766 1.377 1.4492 6.7441-6.4062-6.7246-6.7266z"></path>
              </svg>
            </a>`;

        const elList = document.createElement('ul');
        elList.classList.add('govuk-pagination__list');

        function buildPageNumberLink(pageNumber, isCurrentPage) {
            return `
                <li class="govuk-pagination__item ${isCurrentPage ? 'govuk-pagination__item--current' : ''}">
                    <a class="govuk-link govuk-pagination__link" data-page="${pageNumber}" href="#" aria-label="Page ${pageNumber}" ${isCurrentPage ? 'aria-current="page"' : ''}>${pageNumber}</a>
                </li>`;
        }

        const ELLIPSIS = '<li class="govuk-pagination__item govuk-pagination__item--ellipses">&ctdot;</li>';

        function setVisible(el, isVisible) {
            el.style.display = isVisible ? 'block' : 'none';
        }

        let pageNumberClickHandler = noop,
            previousLinkClickHandler = noop,
            nextLinkClickHandler = noop;

        elList.addEventListener('click', event => pageNumberClickHandler(event));
        elPrev.addEventListener('click', event => previousLinkClickHandler(event));
        elNext.addEventListener('click', event => nextLinkClickHandler(event));

        let initialised = false;
        function initialise() {
            elContainer.appendChild(elPrev);
            elContainer.appendChild(elList);
            elContainer.appendChild(elNext);

            initialised = true;
        }

        return {
            render(currentPage, totalPages) {
                if (!initialised) {
                    initialise();
                }
                setVisible(elContainer, totalPages > 1);
                setVisible(elPrev, currentPage > 1);
                setVisible(elNext, currentPage < totalPages);

                const items = [];
                for (let i = 1; i <= totalPages; i++) {
                    // We always show links to the first and last pages, and to the current page and its immediate neighbours
                    if (i === 1 || i === totalPages || Math.abs(i - currentPage) < 2) {
                        items.push(buildPageNumberLink(i, i === currentPage));
                    } else if (items[items.length - 1] !== ELLIPSIS) {
                        items.push(ELLIPSIS);
                    }
                }
                elList.innerHTML = items.join('');
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

    const model = {currentPage: null, totalPages: null},
        view = buildView();
    let navigationHandler = noop;

    view.onPreviousLinkClick(() => {
        if (model.currentPage > 1) {
            navigationHandler(model.currentPage - 1);
        }
    });

    view.onNextLinkClick(() => {
        if (model.currentPage < model.totalPages) {
            navigationHandler(model.currentPage + 1);
        }
    });

    view.onPageNumberLinkClick(event => {
        const pageNumber = parseInt(event.target.dataset.page);
        if (pageNumber && pageNumber !== model.currentPage) {
            navigationHandler(pageNumber);
        }
    });

    return {
        render(currentPage, totalPages) {
            model.currentPage = currentPage;
            model.totalPages = totalPages;
            view.render(currentPage, totalPages);
        },
        onNavigation(handler) {
            navigationHandler = handler;
        }
    };
}
