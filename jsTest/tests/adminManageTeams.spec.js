import {JSDOM} from 'jsdom';
import {onPageShow} from '../../app/assets/javascripts/adminManageTeams.js';
import {paginationHelper, paginationContainerHtml, arrayFromTo} from "./testUtils.js";

describe('adminManageTeams', () => {
    let document;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="teamsTable">
                <div class="govuk-table__body"></div>                
            </div>
            <input id="teamFilter" type="text">
            <div id="teamCount"></div>
            ${paginationContainerHtml}
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.Event = dom.window.Event;
    });

    function buildTeamPanels(count) {
        document.querySelector('.govuk-table__body').innerHTML = Array.from(
            {length: count},
            (_, i) => `
            <div class="govuk-table__row" 
                data-index="${i+1}" 
                data-name="Team ${i+1}" 
                data-id="Id ${i+1}" 
                data-emails="email${2*i}@example.com,email${2*i+1}@example.com" >Team ${i+1}</div>`
        ).join('');
    }

    function enterFilterText(value) {
        document.getElementById('teamFilter').value = value;
        document.getElementById('teamFilter').dispatchEvent(new Event('input'));
    }

    function getTeamCount() {
        return Number(document.getElementById('teamCount').textContent);
    }

    it("if 10 teams are present on the page then all are visible and pagination is not available",  () => {
        buildTeamPanels(10);

        onPageShow();

        expect(paginationHelper.getVisiblePanelIndexes('.govuk-table__row')).toEqual(arrayFromTo(1, 10));
        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
        expect(getTeamCount()).toBe(10);
    });

    it("if 11 teams are present on the page then only the first 10 are visible and pagination is available",  () => {
        buildTeamPanels(11);

        onPageShow();

        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
        expect(paginationHelper.getVisiblePanelIndexes('.govuk-table__row')).toEqual(arrayFromTo(1, 10));
        expect(getTeamCount()).toBe(11);
    });

    it("when the user enters some filter text then teams with names that match the filter are shown",  () => {
        buildTeamPanels(100);

        onPageShow();
        enterFilterText('EAm 1');

        expect(paginationHelper.getVisiblePanelData('.govuk-table__row', 'name').map(o => o.name)).toEqual([
            'Team 1', 'Team 10', 'Team 11', 'Team 12', 'Team 13', 'Team 14', 'Team 15', 'Team 16', 'Team 17', 'Team 18'
        ]);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();

        paginationHelper.clickNext();
        expect(paginationHelper.getVisiblePanelData('.govuk-table__row', 'name').map(o => o.name)).toEqual(['Team 19', 'Team 100']);
        expect(getTeamCount()).toBe(12);
    });

    it("when the user enters some filter text then teams with ids that match the filter are shown",  () => {
        buildTeamPanels(100);

        onPageShow();
        enterFilterText('D 1');

        expect(paginationHelper.getVisiblePanelData('.govuk-table__row', 'id').map(o => o.id)).toEqual([
            'Id 1', 'Id 10', 'Id 11', 'Id 12', 'Id 13', 'Id 14', 'Id 15', 'Id 16', 'Id 17', 'Id 18'
        ]);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();

        paginationHelper.clickNext();
        expect(paginationHelper.getVisiblePanelData('.govuk-table__row', 'id').map(o => o.id)).toEqual(['Id 19', 'Id 100']);
        expect(getTeamCount()).toBe(12);
    });

    it("when the user enters some filter text then teams with an email address that starts with the filter value are shown",  () => {
        buildTeamPanels(100);

        onPageShow();
        enterFilterText('eMAil1');

        expect(paginationHelper.getVisiblePanelData('.govuk-table__row', 'emails').map(o => o.emails)).toEqual([
            'email0@example.com,email1@example.com',
            'email10@example.com,email11@example.com',
            'email12@example.com,email13@example.com',
            'email14@example.com,email15@example.com',
            'email16@example.com,email17@example.com',
            'email18@example.com,email19@example.com',
            'email100@example.com,email101@example.com',
            'email102@example.com,email103@example.com',
            'email104@example.com,email105@example.com',
            'email106@example.com,email107@example.com',
        ]);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();

        expect(getTeamCount()).toBe(56);
    });

    it("when the user enters some filter text then teams with an email address that contains, but does not start with, the filter value are not shown",  () => {
        buildTeamPanels(100);

        onPageShow();
        enterFilterText('example.com');

        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
        expect(getTeamCount()).toBe(0);
    });

    it("when the user enters some filter text that does not match any teams then no teams are shown",  () => {
        buildTeamPanels(100);

        onPageShow();
        enterFilterText('nomathces');

        expect(paginationHelper.getVisiblePanelIndexes()).toEqual([]);
        expect(paginationHelper.paginationIsAvailable()).toBeFalse();
        expect(getTeamCount()).toBe(0);
    });

    it("when the user navigates back to the page and the filter text is re-populated by the browser the filter takes effect immediately",  () => {
        buildTeamPanels(100);

        enterFilterText('team 1');
        onPageShow();

        expect(paginationHelper.getVisiblePanelData('.govuk-table__row', 'name').map(o => o.name)).toEqual([
            'Team 1', 'Team 10', 'Team 11', 'Team 12', 'Team 13', 'Team 14', 'Team 15', 'Team 16', 'Team 17', 'Team 18'
        ]);
        expect(paginationHelper.paginationIsAvailable()).toBeTrue();
        expect(getTeamCount()).toBe(12);
    });
});
