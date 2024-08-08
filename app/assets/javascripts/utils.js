export function addIntersectionMethodToSet() {
// Modern browsers have Set.intersection but NodeJS only added it fairly recently so we need to provide a polyfill for the build pipeline
    if (! Set.prototype.intersection) {
        Set.prototype.intersection = function(that) {
            return new Set([...that].filter(x => this.has(x)));
        }
    }
}

const GOV_UK_HIDDEN_CLASS = 'govuk-!-display-none';
export function setVisible(el, isVisible) {
    el.classList.toggle(GOV_UK_HIDDEN_CLASS, !isVisible);
}

export function isVisible(el) {
    return !el.classList.contains(GOV_UK_HIDDEN_CLASS);
}

export function removeElement(el) {
    el.parentElement.removeChild(el);
}

export function noop() {}

export function normaliseText(text) {
    return text.trim().toLowerCase();
}
