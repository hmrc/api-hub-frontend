import {noop} from "./utils.js";

function buildCopyLink(valueToCopy, elContainer) {
    const elLink = document.createElement('a');
    elLink.textContent = 'Copy';
    elLink.href = '#';
    elLink.addEventListener('click', () => {
        navigator.clipboard.writeText(valueToCopy).catch(err => {
            console.error('Failed to copy value to clipboard: ', err);
        });
    });
    elContainer.innerHTML = '';
    elContainer.appendChild(elLink);
}

function buildStatDisplay(el, onSuccess) {
    const noValuePlaceholder = '...',
        loadingText = noValuePlaceholder,
        errorText = 'Error',
        elFetchStatButton = el.querySelector('button'),
        elStatValueContainer = el.querySelector('.hip-stat-value');

    let onFetchClickHandler = noop;
    elFetchStatButton.addEventListener('click', () => onFetchClickHandler());

    function setValue(value) {
        elStatValueContainer.textContent = value;
    }
    setValue(noValuePlaceholder);

    return {
        loading() {
            setValue(loadingText);
            elFetchStatButton.disabled = true;
        },
        error() {
            setValue(errorText);
            elFetchStatButton.disabled = false;
        },
        success(statValue) {
            if (onSuccess) {
                onSuccess(statValue, elStatValueContainer);
            } else {
                setValue(statValue);
            }
            elFetchStatButton.disabled = false;
        },
        onFetch(handler) {
            onFetchClickHandler = handler;
        },
    };
}

export function onDomLoaded() {
    const totalApisStat = buildStatDisplay(document.getElementById('statTotalApis')),
        prodApisStat = buildStatDisplay(document.getElementById('statProdApis')),
        listProdApisStat = buildStatDisplay(document.getElementById('statProdList'), buildCopyLink);

    function onFetchApisInProductionClick() {
        totalApisStat.loading();
        prodApisStat.loading();

        fetch(`./statistics/apis-in-production`)
            .then(response => response.json())
            .then(result => {
                totalApisStat.success(result.totalApis);
                prodApisStat.success(result.totalInProduction);
            })
            .catch(e => {
                console.error(e);
                totalApisStat.error();
                prodApisStat.error();
            });
    }

    totalApisStat.onFetch(onFetchApisInProductionClick);
    prodApisStat.onFetch(onFetchApisInProductionClick);

    function onFetchListOfApisInProductionClick() {
        listProdApisStat.loading();

        fetch(`./statistics/list-apis-in-production`)
            .then(response => response.json())
            .then(result => {
                listProdApisStat.success(result.join('\n'));
            })
            .catch(e => {
                console.error(e);
                listProdApisStat.error();
            });
    }
    listProdApisStat.onFetch(onFetchListOfApisInProductionClick);

}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
