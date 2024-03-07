function initSwaggerEditor() {
    const SpecUpdateListenerPlugin = function(system) {
        return {
            statePlugins: {
                spec: {
                    wrapActions: {
                        updateSpec: (oriAction) => (...args) => {
                            const [str] = args
                            console.log("ERRORS", system.errSelectors.allErrors().toJS())
                            // to read content: system.specSelectors.specStr())
                            return oriAction(...args)
                        }
                    }
                }
            }
        }
    }
    const editor = SwaggerEditorBundle({
        dom_id: '#swagger-editor',
        plugins: [
            SpecUpdateListenerPlugin
        ]
    })


    editor.specActions.updateSpec(oasYaml);
}
initSwaggerEditor();