import {JSDOM} from "jsdom";
import {onDOMContentLoaded} from "../../app/assets/javascripts/enterOas.js";
import {buildFakeAceEditor} from "./testUtils.js";

describe('onDOMContentLoaded', () => {
    let document, elHiddenInput, elForm;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <form>
                <input type="hidden" name="value">
            </form>            
        `);
        document = dom.window.document;
        globalThis.document = document;
        globalThis.window = dom.window;
        globalThis.Event = dom.window.Event;
        globalThis.ace = buildFakeAceEditor();
        elHiddenInput = document.querySelector('[name="value"]');
        elForm = document.querySelector('form');
    });

    it("when the page is loaded then the value of the hidden input is copied to the editor",  () => {
        const editor = ace.edit("aceEditorContainer"),
            inputValue = 'some value';

        elHiddenInput.value = inputValue;

        onDOMContentLoaded();

        expect(editor.getValue()).toEqual(inputValue);
    });

    it("when the form is submitted then the value of the editor is copied to the hidden input",  () => {
        onDOMContentLoaded();

        const editor = ace.edit("aceEditorContainer"),
            editorContent = 'some value';
        editor.setValue(editorContent);

        elForm.dispatchEvent(new Event('submit'));

        expect(elHiddenInput.value).toEqual(editorContent);
    });

    it('the editor is configured for YAML', () => {
        const editor = ace.edit("aceEditorContainer");

        onDOMContentLoaded();

        expect(editor.session.setMode).toHaveBeenCalledWith("ace/mode/yaml");
    });

    it('the editor is configured to use the monokai theme', () => {
        const editor = ace.edit("aceEditorContainer");

        onDOMContentLoaded();

        expect(editor.setTheme).toHaveBeenCalledWith("ace/theme/monokai");
    });

    it('the editor is configured to not web workers', () => {
        const editor = ace.edit("aceEditorContainer");

        onDOMContentLoaded();

        expect(editor.setOption).toHaveBeenCalledWith('useWorker', false);
    });

});
