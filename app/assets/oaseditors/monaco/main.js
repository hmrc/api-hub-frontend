var require = { paths: { 'vs': '_static/js/ext/monaco-editor/min/vs' } };

const elEditor = document.getElementById('monaco-editor'),
    editor = monaco.editor.create(elEditor, {
        value: oasYaml,
        language: 'yaml',
        automaticLayout: true,
    });

// to get value of editor: editor.getValue()

/*
setInterval(() => {
    // this isn't working, should get list of all errors in the editor
    console.log(monaco.editor.getModelMarkers());
}, 5000)
*/

/*
// highlight a line
editor.createDecorationsCollection([
    {
        range: new monaco.Range(3, 1, 3, 1),
        options: {
            isWholeLine: true,
            className: "monacoErrorHighlight",
        },
    },
]);
 */