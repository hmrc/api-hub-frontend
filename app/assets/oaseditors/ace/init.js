let editor;
function initAceEditor() {
    editor = ace.edit("ace-editor");
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/yaml");
    editor.setValue(oasYaml);
    // to read content: editor.getValue()

     //to highlight a line in error:
    // var Range = ace.require('ace/range').Range
    // editor.session.addMarker(new Range(3, 0, 4, 0), "aceErrorHighlight");

    /*
    // this doesnt work, should trigger whenever error list changes, see https://github.com/ajaxorg/ace/issues/3423 and https://stackoverflow.com/a/10667290/138256
    editor.getSession().on("changeAnnotation", function(){
        var annot = editor.getSession().getAnnotations();

        for (var key in annot){
            if (annot.hasOwnProperty(key))
                console.log("[" + annot[key][0].row + " , " + annot[key][0].column + "] - \t" + annot[key][0].text);
        }
    });
     */

}

initAceEditor();