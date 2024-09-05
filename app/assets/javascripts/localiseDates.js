const BROWSER_DEFAULT_LOCALE = undefined,
    BROWSER_DEFAULT_TZ = undefined;

export function localiseUtcDateTimeValues(locale = BROWSER_DEFAULT_LOCALE, timeZone = BROWSER_DEFAULT_TZ) {
    /* Ensure that date/time display formats are permitted by the gov.uk style guide, see:
        https://www.gov.uk/guidance/style-guide/a-to-z-of-gov-uk-style#dates
        https://www.gov.uk/guidance/style-guide/a-to-z-of-gov-uk-style#times
    */
    const utcSuffix = 'Z',
        classToOptions = {
            'utcDateShort': [{dateStyle: "medium"}], // e.g. 1 Jan 2020
            'utcDateLong': [{dateStyle: "long"}], // e.g. 1 January 2020
            'utcDateTime': [{dateStyle: "long"}, {hour: "numeric", minute: "numeric", hourCycle: 'h12'}], // e.g. 1 January 2020 at 12:00 pm
        };

    Object.entries(classToOptions).forEach(([className, options]) => {
        document.querySelectorAll(`.${className}`).forEach(el => {
            const rawDateText = el.textContent.trim(),
                utcDateText = rawDateText + (rawDateText.endsWith(utcSuffix) ? '' : utcSuffix),
                date = new Date(utcDateText),
                isValidDate = !isNaN(date.valueOf());
            if (isValidDate) {
                const [dateFormatOptions, timeFormatOptions] = options;
                /* jshint +W119 */
                el.textContent = date.toLocaleDateString(locale, {...dateFormatOptions, timeZone});
                if (timeFormatOptions) {
                    const formattedTime = date.toLocaleTimeString(locale, {...timeFormatOptions, timeZone});
                    el.textContent += ` at ${formattedTime}`;
                }
                /* jshint +W119 */
            } else {
                console.warn(`Expected a valid date/time string but found "${rawDateText}"`);
            }
        });
    });

}