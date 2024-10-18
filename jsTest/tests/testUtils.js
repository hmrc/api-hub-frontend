export function isVisible(el) {
    return !el.classList.contains('govuk-!-display-none');
}

export const paginationContainerHtml = `
    <div id="paginationContainer">
        <strong class="hip-pagination__showing-count"></strong>
        <strong class="hip-pagination__total-count"></strong>
    
        <nav>
            <div class="govuk-pagination__prev"></div>
            <ul class="govuk-pagination__list">
                <li class="govuk-pagination__item">
                    <a class="govuk-pagination__link"></a>
                </li>
                <li class="govuk-pagination__item govuk-pagination__item--current">
                    <a class="govuk-pagination__link"></a>
                </li>
                <li class="govuk-pagination__item govuk-pagination__item--ellipses"></li>            
            </ul>
            <div class="govuk-pagination__next"></div>
        </nav>
    </div>
`;

export function arrayFromTo(from, to) {
    return Array.from({length: to - from + 1}, (_, i) => i + from);
}

export async function waitFor(fnTest, expectedResult) {
    // by default this will timeout after 5 seconds
    await new Promise(resolve => {
        function poll() {
            const result = fnTest();
            if (result === expectedResult) {
                resolve();
            } else {
                setTimeout(poll, 50);
            }
        }
        poll();
    });
}

export const paginationHelper = {
    paginationIsAvailable() {
        return isVisible(document.getElementById('paginationContainer'));
    },
    getCurrentPageNumber() {
        return parseInt(document.querySelector('.govuk-pagination__item--current').textContent);
    },
    getShowingCount() {
        return Number(document.querySelector('.hip-pagination__showing-count').textContent);
    },
    getTotalCount() {
        return Number(document.querySelector('.hip-pagination__total-count').textContent);
    },
    getPaginationPageLink(pageNumber) {
        return document.querySelector(`#paginationContainer a[data-page="${pageNumber}"]`);
    },
    getVisiblePanelData(selector, ...props) {
        return Array.from(document.querySelectorAll(selector))
            .filter(isVisible)
            /* jshint -W119 */
            .map(el => props.reduce((acc, prop) => ({...acc, [prop]: el.dataset[prop]}), {id: parseInt(el.dataset.id)}));
            /* jshint +W119 */
    },
    getVisiblePanelIndexes(selector) {
        return this.getVisiblePanelData(selector, 'index').map(({index}) => parseInt(index));
    },
    getPageNumberLinks() {
        return [...document.querySelectorAll('.govuk-pagination__item a')].map(el => parseInt(el.dataset.page));
    },
    getCurrentPageLinkNumber() {
        return parseInt(document.querySelector('.govuk-pagination__item--current a').dataset.page);
    },
    nextLink() {
        return document.querySelector('.govuk-pagination__next ');
    },
    clickNext(){
        this.nextLink().click();
    },
    previousLink() {
        return document.querySelector('.govuk-pagination__prev');
    },
    clickPrevious() {
        this.previousLink().click();
    }
};

export function buildFakeAceEditor()  {
    const editor = jasmine.createSpyObj({
        setOption: null,
        setTheme: null,
    });
    let currentValue = '';
    editor.session = jasmine.createSpyObj({setMode: null});
    editor.setValue = value => currentValue = value;
    editor.getValue = () => currentValue;
    const fakeAce = {
        edit: () => editor,
    };
    return fakeAce;
}

export function buildFakeClipboard(dom) {
    Object.defineProperty(globalThis, 'navigator', {
        value: dom.window.navigator,
        writable: true
    });

    let clipboardContents = null;
    Object.defineProperty(globalThis.navigator, 'clipboard', {
        value: {
            writeText: text => {
                clipboardContents = text;
                return Promise.resolve();
            }
        }
    });

    return {
        getContents() {
            return clipboardContents;
        }
    };
}
