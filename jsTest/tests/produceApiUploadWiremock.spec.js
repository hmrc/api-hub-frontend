import {JSDOM} from "jsdom";
import {onDOMContentLoaded} from "../../app/assets/javascripts/produceApiUploadWiremock.js";
import {isVisible} from "../../app/assets/javascripts/utils.js";

describe('onDOMContentLoaded', () => {
    let document, window;

    beforeEach(() => {
        const dom = new JSDOM(`
            <!DOCTYPE html>
            <div id="fileDrop" data-maxsize="5" data-validextensions="yaml,yml"></div>
            <input id="wiremockFile" name="wiremockFile">
            <p id="errorMessage"></p>
            <form>
                <input type="hidden" name="fileName" id="fileName">
                <input type="hidden" name="fileContents" id="fileContents">
            </form>            
            <p id="selectedFileName"></p>
        `);
        window = dom.window;
        document = dom.window.document;
        globalThis.window = window;
        globalThis.document = document;
        globalThis.Event = window.Event;
    });

    function buildFile(size, content, name) {
        return {
            size,
            text(){
                return {
                    then: (callback) => callback(content)
                };
            },
            name
        };
    }

    function selectFileUsingDialog(file) {
        const elFilesInput = document.getElementById('wiremockFile');
        spyOnProperty(elFilesInput, 'files').and.returnValue([file]);
        elFilesInput.dispatchEvent(new Event('change'));
    }

    function selectFilesUsingDragAndDrop(...files) {
        const elFileDropTarget = document.getElementById('fileDrop'),
            dropEvent = new Event('drop');
        dropEvent.dataTransfer = {files};
        elFileDropTarget.dispatchEvent(dropEvent);
    }

    function expectSuccessState(fileMessage, fileName, fileContents) {
        const elErrorMessage = document.getElementById('errorMessage'),
            elSelectedFileName = document.getElementById('selectedFileName');
        expect(isVisible(elErrorMessage)).toBeFalse();
        expect(isVisible(elSelectedFileName)).toBeTrue();
        expect(elSelectedFileName.textContent).toEqual(fileMessage);
        expect(document.getElementById('fileName').value).toEqual(fileName);
        expect(document.getElementById('fileContents').value).toEqual(fileContents);
    }

    function expectErrorState(errorMessage) {
        const elErrorMessage = document.getElementById('errorMessage'),
            elSelectedFileName = document.getElementById('selectedFileName');
        expect(isVisible(elErrorMessage)).toBeTrue();
        expect(elErrorMessage.textContent).toEqual(errorMessage);
        expect(isVisible(elSelectedFileName)).toBeFalse();
        expect(document.getElementById('fileName').value).toEqual('');
        expect(document.getElementById('fileContents').value).toEqual('');
    }

    it("when a valid file is selected via the dialog box the form is updated and no errors displayed",  () => {
        const fileName = 'file.yaml',
            fileSize = 1234,
            fileContents = 'some content';

        onDOMContentLoaded();

        selectFileUsingDialog(buildFile(fileSize, fileContents, fileName));

        expectSuccessState('Selected file: file.yaml (1,234 bytes)', fileName, fileContents);
    });

    it("when a valid file is selected via drag/drop the form is updated and no errors displayed",  () => {
        const fileName = 'file.yaml',
            fileSize = 1234,
            fileContents = 'some content';

        onDOMContentLoaded();

        selectFilesUsingDragAndDrop(buildFile(fileSize, fileContents, fileName));

        expectSuccessState('Selected file: file.yaml (1,234 bytes)', fileName, fileContents);
    });

    it("when a file that is too large is selected then form is not updated and an error is displayed",  () => {
        onDOMContentLoaded();

        selectFileUsingDialog(buildFile(5 * 1024 * 1024 + 1, 'some content', 'file.yaml'));

        expectErrorState('File is too large. Maximum file size is 5MB.');
    });

    it("when more than 1 file is selected then form is not updated and an error is displayed",  () => {
        onDOMContentLoaded();

        selectFilesUsingDragAndDrop(
            buildFile(100, 'contents1', 'file1.yaml'),
            buildFile(200, 'contents2', 'file2.yaml')
        );

        expectErrorState('Only one file may be selected.');
    });

    it("when a file of the wrong type is selected then form is not updated and an error is displayed",  () => {
        onDOMContentLoaded();

        selectFileUsingDialog(buildFile(100, 'some content', 'file.json'));

        expectErrorState('File has the wrong type. File must be .yaml or .yml');
    });

    it("when the form is prepopulated then file details are displayed correctly",  () => {
        const fileName = 'file.yaml',
            fileContents = 'some content';

        document.getElementById('fileName').value = fileName;
        document.getElementById('fileContents').value = fileContents;

        onDOMContentLoaded();

        expectSuccessState('Selected file: file.yaml (12 bytes)', fileName, fileContents);
    });
});
