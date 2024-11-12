import {noop, setVisible} from "./utils.js";

export function onDOMContentLoaded() {
    const view = (() => {
        const elFileDrop = document.getElementById('fileDrop'),
            elFileInput = document.getElementById('oasFile'),
            elFileNameInput = document.getElementById('fileName'),
            elFileContentsInput = document.getElementById('fileContents'),
            elErrorMessage = document.getElementById('errorMessage'),
            elSelectedFileName = document.getElementById('selectedFileName'),
            // elErrorSummary will be null unless we previously tried to continue without selecting a file
            elErrorSummary = document.querySelector('.govuk-error-summary'),
            cssHoveringClass = 'hovering';

        let fileSelectionHandler = noop;

        function showSelectedFileDetails(fileName, fileSize) {
            elSelectedFileName.textContent = `Selected file: ${fileName} (${fileSize.toLocaleString()} bytes)`;
            setVisible(elSelectedFileName, true);
        }

        return {
            initialise() {
                /* By default if we drop a file into the browser it will open the file, replacing the current page. We
                * need to turn this behaviour off for the whole page, and run some custom code if the drop occurred within
                * the file drop area. */
                window.addEventListener('dragover', e => {
                    e.preventDefault();
                });
                window.addEventListener('drop', e => {
                    e.preventDefault();
                });

                elFileDrop.addEventListener('dragover', () => {
                    elFileDrop.classList.add(cssHoveringClass);
                });
                elFileDrop.addEventListener('dragleave', () => {
                    elFileDrop.classList.remove(cssHoveringClass);
                });

                elFileDrop.addEventListener('drop', e => {
                    elFileDrop.classList.remove(cssHoveringClass);
                    fileSelectionHandler(e.dataTransfer.files);
                });
                elFileInput.addEventListener('change', () => {
                    fileSelectionHandler(elFileInput.files);
                });

                if (elFileNameInput.value && elFileContentsInput.value) {
                    const fileBytes = new TextEncoder().encode(elFileContentsInput.value);
                    setVisible(elErrorMessage, false);
                    setVisible(elSelectedFileName, true);
                    showSelectedFileDetails(elFileNameInput.value, fileBytes.length);
                }
            },
            get maxFileSizeMB() {
                return elFileDrop.dataset.maxsize;
            },
            get validFileExtensions() {
                return new Set(elFileDrop.dataset.validextensions.split(','));
            },
            onFileSelected(handler) {
                fileSelectionHandler = handler;
            },
            showFileSelectionError(message) {
                elErrorMessage.textContent = message;
                setVisible(elErrorMessage, true);

                elSelectedFileName.textContent = '';
                setVisible(elSelectedFileName, false);
            },
            showFileSelectionSuccess(file) {
                elErrorMessage.textContent = '';
                setVisible(elErrorMessage, false);

                showSelectedFileDetails(file.name, file.size);

                if (elErrorSummary) {
                    setVisible(elErrorSummary, false);
                }
            },
            clearFormFields() {
                elFileInput.value = '';
                elFileNameInput.value = '';
                elFileContentsInput.value = '';
            },
            populateFormFields(fileName, fileContents) {
                elFileNameInput.value = fileName;
                elFileContentsInput.value = fileContents;
            }
        };
    })();

    function fileSelectionSuccess(file) {
        file.text().then(fileContents => {
            view.showFileSelectionSuccess(file);
            view.populateFormFields(file.name, fileContents);
        }).catch(err => console.error(err));
    }

    function fileSelectionError(message) {
        view.showFileSelectionError(message);
        view.clearFormFields();
    }

    function isFileExtensionValid(file) {
        const fileNameParts = file.name.split('.');
        if (fileNameParts.length < 2) {
            return false;
        }

        const fileExtension = fileNameParts[fileNameParts.length - 1].toLowerCase();
        return view.validFileExtensions.has(fileExtension);
    }

    function onFileSelection(files) {
        if (!files || files.length === 0) {
            return;

        } else if (files.length > 1) {
            fileSelectionError('Only one file may be selected.');
            return;
        }

        const file = files[0],
            maxFileSizeBytes = view.maxFileSizeMB * 1024 * 1024;

        if (file.size > maxFileSizeBytes) {
            fileSelectionError(`File is too large. Maximum file size is ${view.maxFileSizeMB}MB.`);
            return;

        } else if (!isFileExtensionValid(file)) {
            const validExtensions = Array.from(view.validFileExtensions).map(ext => '.' + ext).join(' or ');
            fileSelectionError(`File has the wrong type. File must be ${validExtensions}`);
            return;
        }

        fileSelectionSuccess(file);
    }

    view.initialise();
    view.onFileSelected(onFileSelection);
}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', onDOMContentLoaded);
}
