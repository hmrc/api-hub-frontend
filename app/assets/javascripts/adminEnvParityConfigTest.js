
function buildFetchPanel(formId, buildUrl) {
    const elForm = document.getElementById(formId),
        elResults = elForm.querySelector('textarea'),
        elFetchButton = elForm.querySelector('button');

    elFetchButton.addEventListener('click', () => {
        elFetchButton.disabled = true;
        elResults.value = 'Fetching...';

        const environmentName = elForm.querySelector('input[name="environment"]:checked').value,
            elClientId = document.getElementById('clientId'),
            clientId = elClientId ? elClientId.value : null;

        fetch(buildUrl(environmentName, clientId))
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                return `Error ${response.status} fetching results: ${response.statusText}`;
            })
            .then(data => {
                elResults.value = JSON.stringify(data, null, 2);
            })
            .catch(error => {
                elResults.value = `Error fetching results: ${error}`;
            })
            .finally(() => {
                elFetchButton.disabled = false;
            });
    });
}

export function onDomLoaded() {
    buildFetchPanel('scopes', (environmentName, clientId) => `environment-parity-test/client-scopes/${environmentName}/${clientId}`);
    buildFetchPanel('egresses', (environmentName) => `environment-parity-test/egresses/${environmentName}`);
    buildFetchPanel('deployments', (environmentName) => `environment-parity-test/deployments/${environmentName}`);

}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
