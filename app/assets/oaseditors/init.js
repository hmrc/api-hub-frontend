
function initSwaggerEditor() {
    const SpecUpdateListenerPlugin = function(system) {
        return {
            statePlugins: {
                spec: {
                    wrapActions: {
                        updateSpec: (oriAction) => (...args) => {
                            const [str] = args
                            console.log("ERRORS", system.errSelectors.allErrors().toJS())
                            return oriAction(...args)
                        }
                    }
                }
            }
        }
    }
    const SpecValidateListenerPlugin = function(system) {
        return {
            statePlugins: {
                spec: {
                    wrapActions: {
                        validateParams: (ori, { specSelectors }) => (req) => {
                            console.log("====",specSelectors.isOAS3())
                            return ori(req, specSelectors.isOAS3())
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
