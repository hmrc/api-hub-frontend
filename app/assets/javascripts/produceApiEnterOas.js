import {oasExampleYaml} from './oaseditor/oasYamlExample.js';

export function onDOMContentLoaded(){
    const elForm = document.querySelector('form'),
        elOasEditorMirror = document.querySelector('[name="value"]'),
        editor = ace.edit("aceEditorContainer"),
        initialEditorContents = elOasEditorMirror.value || oasExampleYaml;

    editor.setOption('useWorker', false);
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/yaml");
    editor.setValue(initialEditorContents);

    let continueClicked = false;
    elForm.addEventListener('submit', () => {
        elOasEditorMirror.value = editor.getValue();
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
