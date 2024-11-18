import {JSDOM} from "jsdom";
import {onDOMContentLoaded} from "../../app/assets/javascripts/produceApiEnterOas.js";
import {buildFakeAceEditor} from "./testUtils.js";
import {oasExampleYaml} from '../../app/assets/javascripts/oaseditor/oasYamlExample.js';

describe('onDOMContentLoaded', () => {
    let document, elHiddenInput, elForm, window;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <form>
                <input type="hidden" name="value">
            </form>            
        `);
        document = dom.window.document;
        window = dom.window;
        globalThis.document = document;
        globalThis.window = window;
        globalThis.Event = window.Event;
        globalThis.ace = buildFakeAceEditor();
        elHiddenInput = document.querySelector('[name="value"]');
        elForm = document.querySelector('form');
    });

    it(`when the page is loaded, the editor has no value
        and it has the 'data-populate-example' attribute
        then the oas example is copied to the editor`,  () => {
        globalThis.ace = buildFakeAceEditor(true);

        const editor = ace.edit("aceEditorContainer"),
            inputValue = '';

        elHiddenInput.value = inputValue;

        onDOMContentLoaded();

        expect(editor.getValue()).toEqual(oasExampleYaml);
    });

    it(`when the page is loaded, the editor has no value
        and it does not have the 'data-populate-example' attribute
        then the editor remains empty`,  () => {
        globalThis.ace = buildFakeAceEditor(false);

        const editor = ace.edit("aceEditorContainer"),
            inputValue = '';

        elHiddenInput.value = inputValue;

        onDOMContentLoaded();

        expect(editor.getValue()).toEqual(inputValue);
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

    it('the editor is configured to not use web workers', () => {
        const editor = ace.edit("aceEditorContainer");

        onDOMContentLoaded();

        expect(editor.setOption).toHaveBeenCalledWith('useWorker', false);
    });

    it("if the contents of the editor are changed and we try to navigate away from the page the event gets cancelled",  () => {
        const evt = new Event('beforeunload');
        spyOn(evt, 'preventDefault');

        onDOMContentLoaded();

        const editor = ace.edit("aceEditorContainer"),
            editorContent = 'some value';
        editor.setValue(editorContent);

        window.dispatchEvent(evt);

        expect(evt.preventDefault).toHaveBeenCalled();
    });

    it("if the contents of the editor are unchanged and we try to navigate away from the page the event does not get cancelled",  () => {
        const evt = new Event('beforeunload');
        spyOn(evt, 'preventDefault');

        onDOMContentLoaded();

        window.dispatchEvent(evt);

        expect(evt.preventDefault).not.toHaveBeenCalled();
    });

    it("if the contents of the editor are changed and we submit the form the event does not get cancelled",  () => {
        const evt = new Event('beforeunload');
        spyOn(evt, 'preventDefault');

        onDOMContentLoaded();

        const editor = ace.edit("aceEditorContainer"),
            editorContent = 'some value';
        editor.setValue(editorContent);

        elForm.dispatchEvent(new Event('submit'));
        window.dispatchEvent(evt);

        expect(evt.preventDefault).not.toHaveBeenCalled();
    });
});
