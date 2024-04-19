// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
  window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function(event) {

  // handle back click
  var backLink = document.querySelector('.govuk-back-link');
  if (backLink !== null) {
    backLink.addEventListener('click', function(e){
      e.preventDefault();
      e.stopPropagation();
      window.history.back();
    });
  }

  localiseUtcDateTimeValues();
});

function localiseUtcDateTimeValues() {
  /* Ensure that date/time display formats are permitted by the gov.uk style guide, see:
      https://www.gov.uk/guidance/style-guide/a-to-z-of-gov-uk-style#dates
      https://www.gov.uk/guidance/style-guide/a-to-z-of-gov-uk-style#times
  */
  const utcSuffix = 'Z',
      classToOptions = {
        'utcDateShort': {dateStyle: "medium"}, // e.g. 1 Jan 2020
        'utcDateLong': {dateStyle: "long"}, // e.g. 1 January 2020
        'utcDateTime': {day:"numeric", month: "long", year: "numeric", hour: "numeric", minute: "numeric", hourCycle: 'h12'}, // e.g. 1 January 2020 at 12:00 pm
      };

  Object.entries(classToOptions).forEach(([className, options]) => {
    document.querySelectorAll(`.${className}`).forEach(el => {
      const rawDateText = el.textContent.trim(),
          utcDateText = rawDateText + (rawDateText.endsWith(utcSuffix) ? '' : utcSuffix),
          date = new Date(utcDateText),
          isValidDate = !isNaN(date.valueOf());
      if (isValidDate) {
        el.textContent = date.toLocaleString(undefined, options);
      } else {
        console.warn(`Expected a valid date/time string but found "${rawDateText}"`);
      }
    });
  });

}