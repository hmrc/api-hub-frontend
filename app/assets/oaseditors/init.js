


function initAceEditor() {
    const editor = ace.edit("ace-editor");
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/yaml");
    editor.setValue(oasYaml);
    // editor.setOptions({
    //     maxLines: Infinity
    // });
    // editor.getSession().on('change', function () {
    //     const yaml = editor.getValue();
    //     console.log("YAML", yaml);
    // });
}

initSwaggerEditor();
initAceEditor();