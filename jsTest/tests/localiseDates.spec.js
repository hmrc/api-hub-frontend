import {JSDOM} from 'jsdom';
import {localiseUtcDateTimeValues} from '../../app/assets/javascripts/localiseDates.js';

describe('localiseUtcDateTimeValues', () => {
    // need to specify these for headless tests - normally they come from the user's browser configuration
    const LOCALE = 'en-GB',
        TZ = 'Europe/London';
    let document;

    beforeEach(() => {
        const dom = (new JSDOM('<!DOCTYPE html><div id="dates"></div>'));
        document = dom.window.document;
        globalThis.document = document;
    });

    function createDateField(className, date) {
        const el = document.createElement('div');
        el.className = className;
        el.textContent = date;
        document.getElementById('dates').appendChild(el);
    }

    describe('valid date/times are formatted and localised correctly', () => {
        [
            ['utcDateShort', '2020-01-01T00:00:00', '1 Jan 2020'],
            ['utcDateLong', '2020-01-01T00:00:00', '1 January 2020'],
            // the JS date formatter inserts a narrow no-break space character between the time and am/pm
            ['utcDateTime', '2020-01-01T12:00:00', '1 January 2020 at 12:00\u202fpm'],
            ['utcDateTime', '2020-07-01T01:00:00', '1 July 2020 at 2:00\u202fam'],
        ].forEach(([className, utcDateTime, expected]) => {
            it(`${className} format applied correctly to ${utcDateTime}`, () => {
                createDateField(className, utcDateTime);

                localiseUtcDateTimeValues(LOCALE, TZ);

                expect(document.querySelector(`.${className}`).textContent).toBe(expected);
            });
        })
    });

    it("date formatter does not alter invalid date values, and logs a warning",  () => {
        const invalidDate = 'invalid date';
        spyOn(console, 'warn');
        createDateField('utcDateTime', invalidDate);

        localiseUtcDateTimeValues(LOCALE, TZ);

        expect(document.querySelector('.utcDateTime').textContent).toBe(invalidDate);
        expect(console.warn).toHaveBeenCalledWith(`Expected a valid date/time string but found "${invalidDate}"`);
    });

    it("date formatting is still correct if UTC timezone suffix is included in value",  () => {
        createDateField('utcDateLong', '2020-07-01T00:00:00Z');

        localiseUtcDateTimeValues(LOCALE, TZ);

        expect(document.querySelector('.utcDateLong').textContent).toBe('1 July 2020');
    });

});
