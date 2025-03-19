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

            elCopyButton.addEventListener('click', () => {
                navigator.clipboard.writeText(elResponse.innerText).catch(err => {
                    console.error('Failed to copy value to clipboard: ', err);
                });
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

                    if (viewState === VIEW_STATE_INITIAL) {
                        elSelectEnvironment.value = '';
                        elSelectEndpoint.value = '';
                        this.setParameterInputs();
                        this.response = '';

                    } else if (viewState === VIEW_STATE_SELECT_ENDPOINT) {
                        elSelectEndpoint.value = '';
                        this.setParameterInputs();
                        this.response = '';

                    } else if (viewState === VIEW_STATE_WAITING_FOR_RESPONSE) {
                        this.response = '';
                    }
                },
                get environment() {
                    return elSelectEnvironment.value;
                },
                get endpoint() {
                    return elSelectEndpoint.value;
                },
                get parameterValues() {
                    return [...elParameterInputsContainer.querySelectorAll('input')].map(input => input.value);
                },
                set response(text) {
                    try {
                        const parseResult = JSON.parse(text);
                        if (typeof parseResult === 'string') {
                            elResponse.innerText = parseResult;
                        } else {
                            elResponse.innerText = JSON.stringify(parseResult, null, 4);
                        }
                    } catch (e) {
                        elResponse.innerText = text;
                    }
                }
            };
        })(),
        stateMachine = (onStateChanged => {
            let currentState;

            function setState(newState) {
                if (newState !== currentState) {
                    console.debug("State changed: ", currentState, " -> ", newState);
                    currentState = newState;
                    onStateChanged(currentState);
                }
            }

            setState(VIEW_STATE_INITIAL);

            return {
                get state() {
                    return currentState;
                },
                environmentSelected() {
                    if (currentState === VIEW_STATE_INITIAL) {
                        setState(VIEW_STATE_SELECT_ENDPOINT);
                    } else if (currentState === VIEW_STATE_SHOWING_RESPONSE) {
                        setState(VIEW_STATE_READY_TO_SEND);
                    }
                },
                environmentCleared() {
                    setState(VIEW_STATE_INITIAL);
                },
                endpointSelected() {
                    setState(VIEW_STATE_READY_TO_SEND);
                },
                endpointCleared() {
                    setState(VIEW_STATE_SELECT_ENDPOINT);
                },
                requestSubmitted() {
                    setState(VIEW_STATE_WAITING_FOR_RESPONSE);
                },
                successResponseReceived() {
                    setState(VIEW_STATE_SHOWING_RESPONSE);
                },
                errorResponseReceived() {
                    setState(VIEW_STATE_SHOWING_RESPONSE);
                }
            }
        })(newState => view.state = newState);

    view.onEnvironmentChanged(env => {
        if (env) {
            stateMachine.environmentSelected();
        } else {
            stateMachine.environmentCleared();
        }
    });

    view.onEndpointChanged((endpoint, paramNames) => {
        if (endpoint) {
            stateMachine.endpointSelected();
            view.setParameterInputs(...paramNames);
        } else {
            stateMachine.endpointCleared();
        }
    });

    view.onSubmit(() => {
        stateMachine.requestSubmitted();
        const environment = encodeURIComponent(view.environment),
            endpoint = encodeURIComponent(view.endpoint),
            params = view.parameterValues.map(encodeURIComponent).join(',');
        fetch(`test-apim-endpoints/${environment}/${endpoint}/${params}`)
            .then(response => {
                if (!response.ok) {
                    return Promise.reject(response);
                }
                return response.text();
            })
            .then(text => {
                stateMachine.successResponseReceived();
                view.response = text;
            })
            .catch(errorResponse => {
               stateMachine.errorResponseReceived();
               errorResponse.text().then(responseBody => {
                   view.response = `Request not submitted to APIM\nHub endpoint returned HTTP error: ${errorResponse.status} - ${errorResponse.statusText}\n\n${responseBody}`;
               });
            });
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
