const initialState = cm6.createEditorState(oasYaml);
const view = cm6.createEditorView(initialState, document.getElementById("codemirror-editor"));

// to get editor contents: view.state.doc.toString()
