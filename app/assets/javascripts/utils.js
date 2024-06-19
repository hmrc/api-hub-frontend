export function setVisible(el, isVisible) {
    el.classList.toggle('govuk-!-display-none', !isVisible);
}

export function noop() {}
