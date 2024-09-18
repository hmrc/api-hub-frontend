import {oasExampleYaml} from './oaseditor/oasYamlExample.js';

export function onDOMContentLoaded(){
    const elForm = document.querySelector('form'),
        elOasEditorMirror = document.querySelector('[name="value"]'),
        editor = ace.edit("aceEditorContainer");

    editor.setOption('useWorker', false);
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/yaml");
    editor.setValue(elOasEditorMirror.value || oasExampleYaml);

    elForm.addEventListener('submit', () => {
        elOasEditorMirror.value = editor.getValue();
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onDOMContentLoaded);
}
