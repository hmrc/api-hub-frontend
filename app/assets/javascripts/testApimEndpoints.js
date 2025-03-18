import {noop, setVisible} from "./utils.js";

export function onDomLoaded() {
    const
        VIEW_STATE_INITIAL = 'initial',
        VIEW_STATE_SELECT_ENDPOINT = 'selectEndpoint',
        VIEW_STATE_READY_TO_SEND = 'readyToSend',
        VIEW_STATE_WAITING_FOR_RESPONSE = 'waitingForResponse',
        VIEW_STATE_SHOWING_RESPONSE = 'showingResponse',

        view = (() => {
            const elInputsContainer = document.getElementById('apimRequestInputs'),
                elSelectEnvironment = document.getElementById('selectEnvironment'),
                elSelectEndpoint = document.getElementById('selectEndpoint'),
                elSelectEndpointContainer = elSelectEndpoint.closest('.govuk-form-group'),
                elParameterInputsContainer = document.getElementById('parameterInputs'),
                elSubmitButton = document.getElementById('submit'),
                elResponseContainer = document.getElementById('apimResponseContainer'),
                elResponse = document.getElementById('apimResponse'),
                elCopyButton = document.getElementById('copyApimResponse');

            let environmentChangedHandler = noop,
                endpointChangedHandler = noop,
                submitHandler = noop;

            elSelectEnvironment.addEventListener('input', event => {
                environmentChangedHandler(event.target.value);
            });
            elSelectEndpoint.addEventListener('input', event => {
                let paramNames = [];
                if (event.target.selectedOptions.length) {
                    const paramNamesString = event.target.selectedOptions[0].dataset['paramNames'];
                    if (paramNamesString) {
                        paramNames = paramNamesString.split(',');
                    }
                }
                endpointChangedHandler(event.target.value, paramNames);
            });
            elSubmitButton.addEventListener('click', event => {
                submitHandler();
            });

            function setEnabled(el, isEnabled) {
                el.disabled = !isEnabled;
            }

            function buildParameterInputHtml(name) {
                return `<div class="govuk-form-group">
                            <label class="govuk-label" for="${name}">${name}:</label>
                            <input class="govuk-input" name="${name}" type="text">
                        </div>`;
            }

            return {
                onEnvironmentChanged(handler) {
                    environmentChangedHandler = handler;
                },
                onEndpointChanged(handler) {
                    endpointChangedHandler = handler;
                },
                onSubmit(handler) {
                    submitHandler = handler;
                },
                setParameterInputs(...parameterNames) {
                    elParameterInputsContainer.innerHTML = parameterNames.map(buildParameterInputHtml).join('');
                },
                set state(viewState) {
                    setVisible(elSelectEndpointContainer, viewState !== VIEW_STATE_INITIAL);
                    setVisible(elParameterInputsContainer, viewState !== VIEW_STATE_INITIAL);
                    setVisible(elResponseContainer, viewState === VIEW_STATE_SHOWING_RESPONSE);

                    setEnabled(elInputsContainer, viewState !== VIEW_STATE_WAITING_FOR_RESPONSE);
                    setEnabled(elSubmitButton, [VIEW_STATE_READY_TO_SEND, VIEW_STATE_SHOWING_RESPONSE].includes(viewState));
                }
            };
        })();

    view.state = VIEW_STATE_INITIAL;

    view.onEnvironmentChanged(env => {
        if (env) {
            view.state = VIEW_STATE_SELECT_ENDPOINT;
        } else {
            view.state = VIEW_STATE_INITIAL;
        }
    });

    view.onEndpointChanged((endpoint, paramNames) => {
        if (endpoint) {
            view.state = VIEW_STATE_READY_TO_SEND;
            view.setParameterInputs(...paramNames);
        } else {
            view.state = VIEW_STATE_SELECT_ENDPOINT;
        }
    });

    view.onSubmit(() => {
        view.state = VIEW_STATE_WAITING_FOR_RESPONSE;
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
