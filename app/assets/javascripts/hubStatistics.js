import {noop} from "./utils.js";

function buildStatDisplay(el) {
    const noValuePlaceholder = '...',
        loadingText = noValuePlaceholder,
        errorText = 'Error',
        elFetchStatButton = el.querySelector('button'),
        elStatValue = el.querySelector('.hip-stat-number');

    let onFetchClickHandler = noop;
    elFetchStatButton.addEventListener('click', () => onFetchClickHandler());

    function setValue(value) {
        elStatValue.textContent = value;
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
            setValue(statValue);
            elFetchStatButton.disabled = false;
        },
        onFetch(handler) {
            onFetchClickHandler = handler;
        },
    };
}

export function onDomLoaded() {
    const totalApisStat = buildStatDisplay(document.getElementById('statTotalApis')),
        prodApisStat = buildStatDisplay(document.getElementById('statProdApis'))

    function onFetchClick() {
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

    totalApisStat.onFetch(onFetchClick);
    prodApisStat.onFetch(onFetchClick);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
