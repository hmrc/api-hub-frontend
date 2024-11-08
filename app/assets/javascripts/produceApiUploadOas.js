import {setVisible} from "./utils.js";

export function onDOMContentLoaded() {
    const elFileDrop = document.getElementById('fileDrop'),
        elFileInput = document.getElementById('oasFile'),
        elFileNameInput = document.getElementById('fileName'),
        elFileContentsInput = document.getElementById('fileContents'),
        elErrorMessage = document.getElementById('errorMessage'),
        elSelectedFileName = document.getElementById('selectedFileName'),
        // elErrorSummary will be null unless we previously tried to continue without selecting a file
        elErrorSummary = document.querySelector('.govuk-error-summary'),
        maxFileSizeMB = elFileDrop.dataset.maxsize,
        maxFileSizeBytes = maxFileSizeMB * 1024 * 1024,
        validFileExtensions = new Set(['yaml', 'yml']),
        cssHoveringClass = 'hovering';

    /* By default if we drop a file into the browser it will open the file, replacing the current page. We
    * need to turn this behaviour off for the whole page, and run some custom code if the drop occurred within
    * the file drop area. */
    window.addEventListener('dragover', e => {
        e.preventDefault();
    });
    window.addEventListener('drop', e => {
        e.preventDefault();
    });

    elFileDrop.addEventListener('dragover', e => {
        elFileDrop.classList.add(cssHoveringClass);
    });
    elFileDrop.addEventListener('dragleave', e => {
        elFileDrop.classList.remove(cssHoveringClass);
    });

    elFileDrop.addEventListener('drop', e => {
        elFileDrop.classList.remove(cssHoveringClass);
        onFileSelection(e.dataTransfer.files);
    });
    elFileInput.addEventListener('change', e => {
        onFileSelection(elFileInput.files);
    });

    function onFileSelectionError(message) {
        elErrorMessage.textContent = message;
        setVisible(elErrorMessage, true);

        elSelectedFileName.textContent = '';
        setVisible(elSelectedFileName, false);

        elFileInput.value = '';
        elFileNameInput.value = '';
        elFileContentsInput.value = '';
    }

    function onFileSelectionSuccess(file) {
        file.text().then(fileContents => {
            elErrorMessage.textContent = '';
            setVisible(elErrorMessage, false);

            elSelectedFileName.textContent = `Selected file: ${file.name} (${file.size.toLocaleString()} bytes)`;
            setVisible(elSelectedFileName, true);

            elFileNameInput.value = file.name;
            elFileContentsInput.value = fileContents;

            if (elErrorSummary) {
                setVisible(elErrorSummary, false);
            }
        }).catch(err => console.error(err));
    }

    function isFileExtensionValid(file) {
        const fileNameParts = file.name.split('.');
        if (fileNameParts.length < 2) {
            return false;
        }

        const fileExtension = fileNameParts[fileNameParts.length - 1].toLowerCase();
        return validFileExtensions.has(fileExtension);
    }

    function onFileSelection(files) {
        if (!files || files.length === 0) {
            return;

        } else if (files.length > 1) {
            onFileSelectionError('Only one file may be selected.');
            return;
        }

        const file = files[0];
        if (file.size > maxFileSizeBytes) {
            onFileSelectionError(`File is too large. Maximum file size is ${maxFileSizeMB}MB.`);
            return;

        } else if (!isFileExtensionValid(file)) {
            const validExtensions = Array.from(validFileExtensions).map(ext => '.' + ext).join(' or ');
            onFileSelectionError(`File has the wrong type. File must be ${validExtensions}`);
            return;
        }

        onFileSelectionSuccess(file);
    }

    function processHiddenInputFields() {
        //TODO
    }

    processHiddenInputFields();
}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onDOMContentLoaded);
}
