import {setVisible} from "./utils.js";

export function onPageShow() {
    const elBasePathContainer = document.getElementById('basePathContainer'),
        elBasePathValue = document.getElementById('basePathValue'),
        elForm = document.querySelector('form');

    function showBasePath(basePath) {
        if (basePath) {
            setVisible(elBasePathContainer, true);
            elBasePathValue.innerHTML = basePath;
        } else {
            setVisible(elBasePathContainer, false);
            elBasePathValue.innerHTML = '';
        }
    }
    function clearBasePath() {
        showBasePath('');
    }

    function showBasePathIfApplicable() {
        const elSelectedDomain = document.querySelector('input[name="domain"]:checked'),
            elSelectedSubdomain = document.querySelector('input[name="subDomain"]:checked');

        if (elSelectedDomain && elSelectedSubdomain) {
            const selectedDomain = elSelectedDomain.value,
                subDomainMatchesDomain = elSelectedSubdomain.dataset.domain === selectedDomain;

            /* Need to handle the case where user selects a domain and subdomain but then changes the domain without
            picking a new subdomain - there will still be a checked subDomain radio button on the page, but it won't be
            visible to the user and it doesn't make sense to display its base path */
            if (subDomainMatchesDomain) {
                showBasePath(elSelectedSubdomain.dataset.basepath);
            } else {
                clearBasePath();
            }

        } else {
            clearBasePath();
        }
    }

    elForm.addEventListener('click', e => {
        if (e.target.type === 'radio') {
            showBasePathIfApplicable();
        }
    });

    showBasePathIfApplicable(); // run when page is first loaded in case we are returning to a pre-populated form
}

if (typeof window !== 'undefined') {
    window.addEventListener("pageshow", onPageShow);
}
