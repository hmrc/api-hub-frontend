import {buildCodeEditor} from './codeEditor.js';

export function onDOMContentLoaded(){
    const elForm = document.querySelector('form'),
        codeEditor = buildCodeEditor();

    let continueClicked = false;
    elForm.addEventListener('submit', () => {
        continueClicked = true;
    });

    window.addEventListener('beforeunload', evt => {
        if (!continueClicked && codeEditor.hasChanged) {
            evt.preventDefault();
        }
    });

}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onDOMContentLoaded);
}
