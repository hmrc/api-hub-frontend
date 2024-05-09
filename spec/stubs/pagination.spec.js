const buildPaginator = require('../../app/assets/javascripts/pagination');
const jsdom = require("jsdom");

describe('pagination', () => {
    const { JSDOM } = jsdom;
    let elContainer, paginator, document;

    beforeEach(() => {
        const dom = (new JSDOM('<!DOCTYPE html><div id="pg"></div>'))
        document = dom.window.document;
        elContainer = document.getElementById('pg');
        document.body.appendChild(elContainer);
        global.document = document;
        paginator = buildPaginator(elContainer);
    });
    afterEach(() => {
        document.body.removeChild(elContainer);
    });

    it("paginator not visible if only 1 page of results", async () => {
        paginator.render(1, 1);
        expect(elContainer.style.display).toEqual('none');
    });

    it("previous link not visible when on first page", async () => {
        paginator.render(1, 10);
        expect(elContainer.querySelector('.govuk-pagination__prev').style.display).toEqual('none');
    })

    it("previous link is visible when on second page", async () => {
        paginator.render(2, 10);
        expect(elContainer.querySelector('.govuk-pagination__prev').style.display).toEqual('block');
    })
})
