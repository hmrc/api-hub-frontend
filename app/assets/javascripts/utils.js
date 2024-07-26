export function addIntersectionMethodToSet() {
// Modern browsers have Set.intersection but NodeJS only added it fairly recently so we need to provide a polyfill for the build pipeline
    if (! Set.prototype.intersection) {
        Set.prototype.intersection = function(that) {
            return new Set([...that].filter(x => this.has(x)));
        }
    }
}

export function setVisible(el, isVisible) {
    el.classList.toggle('govuk-!-display-none', !isVisible);
}

export function removeElement(el) {
    el.parentElement.removeChild(el);
}

export function noop() {}
