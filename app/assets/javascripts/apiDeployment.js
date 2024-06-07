export function onLoad(){
    const domains = (() => {
        const elDomain = document.getElementById('domain');
        let onChangeHandler = () => {};

        elDomain.addEventListener('input', event => {
            onChangeHandler();
        });

        return {
            get value() {
                return elDomain.value;
            },
            get hasValue() {
                return !! this.value;
            },
            onChange(handler) {
                onChangeHandler = handler;
            }
        };
    })();

    const subDomains = (() => {
        const elSubdomain = document.getElementById('subdomain');

        return {
            enable(isEnabled) {
                elSubdomain.disabled = !isEnabled;
            },
            filterByDomain(domain) {
                [...elSubdomain.options].forEach(elOption => {
                    if (elOption.dataset.domain === domain) {
                        elOption.removeAttribute('hidden');
                    } else {
                        elOption.setAttribute('hidden', 'hidden');
                    }
                });
            },
            clear() {
                elSubdomain.selectedIndex = 0;
            }
        };
    })();

    subDomains.enable(domains.hasValue);
    subDomains.filterByDomain(domains.value);

    domains.onChange(() => {
        subDomains.filterByDomain(domains.value);
        subDomains.enable(domains.hasValue);
        subDomains.clear();
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onLoad);
}
