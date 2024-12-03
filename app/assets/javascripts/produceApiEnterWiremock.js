import {oasExampleWiremock} from './oaseditor/wiremockExample.js';

export function onDOMContentLoaded(){
    const elForm = document.querySelector('form'),
        elWiremockEditorMirror = document.querySelector('[name="value"]'),
        editor = ace.edit("aceEditorContainer");
    const wiremockFallbackValue = editor.container.hasAttribute("data-populate-example") ? oasExampleWiremock : '';
    const initialEditorContents = elWiremockEditorMirror.value || wiremockFallbackValue;

    editor.setOption('useWorker', false);
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/json");
    editor.setValue(initialEditorContents);

    let continueClicked = false;
    elForm.addEventListener('submit', () => {
        elWiremockEditorMirror.value = editor.getValue();
        continueClicked = true;
    });

    window.addEventListener('beforeunload', evt => {
        if (!continueClicked && initialEditorContents !== editor.getValue()) {
            evt.preventDefault();
        }
    });

}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onDOMContentLoaded);
}
