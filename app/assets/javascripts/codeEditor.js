import {exampleOasYaml} from './oaseditor/oasYamlExample.js';
import {exampleWiremock} from './oaseditor/wiremockExample.js';

function getFormatDetails(format) {
    let exampleData = '',
        mode = "ace/mode/text";

    if (format === 'yaml') {
        exampleData = exampleOasYaml;
        mode = 'ace/mode/yaml';
    } else if (format === 'json') {
        exampleData = exampleWiremock;
        mode = 'ace/mode/json';
    } else {
        console.warn(`Unrecognised format value of '${format}' in codeEditor.js`);
    }

    return {exampleData, mode};
}

export function buildCodeEditor() {
    const elAceEditorContainer = document.getElementById('aceEditorContainer'),
        elAceEditorMirror = document.getElementById('aceEditorMirror'),
        formatName = elAceEditorContainer.getAttribute('data-format'),
        {exampleData, mode} = getFormatDetails(formatName),
        editor = ace.edit(elAceEditorContainer.id),
        oasFallbackValue = editor.container.hasAttribute("data-populate-example") ? exampleData : '',
        initialEditorContents = elAceEditorMirror.value || oasFallbackValue;

    function updateMirrorValue() {
        elAceEditorMirror.value = editor.getValue();
    }
    editor.setOption('useWorker', false);
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode(mode);
    editor.setValue(initialEditorContents);

    editor.session.on('change', updateMirrorValue);

    updateMirrorValue();

    return {
        get hasChanged() {
            return initialEditorContents !== editor.getValue();
        }
    };
}
