(function () {
    const body = document.body;
    const bodyDataset = body ? body.dataset : {};
    const modalBackdrops = document.querySelectorAll("[data-modal]");
    const modalTriggers = document.querySelectorAll("[data-open-modal]");
    const modalClosers = document.querySelectorAll("[data-close-modal]");
    const uploadForm = document.querySelector("[data-upload-form]");
    const uploadInput = uploadForm ? uploadForm.querySelector(".upload-file-input") : null;
    const uploadFolderSelect = uploadForm ? uploadForm.querySelector('select[name="folderId"]') : null;
    const uploadOverwriteExistingInput = uploadForm ? uploadForm.querySelector("[data-upload-overwrite-existing]") : null;
    const uploadDescriptionInput = uploadForm ? uploadForm.querySelector("[data-upload-description]") : null;
    const uploadExpiresInput = uploadForm ? uploadForm.querySelector("[data-upload-expires-input]") : null;
    const uploadProgress = uploadForm ? uploadForm.querySelector("[data-upload-progress]") : null;
    const uploadProgressLabel = uploadForm ? uploadForm.querySelector("[data-upload-progress-label]") : null;
    const uploadProgressBar = uploadForm ? uploadForm.querySelector("[data-upload-progress-bar]") : null;
    const uploadProgressValue = uploadForm ? uploadForm.querySelector("[data-upload-progress-value]") : null;
    const uploadQueue = uploadForm ? uploadForm.querySelector("[data-upload-queue]") : null;
    const uploadQueueList = uploadForm ? uploadForm.querySelector("[data-upload-queue-list]") : null;
    const uploadQueueMeta = uploadForm ? uploadForm.querySelector("[data-upload-queue-meta]") : null;
    const uploadFileName = document.querySelector("[data-upload-file-name]");
    const uploadModalName = "upload-modal";
    const pasteModalName = "paste-modal";
    const pasteModal = document.querySelector(`[data-modal="${pasteModalName}"]`);
    const pasteForm = pasteModal ? pasteModal.querySelector("[data-paste-form]") : null;
    const pasteFolderSelect = pasteForm ? pasteForm.querySelector("[data-paste-folder]") : null;
    const pasteFilenameInput = pasteForm ? pasteForm.querySelector("[data-paste-filename]") : null;
    const pasteDescriptionInput = pasteForm ? pasteForm.querySelector("[data-paste-description]") : null;
    const pasteExpiresInput = pasteForm ? pasteForm.querySelector("[data-paste-expires-input]") : null;
    const pasteKindIcon = pasteForm ? pasteForm.querySelector("[data-paste-kind-icon]") : null;
    const pasteKindTitle = pasteForm ? pasteForm.querySelector("[data-paste-kind-title]") : null;
    const pastePreviewMeta = pasteForm ? pasteForm.querySelector("[data-paste-preview-meta]") : null;
    const pastePreviewImageHost = pasteForm ? pasteForm.querySelector("[data-paste-preview-image-host]") : null;
    const pastePreviewText = pasteForm ? pasteForm.querySelector("[data-paste-preview-text]") : null;
    const pasteError = pasteForm ? pasteForm.querySelector("[data-paste-error]") : null;
    const warningModalName = "warning-modal";
    const warningModal = document.querySelector(`[data-modal="${warningModalName}"]`);
    const warningModalTitle = warningModal ? warningModal.querySelector("[data-warning-modal-title]") : null;
    const warningModalMessage = warningModal ? warningModal.querySelector("[data-warning-modal-message]") : null;
    const warningModalTarget = warningModal ? warningModal.querySelector("[data-warning-modal-target]") : null;
    const warningModalSkip = warningModal ? warningModal.querySelector("[data-warning-modal-skip]") : null;
    const warningModalConfirm = warningModal ? warningModal.querySelector("[data-warning-modal-confirm]") : null;
    const itemActionsModalName = "item-actions-modal";
    const itemActionsModal = document.querySelector(`[data-modal="${itemActionsModalName}"]`);
    const itemActionsName = itemActionsModal ? itemActionsModal.querySelector("[data-item-actions-name]") : null;
    const itemActionsIcon = itemActionsModal ? itemActionsModal.querySelector("[data-item-actions-icon]") : null;
    const itemCollaborativeAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-collaborative-action]") : null;
    const itemWebDavAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-webdav-action]") : null;
    const itemShareAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-share-action]") : null;
    const itemRenameAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-rename-action]") : null;
    const itemMoveAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-move-action]") : null;
    const itemPropertiesAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-properties-action]") : null;
    const itemDeleteAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-delete-action]") : null;
    const itemRenameModalName = "item-rename-modal";
    const itemRenameModal = document.querySelector(`[data-modal="${itemRenameModalName}"]`);
    const itemRenameResourceTypeInput = itemRenameModal ? itemRenameModal.querySelector("[data-item-rename-resource-type]") : null;
    const itemRenameResourceIdInput = itemRenameModal ? itemRenameModal.querySelector("[data-item-rename-resource-id]") : null;
    const itemRenameCurrentName = itemRenameModal ? itemRenameModal.querySelector("[data-item-rename-current-name]") : null;
    const itemRenameInput = itemRenameModal ? itemRenameModal.querySelector("[data-item-rename-input]") : null;
    const itemMoveModalName = "item-move-modal";
    const itemMoveModal = document.querySelector(`[data-modal="${itemMoveModalName}"]`);
    const itemMoveResourceTypeInput = itemMoveModal ? itemMoveModal.querySelector("[data-item-move-resource-type]") : null;
    const itemMoveResourceIdInput = itemMoveModal ? itemMoveModal.querySelector("[data-item-move-resource-id]") : null;
    const itemMoveCurrentName = itemMoveModal ? itemMoveModal.querySelector("[data-item-move-current-name]") : null;
    const itemMoveTargetInput = itemMoveModal ? itemMoveModal.querySelector("[data-item-move-target-input]") : null;
    const itemPropertiesModalName = "item-properties-modal";
    const itemPropertiesModal = document.querySelector(`[data-modal="${itemPropertiesModalName}"]`);
    const itemPropertiesResourceTypeInput = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-resource-type]") : null;
    const itemPropertiesResourceIdInput = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-resource-id]") : null;
    const itemPropertiesName = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-name]") : null;
    const itemPropertiesType = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-type]") : null;
    const itemPropertiesCreated = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-created]") : null;
    const itemPropertiesUpdated = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-updated]") : null;
    const itemPropertiesExpiresTitle = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-expires-title]") : null;
    const itemPropertiesExpiresInput = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-expires-input]") : null;
    const itemPropertiesExpiresEnabled = itemPropertiesModal ? itemPropertiesModal.querySelector("[data-item-properties-expires-enabled]") : null;
    const itemDeleteModal = document.querySelector('[data-modal="item-delete-modal"]');
    const itemDeleteForm = itemDeleteModal ? itemDeleteModal.querySelector("[data-item-delete-form]") : null;
    const itemDeleteResourceTypeInput = itemDeleteModal ? itemDeleteModal.querySelector("[data-item-delete-resource-type]") : null;
    const itemDeleteResourceIdInput = itemDeleteModal ? itemDeleteModal.querySelector("[data-item-delete-resource-id]") : null;
    const itemDeleteName = itemDeleteModal ? itemDeleteModal.querySelector("[data-item-delete-name]") : null;
    const shareModal = document.querySelector('[data-modal="share-modal"]');
    const shareForm = shareModal ? shareModal.querySelector("form") : null;
    const shareResourceTypeInput = shareForm ? shareForm.querySelector("[data-share-resource-type-input]") : null;
    const shareResourceIdInput = shareForm ? shareForm.querySelector("[data-share-resource-id-input]") : null;
    const shareFileNameInput = shareForm ? shareForm.querySelector("[data-share-file-name-input]") : null;
    const shareTitleInput = shareForm ? shareForm.querySelector('input[name="title"]') : null;
    const shareItemLabel = shareForm ? shareForm.querySelector("[data-share-item-label-view]") : null;
    const shareExpiresHoursInput = shareForm ? shareForm.querySelector("[data-share-expires-hours]") : null;
    const shareExpiresUnlimitedInput = shareForm ? shareForm.querySelector("[data-share-expires-unlimited]") : null;
    const shareAllowPreviewInput = shareForm ? shareForm.querySelector('input[name="allowPreview"]') : null;
    const shareAllowDownloadInput = shareForm ? shareForm.querySelector('input[name="allowDownload"]') : null;
    const shareCurrent = shareForm ? shareForm.querySelector("[data-share-current]") : null;
    const shareUrlInput = shareForm ? shareForm.querySelector("[data-share-url-input]") : null;
    const shareOpenLink = shareForm ? shareForm.querySelector("[data-share-open-link]") : null;
    const shareCopyLink = shareForm ? shareForm.querySelector("[data-share-copy-link]") : null;
    const shareDeleteTrigger = shareForm ? shareForm.querySelector("[data-share-delete-trigger]") : null;
    const shareExpiresTitle = shareForm ? shareForm.querySelector("[data-share-expires-title]") : null;
    const shareRightsTitle = shareForm ? shareForm.querySelector("[data-share-rights-title]") : null;
    const shareResult = shareForm ? shareForm.querySelector("[data-share-result]") : null;
    const shareResultText = shareForm ? shareForm.querySelector("[data-share-result-text]") : null;
    const shareDeleteModal = document.querySelector('[data-modal="share-delete-modal"]');
    const shareDeleteForm = shareDeleteModal ? shareDeleteModal.querySelector("form") : null;
    const shareDeleteResourceTypeInput = shareDeleteForm ? shareDeleteForm.querySelector("[data-share-delete-resource-type]") : null;
    const shareDeleteResourceIdInput = shareDeleteForm ? shareDeleteForm.querySelector("[data-share-delete-resource-id]") : null;
    const shareDeleteItemName = shareDeleteForm ? shareDeleteForm.querySelector("[data-share-delete-item-name]") : null;
    const collaborativeModal = document.querySelector('[data-modal="collaborative-access-modal"]');
    const collaborativeForm = collaborativeModal ? collaborativeModal.querySelector("form") : null;
    const collaborativeFolderIdInput = collaborativeForm ? collaborativeForm.querySelector("[data-collab-folder-id-input]") : null;
    const collaborativeFolderNameInput = collaborativeForm ? collaborativeForm.querySelector("[data-collab-folder-name-input]") : null;
    const collaborativeLoginsInput = collaborativeForm ? collaborativeForm.querySelector("[data-collab-logins-input]") : null;
    const collaborativeWriteInput = collaborativeForm ? collaborativeForm.querySelector("[data-collab-write-input]") : null;
    const collaborativeDeleteInput = collaborativeForm ? collaborativeForm.querySelector("[data-collab-delete-input]") : null;
    const collaborativePasswordNote = collaborativeForm ? collaborativeForm.querySelector("[data-collab-password-note]") : null;
    const webDavModalName = "webdav-folder-modal";
    const webDavModal = document.querySelector(`[data-modal="${webDavModalName}"]`);
    const webDavForm = webDavModal ? webDavModal.querySelector("form") : null;
    const webDavFolderIdInput = webDavForm ? webDavForm.querySelector("[data-webdav-folder-id-input]") : null;
    const webDavCurrent = webDavForm ? webDavForm.querySelector("[data-webdav-current]") : null;
    const webDavUrlInput = webDavForm ? webDavForm.querySelector("[data-webdav-url-input]") : null;
    const webDavDavUrlInput = webDavForm ? webDavForm.querySelector("[data-webdav-dav-url-input]") : null;
    const webDavWebDavUrlInput = webDavForm ? webDavForm.querySelector("[data-webdav-webdav-url-input]") : null;
    const webDavLoginTitle = webDavForm ? webDavForm.querySelector("[data-webdav-login-title]") : null;
    const webDavStatusTitle = webDavForm ? webDavForm.querySelector("[data-webdav-status-title]") : null;
    const webDavModeTitle = webDavForm ? webDavForm.querySelector("[data-webdav-mode-title]") : null;
    const webDavCopyUrl = webDavForm ? webDavForm.querySelector("[data-webdav-copy-url]") : null;
    const webDavCopyDavUrl = webDavForm ? webDavForm.querySelector("[data-webdav-copy-dav-url]") : null;
    const webDavCopyWebDavUrl = webDavForm ? webDavForm.querySelector("[data-webdav-copy-webdav-url]") : null;
    const webDavCopyLogin = webDavForm ? webDavForm.querySelector("[data-webdav-copy-login]") : null;
    const webDavFolderNameInput = webDavForm ? webDavForm.querySelector("[data-webdav-folder-name-input]") : null;
    const webDavPasswordNote = webDavForm ? webDavForm.querySelector("[data-webdav-password-note]") : null;
    const webDavLoginInput = webDavForm ? webDavForm.querySelector("[data-webdav-login-input]") : null;
    const webDavPasswordInput = webDavForm ? webDavForm.querySelector("[data-webdav-password-input]") : null;
    const webDavEnabledInput = webDavForm ? webDavForm.querySelector("[data-webdav-enabled-input]") : null;
    const webDavAllowWriteInput = webDavForm ? webDavForm.querySelector("[data-webdav-allow-write-input]") : null;
    const webDavRotateTokenInput = webDavForm ? webDavForm.querySelector("[data-webdav-rotate-token-input]") : null;
    const webDavDeleteButton = webDavForm ? webDavForm.querySelector("[data-webdav-delete-button]") : null;
    const copyTriggers = document.querySelectorAll("[data-copy-text]");
    const previewTriggers = document.querySelectorAll("[data-preview-trigger]");
    const previewModal = document.querySelector('[data-modal="preview-modal"]');
    const previewModalTitle = previewModal ? previewModal.querySelector("[data-preview-modal-title]") : null;
    const previewContentHost = previewModal ? previewModal.querySelector("[data-preview-content-host]") : null;
    const previewPrevButton = previewModal ? previewModal.querySelector("[data-preview-prev]") : null;
    const previewNextButton = previewModal ? previewModal.querySelector("[data-preview-next]") : null;
    const previewPosition = previewModal ? previewModal.querySelector("[data-preview-position]") : null;
    const previewDownloadLink = previewModal ? previewModal.querySelector("[data-preview-download-link]") : null;
    const previewShareLink = previewModal ? previewModal.querySelector("[data-preview-share-link]") : null;
    const toastStack = document.querySelector("[data-toast-stack]");
    const bulkDeleteForm = document.querySelector("[data-bulk-delete-form]");
    const bulkMoveForm = document.querySelector("[data-bulk-move-form]");
    const bulkDeleteTrigger = document.querySelector("[data-bulk-delete-trigger]");
    const bulkMoveTrigger = document.querySelector("[data-bulk-move-trigger]");
    const bulkToggleAll = document.querySelector("[data-bulk-toggle-all]");
    const bulkSelectItems = document.querySelectorAll("[data-bulk-select-item]");
    const dropTargets = document.querySelectorAll("[data-upload-dropzone], [data-upload-dropzone-inner]");
    const dragOverlay = document.querySelector("[data-drag-overlay]");
    const folderRows = document.querySelectorAll("[data-folder-row]");
    const inlineConfirmTriggers = document.querySelectorAll("[data-inline-confirm]");
    const expirationGroups = document.querySelectorAll("form");
    const pickerInputs = document.querySelectorAll('input[type="date"], input[type="datetime-local"]');
    let activeItemTrigger = null;
    let activeShareTrigger = null;
    let activeCollaborativeTrigger = null;
    let activePreviewIndex = -1;
    let activePreviewItems = [];
    let dragDepth = 0;
    let pendingWarningAction = null;
    let pendingWarningSkipAction = null;
    let pendingClipboardPayload = null;
    let pendingClipboardPreviewUrl = null;
    let activePreviewRequestId = 0;
    let modalStackSeed = 30;
    let selectedUploadItems = [];
    let uploadBatchRunning = false;
    let uploadBatchOverwriteExisting = false;
    let uploadBatchActiveCount = 0;
    let uploadBatchSuccessCount = 0;
    let uploadBatchFailureCount = 0;
    let uploadBatchRedirectUrl = "";
    let uploadItemSequence = 0;

    function syncBodyLockOffset() {
        if (!body) {
            return;
        }
        const hasOpenModal = [...modalBackdrops].some((item) => !item.hidden);
        if (!hasOpenModal) {
            body.style.removeProperty("--scrollbar-offset");
            return;
        }
        const scrollbarOffset = Math.max(0, window.innerWidth - document.documentElement.clientWidth);
        body.style.setProperty("--scrollbar-offset", `${scrollbarOffset}px`);
    }

    function openModal(name) {
        const modal = document.querySelector(`[data-modal="${name}"]`);
        if (!modal) {
            return;
        }
        syncBodyLockOffset();
        modalStackSeed += 2;
        modal.style.zIndex = String(modalStackSeed);
        modal.hidden = false;
        body.classList.add("modal-open");
    }

    function closeModal(name) {
        const modal = document.querySelector(`[data-modal="${name}"]`);
        if (!modal) {
            return;
        }
        if (name === uploadModalName) {
            abortUploadBatch();
            if (uploadForm) {
                uploadForm.reset();
            }
            if (uploadInput) {
                uploadInput.value = "";
            }
            updateUploadFileName(null);
            resetUploadProgressState();
        }
        if (name === pasteModalName) {
            resetPasteModalState();
        }
        if (name === warningModalName) {
            resetWarningModalState();
        }
        modal.hidden = true;
        modal.style.removeProperty("z-index");
        if (![...modalBackdrops].some((item) => !item.hidden)) {
            body.classList.remove("modal-open");
            modalStackSeed = 30;
        }
        syncBodyLockOffset();
    }

    function isTruthyAttributeValue(value) {
        if (value == null) {
            return false;
        }
        const normalized = String(value).trim().toLowerCase();
        return normalized === "true" || normalized === "1" || normalized === "yes" || normalized === "on";
    }

    function updateUploadFileName(files) {
        if (!uploadFileName) {
            return;
        }
        if (!files || files.length === 0) {
            uploadFileName.textContent = "Файл еще не выбран";
            return;
        }
        if (files.length === 1) {
            uploadFileName.textContent = files[0].name;
            return;
        }
        uploadFileName.textContent = `Выбрано файлов: ${files.length}`;
    }

    function resetUploadProgressState() {
        if (uploadProgress) {
            uploadProgress.hidden = true;
            uploadProgress.classList.remove("is-processing");
        }
        if (uploadProgressLabel) {
            uploadProgressLabel.textContent = "Загрузка файла";
        }
        if (uploadProgressBar) {
            uploadProgressBar.style.width = "0%";
        }
        if (uploadProgressValue) {
            uploadProgressValue.textContent = "0%";
        }
        if (uploadForm) {
            uploadForm.classList.remove("is-uploading");
        }
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function buildUploadQueueItems(files) {
        return Array.from(files || []).filter(Boolean).map((file) => ({
            id: ++uploadItemSequence,
            file,
            overwriteExisting: false,
            status: "queued",
            progress: 0,
            loadedBytes: 0,
            error: "",
            xhr: null
        }));
    }

    function setSelectedUploadFiles(files) {
        selectedUploadItems = buildUploadQueueItems(files);
        syncUploadSelectionState();
        renderUploadQueue();
    }

    function syncUploadSelectionState() {
        if (uploadQueue) {
            uploadQueue.hidden = selectedUploadItems.length === 0;
        }
        if (!uploadFileName) {
            return;
        }
        if (!selectedUploadItems.length) {
            uploadFileName.textContent = "Файл еще не выбран";
            if (uploadInput) {
                uploadInput.value = "";
            }
            return;
        }
        if (selectedUploadItems.length === 1) {
            uploadFileName.textContent = selectedUploadItems[0].file.name;
            return;
        }
        uploadFileName.textContent = `Выбрано файлов: ${selectedUploadItems.length}`;
    }

    function abortUploadBatch() {
        selectedUploadItems.forEach((item) => {
            if (item.xhr) {
                try {
                    item.xhr.abort();
                } catch (_) {
                    // Ignore abort race.
                }
                item.xhr = null;
            }
        });
        selectedUploadItems = [];
        uploadBatchRunning = false;
        uploadBatchOverwriteExisting = false;
        uploadBatchActiveCount = 0;
        uploadBatchSuccessCount = 0;
        uploadBatchFailureCount = 0;
        uploadBatchRedirectUrl = "";
        syncUploadSelectionState();
        renderUploadQueue();
    }

    function canRemoveUploadItem(item) {
        return !!item && item.status !== "done";
    }

    function removeUploadQueueItem(itemId) {
        const item = selectedUploadItems.find((entry) => entry.id === itemId);
        if (!item || !canRemoveUploadItem(item)) {
            return;
        }
        if (item.xhr) {
            try {
                item.xhr.abort();
            } catch (_) {
                // Ignore abort race.
            }
        }
        const previousStatus = item.status;
        selectedUploadItems = selectedUploadItems.filter((entry) => entry.id !== itemId);
        if (previousStatus === "error") {
            uploadBatchFailureCount = Math.max(0, uploadBatchFailureCount - 1);
        }
        if (!selectedUploadItems.length) {
            uploadBatchRunning = false;
            uploadBatchActiveCount = 0;
            uploadBatchSuccessCount = 0;
            uploadBatchFailureCount = 0;
            uploadBatchRedirectUrl = "";
            resetUploadProgressState();
        }
        syncUploadSelectionState();
        renderUploadQueue();
        if (uploadBatchRunning) {
            pumpUploadQueue();
        }
    }

    function renderUploadQueue() {
        if (!uploadQueueList) {
            return;
        }
        if (!selectedUploadItems.length) {
            uploadQueueList.replaceChildren();
            if (uploadQueueMeta) {
                uploadQueueMeta.textContent = "0 файлов";
            }
            return;
        }

        uploadQueueList.innerHTML = selectedUploadItems.map((item) => {
            const statusText = item.status === "done"
                ? "Готово"
                : item.status === "error"
                    ? `Ошибка${item.error ? `: ${escapeHtml(item.error)}` : ""}`
                    : item.status === "processing"
                        ? "Сохранение файла..."
                        : item.status === "uploading"
                            ? "Загрузка..."
                            : "Ожидает";
            const overwriteBadge = item.overwriteExisting ? '<span class="upload-queue-item-flag">Перезапись</span>' : "";
            const removeButton = canRemoveUploadItem(item)
                ? `<button type="button" class="upload-queue-item-remove" data-upload-queue-remove="${item.id}" aria-label="Убрать файл из очереди" title="Убрать из очереди">×</button>`
                : "";
            return `
                <div class="upload-queue-item is-${item.status}">
                    <div class="upload-queue-item-head">
                        <div class="upload-queue-item-name-wrap">
                            <div class="upload-queue-item-name" title="${escapeHtml(item.file.name)}">${escapeHtml(item.file.name)}</div>
                            ${overwriteBadge}
                        </div>
                        <div class="upload-queue-item-actions">
                            <div class="upload-queue-item-progress">${Math.max(0, Math.min(100, Math.round(item.progress)))}%</div>
                            ${removeButton}
                        </div>
                    </div>
                    <div class="upload-queue-item-meta">
                        <span>${formatFileSize(item.file.size)}</span>
                        <span>${statusText}</span>
                    </div>
                    <div class="upload-queue-item-track" aria-hidden="true">
                        <div class="upload-queue-item-bar" style="width:${Math.max(0, Math.min(100, item.progress))}%"></div>
                    </div>
                </div>
            `;
        }).join("");

        if (uploadQueueMeta) {
            const completed = selectedUploadItems.filter((item) => item.status === "done").length;
            const failed = selectedUploadItems.filter((item) => item.status === "error").length;
            uploadQueueMeta.textContent = `${selectedUploadItems.length} файлов • ${completed} готово • ${failed} ошибок`;
        }

        uploadQueueList.querySelectorAll("[data-upload-queue-remove]").forEach((button) => {
            button.addEventListener("click", function () {
                const rawId = this.getAttribute("data-upload-queue-remove");
                const itemId = Number(rawId);
                if (Number.isFinite(itemId)) {
                    removeUploadQueueItem(itemId);
                }
            });
        });
    }

    function syncUploadBatchProgress() {
        const totalBytes = selectedUploadItems.reduce((sum, item) => sum + Math.max(1, item.file.size || 0), 0);
        if (!totalBytes) {
            resetUploadProgressState();
            renderUploadQueue();
            return;
        }

        let processedBytes = 0;
        let hasProcessing = false;
        selectedUploadItems.forEach((item) => {
            const size = Math.max(1, item.file.size || 0);
            if (item.status === "done") {
                processedBytes += size;
                item.progress = 100;
                return;
            }
            if (item.status === "processing") {
                processedBytes += size * 0.95;
                item.progress = Math.max(item.progress, 95);
                hasProcessing = true;
                return;
            }
            if (item.status === "uploading") {
                processedBytes += Math.min(size, item.loadedBytes) * 0.95;
                return;
            }
        });

        const overallPercent = Math.max(0, Math.min(100, Math.round((processedBytes / totalBytes) * 100)));
        if (uploadProgress) {
            uploadProgress.hidden = false;
            uploadProgress.classList.toggle("is-processing", hasProcessing);
        }
        if (uploadProgressLabel) {
            uploadProgressLabel.textContent = hasProcessing ? "Сохранение файлов..." : "Загрузка файлов";
        }
        if (uploadProgressBar) {
            uploadProgressBar.style.width = `${overallPercent}%`;
        }
        if (uploadProgressValue) {
            uploadProgressValue.textContent = `${overallPercent}%`;
        }
        if (uploadForm) {
            uploadForm.classList.add("is-uploading");
        }
        renderUploadQueue();
    }

    function buildUploadFormData(file, overwriteExisting) {
        const formData = new FormData(uploadForm);
        if (uploadInput && uploadInput.name) {
            formData.delete(uploadInput.name);
            formData.append(uploadInput.name, file, file.name);
        }
        if (uploadOverwriteExistingInput && uploadOverwriteExistingInput.name) {
            formData.delete(uploadOverwriteExistingInput.name);
            formData.append(uploadOverwriteExistingInput.name, overwriteExisting ? "true" : "false");
        }
        return formData;
    }

    function finishUploadBatch() {
        uploadBatchRunning = false;
        uploadBatchActiveCount = 0;
        const total = selectedUploadItems.length;
        if (uploadBatchFailureCount === 0 && uploadBatchSuccessCount > 0 && uploadBatchRedirectUrl) {
            const message = `Загружено файлов: ${uploadBatchSuccessCount}.`;
            window.sessionStorage.setItem("agtydrive_upload_toast_message", message);
            window.location.href = uploadBatchRedirectUrl;
            return;
        }
        resetUploadProgressState();
        renderUploadQueue();
        if (uploadBatchFailureCount > 0) {
            openWarningModal({
                title: "Загрузка не завершена",
                message: `Не удалось загрузить файлов: ${uploadBatchFailureCount}. Проверьте очередь ниже.`,
                confirmText: "Понятно",
                danger: false
            });
        }
    }

    function pumpUploadQueue() {
        if (!uploadBatchRunning) {
            return;
        }
        const maxParallelUploads = 5;
        while (uploadBatchActiveCount < maxParallelUploads) {
            const nextItem = selectedUploadItems.find((item) => item.status === "queued");
            if (!nextItem) {
                break;
            }
            uploadQueueItem(nextItem);
        }

        const hasPending = selectedUploadItems.some((item) => item.status === "queued" || item.status === "uploading" || item.status === "processing");
        if (!hasPending && uploadBatchActiveCount === 0) {
            finishUploadBatch();
        }
    }

    function uploadQueueItem(item) {
        if (!uploadForm || !item || item.status !== "queued") {
            return;
        }

        const xhr = new XMLHttpRequest();
        item.xhr = xhr;
        item.status = "uploading";
        item.progress = 0;
        item.loadedBytes = 0;
        item.error = "";
        uploadBatchActiveCount += 1;
        syncUploadBatchProgress();

        xhr.open("POST", uploadForm.action, true);
        xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        xhr.responseType = "json";

        xhr.upload.addEventListener("progress", function (event) {
            if (!event.lengthComputable) {
                return;
            }
            item.loadedBytes = event.loaded;
            item.progress = Math.max(0, Math.min(95, Math.round((event.loaded / Math.max(1, event.total)) * 95)));
            syncUploadBatchProgress();
        });

        xhr.upload.addEventListener("load", function () {
            item.status = "processing";
            item.progress = 95;
            item.loadedBytes = item.file.size || item.loadedBytes;
            syncUploadBatchProgress();
        });

        xhr.addEventListener("load", function () {
            const response = xhr.response && typeof xhr.response === "object" ? xhr.response : null;
            item.xhr = null;
            uploadBatchActiveCount = Math.max(0, uploadBatchActiveCount - 1);
            if (xhr.status >= 200 && xhr.status < 300 && response && response.redirectUrl) {
                item.status = "done";
                item.progress = 100;
                item.loadedBytes = item.file.size || item.loadedBytes;
                uploadBatchSuccessCount += 1;
                uploadBatchRedirectUrl = response.redirectUrl;
            } else {
                item.status = "error";
                item.error = response && response.error ? response.error : "Не удалось загрузить файл.";
                item.progress = 100;
                uploadBatchFailureCount += 1;
            }
            syncUploadBatchProgress();
            pumpUploadQueue();
        });

        xhr.addEventListener("error", function () {
            item.xhr = null;
            item.status = "error";
            item.error = "Ошибка соединения.";
            item.progress = 100;
            uploadBatchActiveCount = Math.max(0, uploadBatchActiveCount - 1);
            uploadBatchFailureCount += 1;
            syncUploadBatchProgress();
            pumpUploadQueue();
        });

        xhr.addEventListener("abort", function () {
            item.xhr = null;
            item.status = "queued";
            item.progress = 0;
            item.loadedBytes = 0;
            uploadBatchActiveCount = Math.max(0, uploadBatchActiveCount - 1);
            syncUploadBatchProgress();
        });

        xhr.send(buildUploadFormData(item.file, item.overwriteExisting));
    }

    function startUploadBatch() {
        if (!selectedUploadItems.length || uploadBatchRunning) {
            return;
        }
        uploadBatchRunning = true;
        uploadBatchOverwriteExisting = selectedUploadItems.some((item) => item.overwriteExisting);
        uploadBatchActiveCount = 0;
        uploadBatchSuccessCount = 0;
        uploadBatchFailureCount = 0;
        uploadBatchRedirectUrl = "";
        selectedUploadItems.forEach((item) => {
            item.status = "queued";
            item.progress = 0;
            item.loadedBytes = 0;
            item.error = "";
            item.xhr = null;
        });
        syncUploadBatchProgress();
        pumpUploadQueue();
    }

    function submitSelectedUploadFlow() {
        if (!selectedUploadItems.length) {
            return;
        }

        const duplicateNames = new Set();
        const seenNames = new Set();
        selectedUploadItems.forEach((item) => {
            const normalized = normalizeUploadFilename(item.file.name);
            if (!normalized) {
                return;
            }
            if (seenNames.has(normalized)) {
                duplicateNames.add(item.file.name);
                return;
            }
            seenNames.add(normalized);
        });
        if (duplicateNames.size > 0) {
            openWarningModal({
                title: "Повторяющиеся имена в выборе",
                message: "В выбранной пачке есть файлы с одинаковыми именами. Уберите дубликаты и повторите загрузку.",
                confirmText: "Понятно",
                target: [...duplicateNames].slice(0, 3).join(", "),
                danger: false
            });
            return;
        }

        const existingNames = resolveExistingUploadNames();
        const duplicatesWithExisting = selectedUploadItems.filter((item) => existingNames.includes(normalizeUploadFilename(item.file.name)));
        if (uploadOverwriteExistingInput) {
            uploadOverwriteExistingInput.value = "false";
        }

        if (duplicatesWithExisting.length === 0) {
            selectedUploadItems.forEach((item) => {
                item.overwriteExisting = false;
            });
            startUploadBatch();
            return;
        }

        openWarningModal({
            title: "Файлы уже существуют",
            message: `В этой директории уже есть файлов с такими именами: ${duplicatesWithExisting.length}. При перезаписи старые версии будут удалены вместе с их ссылками и настройками доступа.`,
            confirmText: "Перезаписать",
            skipText: "Пропустить",
            target: duplicatesWithExisting.length === 1 ? duplicatesWithExisting[0].file.name : `${duplicatesWithExisting.length} файлов`,
            onConfirm: () => {
                selectedUploadItems.forEach((item) => {
                    item.overwriteExisting = true;
                });
                startUploadBatch();
            },
            onSkip: () => {
                const conflictingNames = new Set(duplicatesWithExisting.map((item) => normalizeUploadFilename(item.file.name)));
                selectedUploadItems = selectedUploadItems.filter((item) => !conflictingNames.has(normalizeUploadFilename(item.file.name)));
                selectedUploadItems.forEach((item) => {
                    item.overwriteExisting = false;
                });
                syncUploadSelectionState();
                renderUploadQueue();
                if (!selectedUploadItems.length) {
                    openWarningModal({
                        title: "Загрузка отменена",
                        message: "После пропуска конфликтующих файлов в очереди ничего не осталось.",
                        confirmText: "Понятно",
                        danger: false
                    });
                    return;
                }
                startUploadBatch();
            }
        });
    }

    function normalizeUploadFilename(value) {
        if (!value) {
            return "";
        }
        return String(value).trim().toLowerCase();
    }

    function resolveExistingUploadNames() {
        if (uploadFolderSelect) {
            const selectedOption = uploadFolderSelect.options[uploadFolderSelect.selectedIndex];
            const raw = selectedOption ? selectedOption.getAttribute("data-existing-file-names") : "";
            return raw ? raw.split("\n").map((item) => item.trim().toLowerCase()).filter(Boolean) : [];
        }
        if (uploadForm) {
            const raw = uploadForm.getAttribute("data-existing-file-names");
            return raw ? raw.split("\n").map((item) => item.trim().toLowerCase()).filter(Boolean) : [];
        }
        return [];
    }

    function isEditableTarget(target) {
        if (!(target instanceof Element)) {
            return false;
        }
        const editable = target.closest("input, textarea, select, [contenteditable='true']");
        if (!editable) {
            return false;
        }
        if (editable instanceof HTMLInputElement && (editable.type === "checkbox" || editable.type === "radio" || editable.type === "button" || editable.type === "submit")) {
            return false;
        }
        return true;
    }

    function formatFileSize(size) {
        const value = Number(size) || 0;
        if (value < 1024) {
            return `${value} Б`;
        }
        if (value < 1024 * 1024) {
            return `${(value / 1024).toFixed(value < 10 * 1024 ? 1 : 0)} КБ`;
        }
        return `${(value / (1024 * 1024)).toFixed(value < 10 * 1024 * 1024 ? 1 : 0)} МБ`;
    }

    function getPreviewTriggerIdentity(trigger) {
        if (!trigger) {
            return "";
        }
        return trigger.getAttribute("data-preview-share-resource-id")
            || trigger.getAttribute("data-preview-src")
            || trigger.getAttribute("data-preview-title")
            || "";
    }

    function resolvePreviewItems(trigger) {
        const items = [];
        const seen = new Set();
        [...previewTriggers].forEach((item) => {
            const identity = getPreviewTriggerIdentity(item);
            if (!identity || seen.has(identity)) {
                return;
            }
            seen.add(identity);
            items.push(item);
        });
        if (!trigger) {
            return items;
        }
        const triggerIdentity = getPreviewTriggerIdentity(trigger);
        if (!triggerIdentity) {
            return items;
        }
        return items.filter((item) => {
            const itemType = item.getAttribute("data-preview-type") || "";
            const triggerType = trigger.getAttribute("data-preview-type") || "";
            if (itemType !== triggerType) {
                return false;
            }
            return true;
        });
    }

    function formatMediaTime(seconds) {
        if (!Number.isFinite(seconds) || seconds < 0) {
            return "00:00";
        }
        const total = Math.floor(seconds);
        const hours = Math.floor(total / 3600);
        const minutes = Math.floor((total % 3600) / 60);
        const secs = total % 60;
        if (hours > 0) {
            return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
        }
        return `${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
    }

    function readStoredMediaVolume() {
        const rawValue = window.localStorage.getItem("agtydrive_video_volume");
        const parsedValue = rawValue == null ? Number.NaN : Number.parseFloat(rawValue);
        if (!Number.isFinite(parsedValue)) {
            return 0.5;
        }
        return Math.min(1, Math.max(0, parsedValue));
    }

    function initCustomMediaPlayer(player) {
        if (!player) {
            return;
        }
        const media = player.querySelector("[data-video-element]");
        const mediaStage = player.querySelector("[data-media-stage]");
        const toggleButton = player.querySelector("[data-video-toggle]");
        const progressInput = player.querySelector("[data-video-progress]");
        const volumeInput = player.querySelector("[data-video-volume]");
        const timeElement = player.querySelector("[data-video-time]");
        const muteButton = player.querySelector("[data-video-mute]");
        if (!media || !toggleButton || !progressInput || !volumeInput || !timeElement) {
            return;
        }

        let isSeeking = false;

        const updateTime = () => {
            timeElement.textContent = `${formatMediaTime(media.currentTime)} / ${formatMediaTime(media.duration)}`;
        };

        const updateToggle = () => {
            toggleButton.classList.remove("is-play", "is-pause");
            if (media.paused) {
                toggleButton.classList.add("is-play");
                toggleButton.setAttribute("aria-label", "Воспроизвести");
            } else {
                toggleButton.classList.add("is-pause");
                toggleButton.setAttribute("aria-label", "Пауза");
            }
        };

        const updateMuteButton = () => {
            if (!muteButton) {
                return;
            }
            const muted = media.muted || media.volume === 0;
            muteButton.classList.toggle("is-muted", muted);
            muteButton.setAttribute("aria-label", muted ? "Включить звук" : "Выключить звук");
        };

        const applyVolume = (value) => {
            media.volume = value;
            media.muted = value === 0;
            volumeInput.value = String(Math.round(value * 100));
            updateMuteButton();
        };

        const seekToPercent = () => {
            if (!Number.isFinite(media.duration) || media.duration <= 0) {
                return;
            }
            const percent = Number.parseFloat(progressInput.value);
            if (!Number.isFinite(percent)) {
                return;
            }
            media.currentTime = (percent / 100) * media.duration;
            updateTime();
        };

        const seekToClientX = (clientX) => {
            if (!Number.isFinite(media.duration) || media.duration <= 0) {
                return;
            }
            const rect = progressInput.getBoundingClientRect();
            if (!rect.width) {
                return;
            }
            const offsetX = Math.min(rect.width, Math.max(0, clientX - rect.left));
            const percent = (offsetX / rect.width) * 100;
            progressInput.value = String(percent);
            media.currentTime = (percent / 100) * media.duration;
            updateTime();
        };

        applyVolume(readStoredMediaVolume());
        updateTime();
        updateToggle();
        player.classList.toggle("is-playing", !media.paused);

        const togglePlayback = () => {
            if (media.paused) {
                void media.play();
            } else {
                media.pause();
            }
        };

        toggleButton.addEventListener("click", togglePlayback);
        if (media.tagName === "VIDEO") {
            media.addEventListener("click", togglePlayback);
        } else if (mediaStage) {
            mediaStage.addEventListener("click", togglePlayback);
        }

        media.addEventListener("play", () => {
            updateToggle();
            player.classList.add("is-playing");
        });

        media.addEventListener("pause", () => {
            updateToggle();
            player.classList.remove("is-playing");
        });

        media.addEventListener("loadedmetadata", updateTime);
        media.addEventListener("timeupdate", () => {
            if (!isSeeking && Number.isFinite(media.duration) && media.duration > 0) {
                progressInput.value = String((media.currentTime / media.duration) * 100);
            } else if (!isSeeking) {
                progressInput.value = "0";
            }
            updateTime();
        });

        progressInput.addEventListener("pointerdown", () => {
            isSeeking = true;
        });
        progressInput.addEventListener("input", seekToPercent);
        progressInput.addEventListener("click", (event) => {
            seekToClientX(event.clientX);
        });
        progressInput.addEventListener("change", () => {
            seekToPercent();
            isSeeking = false;
        });
        progressInput.addEventListener("pointerup", (event) => {
            seekToClientX(event.clientX);
            isSeeking = false;
        });
        progressInput.addEventListener("blur", () => {
            isSeeking = false;
        });

        volumeInput.addEventListener("input", () => {
            const percent = Number.parseFloat(volumeInput.value);
            if (!Number.isFinite(percent)) {
                return;
            }
            const normalized = Math.min(1, Math.max(0, percent / 100));
            applyVolume(normalized);
            window.localStorage.setItem("agtydrive_video_volume", String(normalized));
        });

        if (muteButton) {
            muteButton.addEventListener("click", () => {
                if (media.muted || media.volume === 0) {
                    const restoredValue = readStoredMediaVolume();
                    applyVolume(restoredValue > 0 ? restoredValue : 0.5);
                } else {
                    applyVolume(0);
                }
            });
        }
    }

    async function loadPreviewText(src, title) {
        if (!previewContentHost || !src) {
            return;
        }
        const textNode = document.createElement("pre");
        textNode.className = "preview-text-block";
        textNode.textContent = "Загрузка предпросмотра...";
        previewContentHost.replaceChildren(textNode);
        const requestId = ++activePreviewRequestId;
        try {
            const response = await fetch(src, {credentials: "same-origin"});
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const text = await response.text();
            if (requestId !== activePreviewRequestId) {
                return;
            }
            textNode.textContent = text;
        } catch (_) {
            if (requestId !== activePreviewRequestId) {
                return;
            }
            textNode.textContent = `Не удалось загрузить предпросмотр файла «${title}».`;
        }
    }

    function buildClipboardTimestamp() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, "0");
        const day = String(now.getDate()).padStart(2, "0");
        const hours = String(now.getHours()).padStart(2, "0");
        const minutes = String(now.getMinutes()).padStart(2, "0");
        const seconds = String(now.getSeconds()).padStart(2, "0");
        return `${year}${month}${day}-${hours}${minutes}${seconds}`;
    }

    function extensionFromMimeType(mimeType) {
        switch ((mimeType || "").toLowerCase()) {
            case "image/png":
                return "png";
            case "image/jpeg":
                return "jpg";
            case "image/webp":
                return "webp";
            case "image/gif":
                return "gif";
            case "image/svg+xml":
                return "svg";
            case "text/plain":
                return "txt";
            default:
                return "";
        }
    }

    function suggestClipboardFilename(kind, mimeType) {
        const extension = extensionFromMimeType(mimeType);
        const baseName = kind === "image" ? "pasted-image" : "pasted-text";
        return extension ? `${baseName}-${buildClipboardTimestamp()}.${extension}` : `${baseName}-${buildClipboardTimestamp()}`;
    }

    function normalizeClipboardFilename(filename, mimeType) {
        const trimmed = (filename || "").trim();
        const fallback = suggestClipboardFilename(pendingClipboardPayload?.kind || "text", mimeType);
        if (!trimmed) {
            return fallback;
        }
        if (trimmed.includes(".")) {
            return trimmed;
        }
        const extension = extensionFromMimeType(mimeType);
        return extension ? `${trimmed}.${extension}` : trimmed;
    }

    function hidePasteError() {
        if (pasteError) {
            pasteError.hidden = true;
            pasteError.textContent = "Некорректные данные буфера.";
        }
    }

    function showPasteError(message) {
        if (pasteError) {
            pasteError.hidden = false;
            pasteError.textContent = message;
        }
    }

    function resetPasteModalState() {
        pendingClipboardPayload = null;
        hidePasteError();
        if (pasteForm) {
            pasteForm.reset();
            const expirationToggle = pasteForm.querySelector("[data-expiration-toggle]");
            if (expirationToggle && pasteExpiresInput) {
                syncExpirationGroup(expirationToggle, pasteExpiresInput);
            }
        }
        if (pendingClipboardPreviewUrl) {
            URL.revokeObjectURL(pendingClipboardPreviewUrl);
            pendingClipboardPreviewUrl = null;
        }
        if (pasteKindTitle) {
            pasteKindTitle.textContent = "Текст";
        }
        if (pasteKindIcon) {
            pasteKindIcon.textContent = "TXT";
        }
        if (pastePreviewMeta) {
            pastePreviewMeta.textContent = "0 Б";
        }
        if (pastePreviewImageHost) {
            pastePreviewImageHost.hidden = true;
            pastePreviewImageHost.replaceChildren();
        }
        if (pastePreviewText) {
            pastePreviewText.hidden = true;
            pastePreviewText.textContent = "";
        }
    }

    function populatePastePreview(payload) {
        if (!payload) {
            return;
        }
        hidePasteError();
        if (pasteFilenameInput) {
            pasteFilenameInput.value = payload.suggestedName || "";
        }
        if (pasteKindTitle) {
            pasteKindTitle.textContent = payload.kind === "image" ? "Изображение" : "Текст";
        }
        if (pasteKindIcon) {
            pasteKindIcon.textContent = payload.kind === "image" ? "IMG" : "TXT";
        }
        if (pastePreviewMeta) {
            pastePreviewMeta.textContent = `${formatFileSize(payload.size)} • ${payload.mimeType || "application/octet-stream"}`;
        }
        if (pastePreviewImageHost) {
            pastePreviewImageHost.hidden = payload.kind !== "image";
            pastePreviewImageHost.replaceChildren();
            if (payload.kind === "image") {
                pendingClipboardPreviewUrl = URL.createObjectURL(payload.blob);
                const image = document.createElement("img");
                image.alt = "Предпросмотр из буфера";
                image.src = pendingClipboardPreviewUrl;
                pastePreviewImageHost.appendChild(image);
            }
        }
        if (pastePreviewText) {
            if (payload.kind === "text") {
                pastePreviewText.hidden = false;
                pastePreviewText.textContent = payload.previewText || "";
            } else {
                pastePreviewText.hidden = true;
                pastePreviewText.textContent = "";
            }
        }
    }

    function openPasteModal(payload) {
        if (!pasteModal || !pasteForm) {
            return;
        }
        resetPasteModalState();
        pendingClipboardPayload = payload;
        populatePastePreview(payload);
        if (pasteFolderSelect) {
            ensureUploadFolderSelected();
            if (uploadFolderSelect && uploadFolderSelect.value) {
                pasteFolderSelect.value = uploadFolderSelect.value;
            } else {
                const firstAvailableOption = [...pasteFolderSelect.options].find((option) => option.value);
                if (firstAvailableOption) {
                    pasteFolderSelect.value = firstAvailableOption.value;
                }
            }
        }
        openModal(pasteModalName);
    }

    function resetWarningModalState() {
        pendingWarningAction = null;
        pendingWarningSkipAction = null;
        if (warningModalTitle) {
            warningModalTitle.textContent = "Подтверждение";
        }
        if (warningModalMessage) {
            warningModalMessage.textContent = "Подтвердите действие.";
        }
        if (warningModalTarget) {
            warningModalTarget.textContent = "";
            warningModalTarget.hidden = true;
        }
        if (warningModalConfirm) {
            warningModalConfirm.textContent = "Подтвердить";
            warningModalConfirm.classList.add("inline-link-danger");
        }
        if (warningModalSkip) {
            warningModalSkip.textContent = "Пропустить";
            warningModalSkip.hidden = true;
            warningModalSkip.style.display = "none";
        }
        if (uploadOverwriteExistingInput) {
            uploadOverwriteExistingInput.value = "false";
        }
    }

    function openWarningModal(options) {
        if (!warningModal) {
            if (typeof options?.onConfirm === "function") {
                options.onConfirm();
            }
            return;
        }
        if (warningModalTitle) {
            warningModalTitle.textContent = options?.title || "Подтверждение";
        }
        if (warningModalMessage) {
            warningModalMessage.textContent = options?.message || "Подтвердите действие.";
        }
        if (warningModalTarget) {
            const hasTarget = !!options?.target;
            warningModalTarget.hidden = !hasTarget;
            warningModalTarget.textContent = hasTarget ? options.target : "";
        }
        if (warningModalConfirm) {
            warningModalConfirm.textContent = options?.confirmText || "Подтвердить";
            warningModalConfirm.classList.toggle("inline-link-danger", options?.danger !== false);
        }
        if (warningModalSkip) {
            const hasSkipAction = typeof options?.onSkip === "function";
            warningModalSkip.hidden = !hasSkipAction;
            warningModalSkip.style.display = hasSkipAction ? "" : "none";
            warningModalSkip.textContent = options?.skipText || "Пропустить";
        }
        pendingWarningAction = typeof options?.onConfirm === "function" ? options.onConfirm : null;
        pendingWarningSkipAction = typeof options?.onSkip === "function" ? options.onSkip : null;
        openModal(warningModalName);
    }

    function openNativePicker(input) {
        if (!input || input.disabled || typeof input.showPicker !== "function") {
            return;
        }
        try {
            input.showPicker();
        } catch (error) {
            // Ignore browsers that block programmatic picker opening.
        }
    }

    function showToast(message, title) {
        if (!toastStack || !message) {
            return;
        }
        const toast = document.createElement("div");
        toast.className = "toast-card";
        toast.innerHTML = `
            <div class="toast-title">${title || "Уведомление"}</div>
            <div class="toast-message"></div>
        `;
        const messageNode = toast.querySelector(".toast-message");
        if (messageNode) {
            messageNode.textContent = message;
        }
        toastStack.appendChild(toast);

        const removeToast = () => {
            if (!toast.isConnected) {
                return;
            }
            toast.classList.add("is-leaving");
            window.setTimeout(() => {
                if (toast.isConnected) {
                    toast.remove();
                }
            }, 180);
        };

        window.setTimeout(removeToast, 3200);
    }

    function formatDateTimeLocal(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hours = String(date.getHours()).padStart(2, "0");
        const minutes = String(date.getMinutes()).padStart(2, "0");
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    }

    function applyExpirationPreset(input, preset) {
        if (!input || !preset) {
            return;
        }
        const date = new Date();
        switch (preset) {
            case "1h":
                date.setHours(date.getHours() + 1);
                break;
            case "24h":
                date.setHours(date.getHours() + 24);
                break;
            case "7d":
                date.setDate(date.getDate() + 7);
                break;
            case "30d":
                date.setDate(date.getDate() + 30);
                break;
            default:
                return;
        }
        input.value = formatDateTimeLocal(date);
    }

    function syncExpirationGroup(toggle, input) {
        if (!toggle || !input) {
            return;
        }
        const enabled = toggle.checked;
        input.disabled = !enabled;
        if (!enabled) {
            input.value = "";
        }
    }

    function initExpirationGroup(form) {
        if (!form) {
            return;
        }
        const toggle = form.querySelector("[data-expiration-toggle], [data-item-properties-expires-enabled]");
        const input = form.querySelector("[data-expiration-input]");
        const presetButtons = form.querySelectorAll("[data-expiration-preset]");
        if (!toggle || !input) {
            return;
        }

        if (toggle.hasAttribute("data-item-properties-expires-enabled")) {
            input.disabled = !toggle.checked;
        } else {
            toggle.checked = !!input.value;
            syncExpirationGroup(toggle, input);
        }

        toggle.addEventListener("change", function () {
            syncExpirationGroup(toggle, input);
        });

        presetButtons.forEach((button) => {
            button.addEventListener("click", function () {
                toggle.checked = true;
                input.disabled = false;
                applyExpirationPreset(input, button.getAttribute("data-expiration-preset"));
            });
        });
    }

    function collectBulkSelection() {
        const fileIds = [];
        const folderIds = [];
        bulkSelectItems.forEach((item) => {
            if (!item.checked) {
                return;
            }
            const type = item.getAttribute("data-bulk-item-type");
            const id = item.getAttribute("data-bulk-item-id");
            if (!id) {
                return;
            }
            if (type === "FILE") {
                fileIds.push(id);
            } else if (type === "FOLDER") {
                folderIds.push(id);
            }
        });
        return {fileIds, folderIds};
    }

    function applyBulkSelectionToForm(form) {
        if (!form) {
            return false;
        }
        const selection = collectBulkSelection();
        const fileInput = form.querySelector('[data-bulk-file-ids]');
        const folderInput = form.querySelector('[data-bulk-folder-ids]');
        if (fileInput) {
            fileInput.value = selection.fileIds.join(",");
        }
        if (folderInput) {
            folderInput.value = selection.folderIds.join(",");
        }
        return selection.fileIds.length > 0 || selection.folderIds.length > 0;
    }

    function ensureUploadFolderSelected() {
        if (!uploadForm || !uploadFolderSelect) {
            return;
        }
        const currentFolderId = uploadForm.getAttribute("data-current-folder-id");
        if (currentFolderId == null || currentFolderId === "") {
            uploadFolderSelect.value = "";
            return;
        }
        const hasCurrentFolderOption = [...uploadFolderSelect.options].some((option) => option.value === currentFolderId);
        if (hasCurrentFolderOption) {
            uploadFolderSelect.value = currentFolderId;
        }
    }

    function assignFilesToUploadInput(files) {
        if (!uploadInput || !files || files.length === 0) {
            return false;
        }
        const transfer = new DataTransfer();
        Array.from(files).forEach((file) => transfer.items.add(file));
        uploadInput.files = transfer.files;
        updateUploadFileName(transfer.files);
        return true;
    }

    function submitClipboardPayload() {
        if (!pendingClipboardPayload || !uploadForm || !uploadInput) {
            showPasteError("Буфер обмена пуст или недоступен для загрузки.");
            return;
        }
        const filename = normalizeClipboardFilename(pasteFilenameInput ? pasteFilenameInput.value : "", pendingClipboardPayload.mimeType);
        if (!filename) {
            showPasteError("Укажите имя файла.");
            return;
        }

        const file = new File([pendingClipboardPayload.blob], filename, {
            type: pendingClipboardPayload.mimeType || "application/octet-stream",
            lastModified: Date.now()
        });

        if (!assignFilesToUploadInput([file])) {
            showPasteError("Не удалось подготовить файл для загрузки.");
            return;
        }
        setSelectedUploadFiles([file]);

        if (uploadFolderSelect && pasteFolderSelect) {
            uploadFolderSelect.value = pasteFolderSelect.value;
        }
        ensureUploadFolderSelected();

        if (uploadDescriptionInput && pasteDescriptionInput) {
            uploadDescriptionInput.value = pasteDescriptionInput.value || "";
        }
        if (uploadExpiresInput && pasteExpiresInput) {
            uploadExpiresInput.value = pasteExpiresInput.value || "";
        }
        if (uploadOverwriteExistingInput) {
            uploadOverwriteExistingInput.value = "false";
        }

        closeModal(pasteModalName);
        submitSelectedUploadFlow();
    }

    function showGlobalDragOverlay() {
        if (dragOverlay) {
            dragOverlay.hidden = false;
            body.classList.add("drag-active");
        }
    }

    function hideGlobalDragOverlay() {
        if (dragOverlay) {
            dragOverlay.hidden = true;
            body.classList.remove("drag-active");
        }
    }

    function resetGlobalDragState() {
        dragDepth = 0;
        hideGlobalDragOverlay();
        dropTargets.forEach((target) => {
            target.classList.remove("is-dragover");
        });
    }

    function shouldResetDragStateForPointerExit(event) {
        if (!body || !body.classList.contains("drag-active")) {
            return false;
        }
        if (!event) {
            return true;
        }
        if (!event.relatedTarget) {
            return true;
        }
        return event.clientX <= 0
            || event.clientY <= 0
            || event.clientX >= window.innerWidth
            || event.clientY >= window.innerHeight;
    }

    function syncShareExpiryState() {
        if (!shareExpiresHoursInput || !shareExpiresUnlimitedInput) {
            return;
        }
        const disabled = shareExpiresUnlimitedInput.checked;
        shareExpiresHoursInput.disabled = disabled;
    }

    function resetItemActionsState() {
        activeItemTrigger = null;
        if (itemActionsName) {
            itemActionsName.textContent = "Имя файла";
        }
        if (itemActionsIcon) {
            itemActionsIcon.classList.remove("folder");
            itemActionsIcon.classList.add("file");
        }
        if (itemDeleteForm) {
            if (itemDeleteResourceTypeInput) {
                itemDeleteResourceTypeInput.value = "";
            }
            if (itemDeleteResourceIdInput) {
                itemDeleteResourceIdInput.value = "";
            }
        }
        if (itemDeleteName) {
            itemDeleteName.textContent = "Файл";
        }
        if (itemCollaborativeAction) {
            itemCollaborativeAction.hidden = true;
        }
        if (itemWebDavAction) {
            itemWebDavAction.hidden = true;
        }
    }

    function populateRenameState(trigger, preserveInputValue) {
        if (!trigger) {
            return;
        }
        const itemType = trigger.getAttribute("data-item-resource-type") || "FILE";
        const itemName = trigger.getAttribute("data-item-name") || "без названия";
        if (itemRenameResourceTypeInput) {
            itemRenameResourceTypeInput.value = itemType;
        }
        if (itemRenameResourceIdInput) {
            itemRenameResourceIdInput.value = trigger.getAttribute("data-item-resource-id") || "";
        }
        if (itemRenameCurrentName) {
            itemRenameCurrentName.textContent = itemName;
        }
        if (itemRenameInput && !preserveInputValue) {
            itemRenameInput.value = itemName;
            itemRenameInput.select();
        }
    }

    function populateMoveState(trigger, preserveSelection) {
        if (!trigger) {
            return;
        }
        const itemType = trigger.getAttribute("data-item-resource-type") || "FILE";
        const itemId = trigger.getAttribute("data-item-resource-id") || "";
        const itemName = trigger.getAttribute("data-item-name") || "без названия";
        const itemPathKey = trigger.getAttribute("data-item-path-key") || "";
        const currentParentId = trigger.getAttribute("data-item-current-parent-id") || "";

        if (itemMoveResourceTypeInput) {
            itemMoveResourceTypeInput.value = itemType;
        }
        if (itemMoveResourceIdInput) {
            itemMoveResourceIdInput.value = itemId;
        }
        if (itemMoveCurrentName) {
            itemMoveCurrentName.textContent = itemName;
        }

        if (!itemMoveTargetInput) {
            return;
        }

        [...itemMoveTargetInput.options].forEach((option) => {
            const optionPathKey = option.getAttribute("data-path-key") || "";
            const optionId = option.value || "";
            let disabled = false;

            if (itemType === "FOLDER" && itemPathKey) {
                disabled = optionId === itemId || (optionPathKey && optionPathKey.startsWith(itemPathKey + "/"));
            }

            option.disabled = disabled;
        });

        if (!preserveSelection) {
            itemMoveTargetInput.value = currentParentId;
        }
    }

    function syncItemPropertiesExpiryState() {
        if (!itemPropertiesExpiresInput || !itemPropertiesExpiresEnabled) {
            return;
        }
        itemPropertiesExpiresInput.disabled = !itemPropertiesExpiresEnabled.checked;
        if (!itemPropertiesExpiresEnabled.checked) {
            itemPropertiesExpiresInput.value = "";
        }
    }

    function populatePropertiesState(trigger) {
        if (!trigger) {
            return;
        }
        if (itemPropertiesResourceTypeInput) {
            itemPropertiesResourceTypeInput.value = trigger.getAttribute("data-item-resource-type") || "FILE";
        }
        if (itemPropertiesResourceIdInput) {
            itemPropertiesResourceIdInput.value = trigger.getAttribute("data-item-resource-id") || "";
        }
        if (itemPropertiesName) {
            itemPropertiesName.textContent = trigger.getAttribute("data-item-name") || "без названия";
        }
        if (itemPropertiesType) {
            itemPropertiesType.textContent = trigger.getAttribute("data-item-label") || "Файл";
        }
        if (itemPropertiesCreated) {
            itemPropertiesCreated.textContent = trigger.getAttribute("data-item-created-at") || "—";
        }
        if (itemPropertiesUpdated) {
            itemPropertiesUpdated.textContent = trigger.getAttribute("data-item-updated-at") || "—";
        }
        if (itemPropertiesExpiresTitle) {
            itemPropertiesExpiresTitle.textContent = trigger.getAttribute("data-item-expires-at") || "Без срока";
        }
        if (itemPropertiesExpiresInput) {
            itemPropertiesExpiresInput.value = trigger.getAttribute("data-item-expires-input") || "";
        }
        if (itemPropertiesExpiresEnabled) {
            itemPropertiesExpiresEnabled.checked = !isTruthyAttributeValue(trigger.getAttribute("data-item-expires-unlimited"));
        }
        syncItemPropertiesExpiryState();
    }

    function populateItemActionsState(trigger) {
        if (!trigger) {
            return;
        }
        activeItemTrigger = trigger;
        const itemType = trigger.getAttribute("data-item-resource-type") || "FILE";
        const itemLabel = trigger.getAttribute("data-item-label") || "Файл";
        const itemName = trigger.getAttribute("data-item-name") || "без названия";

        if (itemActionsName) {
            itemActionsName.textContent = itemName;
        }
        if (itemActionsIcon) {
            itemActionsIcon.classList.toggle("folder", itemType === "FOLDER");
            itemActionsIcon.classList.toggle("file", itemType !== "FOLDER");
        }
        if (itemDeleteForm) {
            if (itemDeleteResourceTypeInput) {
                itemDeleteResourceTypeInput.value = itemType;
            }
            if (itemDeleteResourceIdInput) {
                itemDeleteResourceIdInput.value = trigger.getAttribute("data-item-resource-id") || "";
            }
        }
        if (itemDeleteName) {
            itemDeleteName.textContent = `${itemLabel}: ${itemName}`;
        }
        if (itemCollaborativeAction) {
            itemCollaborativeAction.hidden = itemType !== "FOLDER";
        }
        if (itemWebDavAction) {
            itemWebDavAction.hidden = itemType !== "FOLDER";
        }
    }

    function resetCollaborativeState() {
        activeCollaborativeTrigger = null;
        if (collaborativeForm) {
            collaborativeForm.reset();
        }
        if (collaborativeFolderIdInput) {
            collaborativeFolderIdInput.value = "";
        }
        if (collaborativeFolderNameInput) {
            collaborativeFolderNameInput.value = "";
        }
        if (collaborativeLoginsInput) {
            collaborativeLoginsInput.value = "";
        }
        if (collaborativeWriteInput) {
            collaborativeWriteInput.checked = false;
        }
        if (collaborativeDeleteInput) {
            collaborativeDeleteInput.checked = false;
        }
        if (collaborativePasswordNote) {
            collaborativePasswordNote.hidden = true;
        }
    }

    function resetWebDavState() {
        if (webDavForm) {
            webDavForm.reset();
        }
        if (webDavFolderIdInput) {
            webDavFolderIdInput.value = "";
        }
        if (webDavFolderNameInput) {
            webDavFolderNameInput.value = "";
        }
        if (webDavCurrent) {
            webDavCurrent.hidden = true;
        }
        if (webDavUrlInput) {
            webDavUrlInput.value = "";
        }
        if (webDavDavUrlInput) {
            webDavDavUrlInput.value = "";
        }
        if (webDavWebDavUrlInput) {
            webDavWebDavUrlInput.value = "";
        }
        if (webDavLoginTitle) {
            webDavLoginTitle.textContent = "—";
        }
        if (webDavStatusTitle) {
            webDavStatusTitle.textContent = "Отключен";
        }
        if (webDavModeTitle) {
            webDavModeTitle.textContent = "Только чтение";
        }
        if (webDavPasswordNote) {
            webDavPasswordNote.hidden = true;
        }
        if (webDavLoginInput) {
            webDavLoginInput.value = "";
            webDavLoginInput.defaultValue = "";
        }
        if (webDavPasswordInput) {
            webDavPasswordInput.value = "";
            webDavPasswordInput.defaultValue = "";
        }
        if (webDavEnabledInput) {
            webDavEnabledInput.checked = false;
            webDavEnabledInput.defaultChecked = false;
        }
        if (webDavAllowWriteInput) {
            webDavAllowWriteInput.checked = false;
            webDavAllowWriteInput.defaultChecked = false;
        }
        if (webDavRotateTokenInput) {
            webDavRotateTokenInput.checked = false;
            webDavRotateTokenInput.defaultChecked = false;
        }
        if (webDavDeleteButton) {
            webDavDeleteButton.hidden = true;
        }
    }

    function buildWebDavSchemeUrl(baseUrl, login, scheme) {
        if (!baseUrl) {
            return "";
        }
        try {
            const parsed = new URL(baseUrl);
            const isSecure = parsed.protocol === "https:";
            const normalizedScheme = scheme || "dav";
            const effectiveScheme = isSecure
                ? (normalizedScheme === "webdav" ? "webdavs" : "davs")
                : normalizedScheme;
            const loginPart = login ? `${encodeURIComponent(login)}@` : "";
            return `${effectiveScheme}://${loginPart}${parsed.host}${parsed.pathname}`;
        } catch (_) {
            return "";
        }
    }

    function populateWebDavState(trigger) {
        if (!trigger) {
            return;
        }
        const folderId = trigger.getAttribute("data-webdav-folder-id") || "";
        const folderName = trigger.getAttribute("data-webdav-folder-name") || "";
        const enabled = isTruthyAttributeValue(trigger.getAttribute("data-webdav-enabled"));
        const allowWrite = isTruthyAttributeValue(trigger.getAttribute("data-webdav-allow-write"));
        const url = trigger.getAttribute("data-webdav-url") || "";
        const login = trigger.getAttribute("data-webdav-login") || "";
        const hasPassword = isTruthyAttributeValue(trigger.getAttribute("data-webdav-has-password"));

        if (webDavFolderIdInput) {
            webDavFolderIdInput.value = folderId;
        }
        if (webDavFolderNameInput) {
            webDavFolderNameInput.value = folderName;
        }
        if (webDavCurrent) {
            webDavCurrent.hidden = !url;
        }
        if (webDavUrlInput) {
            webDavUrlInput.value = url;
        }
        if (webDavDavUrlInput) {
            webDavDavUrlInput.value = buildWebDavSchemeUrl(url, login, "dav");
        }
        if (webDavWebDavUrlInput) {
            webDavWebDavUrlInput.value = buildWebDavSchemeUrl(url, login, "webdav");
        }
        if (webDavLoginTitle) {
            webDavLoginTitle.textContent = login || "—";
        }
        if (webDavStatusTitle) {
            webDavStatusTitle.textContent = enabled ? "Включен" : "Отключен";
        }
        if (webDavModeTitle) {
            webDavModeTitle.textContent = allowWrite ? "Чтение и запись" : "Только чтение";
        }
        if (webDavPasswordNote) {
            webDavPasswordNote.hidden = !hasPassword;
        }
        if (webDavLoginInput) {
            webDavLoginInput.value = login;
            webDavLoginInput.defaultValue = login;
        }
        if (webDavPasswordInput) {
            webDavPasswordInput.value = "";
            webDavPasswordInput.defaultValue = "";
        }
        if (webDavEnabledInput) {
            webDavEnabledInput.checked = enabled;
            webDavEnabledInput.defaultChecked = enabled;
        }
        if (webDavAllowWriteInput) {
            webDavAllowWriteInput.checked = allowWrite;
            webDavAllowWriteInput.defaultChecked = allowWrite;
        }
        if (webDavRotateTokenInput) {
            webDavRotateTokenInput.checked = false;
            webDavRotateTokenInput.defaultChecked = false;
        }
        if (webDavDeleteButton) {
            webDavDeleteButton.hidden = !url;
        }
    }

    function populateCollaborativeState(trigger) {
        if (!trigger) {
            return;
        }
        activeCollaborativeTrigger = trigger;
        if (collaborativeFolderIdInput) {
            collaborativeFolderIdInput.value = trigger.getAttribute("data-collab-folder-id") || "";
        }
        if (collaborativeFolderNameInput) {
            collaborativeFolderNameInput.value = trigger.getAttribute("data-collab-folder-name") || "";
        }
        if (collaborativeLoginsInput) {
            const loginsValue = trigger.getAttribute("data-collab-logins") || "";
            collaborativeLoginsInput.value = loginsValue;
            collaborativeLoginsInput.defaultValue = loginsValue;
        }
        if (collaborativeWriteInput) {
            const allowWrite = isTruthyAttributeValue(trigger.getAttribute("data-collab-write"));
            collaborativeWriteInput.checked = allowWrite;
            collaborativeWriteInput.defaultChecked = allowWrite;
        }
        if (collaborativeDeleteInput) {
            const allowDelete = isTruthyAttributeValue(trigger.getAttribute("data-collab-delete"));
            collaborativeDeleteInput.checked = allowDelete;
            collaborativeDeleteInput.defaultChecked = allowDelete;
        }
        if (collaborativePasswordNote) {
            collaborativePasswordNote.hidden = !isTruthyAttributeValue(trigger.getAttribute("data-collab-password-protected"));
        }
    }

    function resetShareState() {
        if (shareForm) {
            shareForm.reset();
        }
        if (shareResourceIdInput) {
            shareResourceIdInput.value = "";
        }
        if (shareResourceTypeInput) {
            shareResourceTypeInput.value = "FILE";
        }
        if (shareFileNameInput) {
            shareFileNameInput.value = "";
        }
        if (shareTitleInput) {
            shareTitleInput.value = "";
        }
        if (shareItemLabel) {
            shareItemLabel.textContent = "Файл";
        }
        if (shareCurrent) {
            shareCurrent.hidden = true;
        }
        if (shareUrlInput) {
            shareUrlInput.value = "";
        }
        if (shareOpenLink) {
            shareOpenLink.href = "#";
        }
        if (shareExpiresTitle) {
            shareExpiresTitle.textContent = "Без срока действия";
        }
        if (shareRightsTitle) {
            shareRightsTitle.textContent = "Просмотр и скачивание";
        }
        if (shareResult) {
            shareResult.hidden = true;
        }
        if (shareResultText) {
            shareResultText.textContent = "Публичная ссылка создана.";
        }
        syncShareExpiryState();
    }

    function populateShareState(trigger) {
        if (!trigger) {
            return;
        }
        activeShareTrigger = trigger;
        if (shareResourceTypeInput) {
            shareResourceTypeInput.value = trigger.getAttribute("data-share-resource-type") || "FILE";
        }
        if (shareResourceIdInput) {
            shareResourceIdInput.value = trigger.getAttribute("data-share-resource-id") || "";
        }
        if (shareFileNameInput) {
            shareFileNameInput.value = trigger.getAttribute("data-share-item-name") || "";
        }
        if (shareTitleInput) {
            shareTitleInput.value = trigger.getAttribute("data-share-title") || "";
        }
        if (shareItemLabel) {
            shareItemLabel.textContent = trigger.getAttribute("data-share-item-label") || "Файл";
        }

        const shareUrl = trigger.getAttribute("data-share-url");
        const absoluteUrl = shareUrl ? `${window.location.origin}${shareUrl}` : "";
        const withoutExpiry = trigger.getAttribute("data-share-unlimited") === "true";
        const allowPreview = trigger.getAttribute("data-share-preview") === "true";
        const allowDownload = trigger.getAttribute("data-share-download") === "true";

        if (shareExpiresUnlimitedInput) {
            shareExpiresUnlimitedInput.checked = withoutExpiry;
        }
        if (shareExpiresHoursInput) {
            shareExpiresHoursInput.value = withoutExpiry ? "" : (shareExpiresHoursInput.value || "24");
        }
        if (shareAllowPreviewInput) {
            shareAllowPreviewInput.checked = allowPreview;
        }
        if (shareAllowDownloadInput) {
            shareAllowDownloadInput.checked = allowDownload;
        }
        syncShareExpiryState();

        if (shareCurrent) {
            shareCurrent.hidden = !absoluteUrl;
        }
        if (shareUrlInput) {
            shareUrlInput.value = absoluteUrl;
        }
        if (shareOpenLink) {
            shareOpenLink.href = absoluteUrl || "#";
        }
        if (shareExpiresTitle) {
            shareExpiresTitle.textContent = trigger.getAttribute("data-share-expires") || "Без срока";
        }
        if (shareRightsTitle) {
            const parts = [];
            if (allowPreview) parts.push("Просмотр");
            if (allowDownload) parts.push("Скачивание");
            shareRightsTitle.textContent = parts.length === 0 ? "Без прав" : parts.join(" и ");
        }
    }

    function populateShareStateFromPreview(trigger) {
        if (!trigger || !shareModal) {
            return;
        }
        activeShareTrigger = null;
        if (shareResourceTypeInput) {
            shareResourceTypeInput.value = trigger.getAttribute("data-preview-share-resource-type") || "FILE";
        }
        if (shareResourceIdInput) {
            shareResourceIdInput.value = trigger.getAttribute("data-preview-share-resource-id") || "";
        }
        if (shareFileNameInput) {
            shareFileNameInput.value = trigger.getAttribute("data-preview-share-item-name") || "";
        }
        if (shareTitleInput) {
            shareTitleInput.value = trigger.getAttribute("data-preview-share-title") || "";
        }
        if (shareItemLabel) {
            shareItemLabel.textContent = trigger.getAttribute("data-preview-share-item-label") || "Файл";
        }

        const shareUrl = trigger.getAttribute("data-preview-share-url");
        const absoluteUrl = shareUrl ? `${window.location.origin}${shareUrl}` : "";
        const withoutExpiry = trigger.getAttribute("data-preview-share-unlimited") === "true";
        const allowPreview = trigger.getAttribute("data-preview-share-preview") === "true";
        const allowDownload = trigger.getAttribute("data-preview-share-download") === "true";

        if (shareExpiresUnlimitedInput) {
            shareExpiresUnlimitedInput.checked = withoutExpiry;
        }
        if (shareExpiresHoursInput) {
            shareExpiresHoursInput.value = withoutExpiry ? "" : (shareExpiresHoursInput.value || "24");
        }
        if (shareAllowPreviewInput) {
            shareAllowPreviewInput.checked = allowPreview;
        }
        if (shareAllowDownloadInput) {
            shareAllowDownloadInput.checked = allowDownload;
        }
        syncShareExpiryState();

        if (shareCurrent) {
            shareCurrent.hidden = !absoluteUrl;
        }
        if (shareUrlInput) {
            shareUrlInput.value = absoluteUrl;
        }
        if (shareOpenLink) {
            shareOpenLink.href = absoluteUrl || "#";
        }
        if (shareExpiresTitle) {
            shareExpiresTitle.textContent = trigger.getAttribute("data-preview-share-expires") || "Без срока";
        }
        if (shareRightsTitle) {
            const parts = [];
            if (allowPreview) parts.push("Просмотр");
            if (allowDownload) parts.push("Скачивание");
            shareRightsTitle.textContent = parts.length === 0 ? "Без прав" : parts.join(" и ");
        }
    }

    function populatePreviewState(trigger) {
        if (!trigger) {
            return;
        }
        const title = trigger.getAttribute("data-preview-title") || "Быстрый просмотр";
        const src = trigger.getAttribute("data-preview-src") || "";
        const type = trigger.getAttribute("data-preview-type") || "image";
        activePreviewRequestId += 1;

        if (previewModalTitle) {
            previewModalTitle.textContent = title;
        }
        if (previewContentHost) {
            previewContentHost.replaceChildren();
            if (type === "image") {
                const image = document.createElement("img");
                image.src = src;
                image.alt = title;
                previewContentHost.appendChild(image);
            } else if (type === "video") {
                const player = document.createElement("div");
                player.className = "share-video-player";
                player.setAttribute("data-video-player", "");
                player.innerHTML = `
                    <div class="share-video-stage" data-media-stage>
                        <video class="share-video-element" data-video-element playsinline preload="metadata">
                            <source src="${src}" type="${trigger.getAttribute("data-preview-mime-type") || ""}">
                        </video>
                    </div>
                    <div class="share-video-controls">
                        <div class="share-video-progress-wrap">
                            <input type="range" min="0" max="100" value="0" class="share-video-progress" data-video-progress>
                        </div>
                        <div class="share-video-controls-row">
                            <div class="share-video-controls-left">
                                <button type="button" class="share-video-button" data-video-toggle aria-label="Воспроизвести"></button>
                                <div class="share-video-time" data-video-time>00:00 / 00:00</div>
                            </div>
                            <div class="share-video-controls-right">
                                <div class="share-video-volume-wrap">
                                    <button type="button" class="share-video-volume-button" data-video-mute aria-label="Выключить звук"></button>
                                    <input type="range" min="0" max="100" value="50" class="share-video-volume" data-video-volume>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                previewContentHost.appendChild(player);
                initCustomMediaPlayer(player);
            } else if (type === "audio") {
                const player = document.createElement("div");
                player.className = "share-video-player share-audio-player";
                player.setAttribute("data-video-player", "");
                player.innerHTML = `
                    <div class="share-video-stage share-audio-stage" data-media-stage>
                        <audio data-video-element preload="metadata">
                            <source src="${src}" type="${trigger.getAttribute("data-preview-mime-type") || ""}">
                        </audio>
                        <div class="share-audio-note">
                            <div class="share-audio-icon" aria-hidden="true"></div>
                            <div class="share-audio-meta">
                                <div class="share-audio-title">${title}</div>
                                <div class="share-audio-subtitle">Аудиофайл</div>
                            </div>
                        </div>
                    </div>
                    <div class="share-video-controls">
                        <div class="share-video-progress-wrap">
                            <input type="range" min="0" max="100" value="0" class="share-video-progress" data-video-progress>
                        </div>
                        <div class="share-video-controls-row">
                            <div class="share-video-controls-left">
                                <button type="button" class="share-video-button" data-video-toggle aria-label="Воспроизвести"></button>
                                <div class="share-video-time" data-video-time>00:00 / 00:00</div>
                            </div>
                            <div class="share-video-controls-right">
                                <div class="share-video-volume-wrap">
                                    <button type="button" class="share-video-volume-button" data-video-mute aria-label="Выключить звук"></button>
                                    <input type="range" min="0" max="100" value="50" class="share-video-volume" data-video-volume>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                previewContentHost.appendChild(player);
                initCustomMediaPlayer(player);
            } else if (type === "text") {
                loadPreviewText(src, title);
            }
        }

        if (previewDownloadLink) {
            const downloadUrl = trigger.getAttribute("data-preview-download-url") || "#";
            previewDownloadLink.href = downloadUrl;
            previewDownloadLink.setAttribute("aria-disabled", downloadUrl === "#" ? "true" : "false");
        }

        if (previewShareLink) {
            previewShareLink.hidden = !(trigger.getAttribute("data-preview-share-resource-id"));
        }

        activePreviewItems = resolvePreviewItems(trigger);
        activePreviewIndex = activePreviewItems.findIndex((item) => getPreviewTriggerIdentity(item) === getPreviewTriggerIdentity(trigger));
        syncPreviewNavigation();
    }

    function resetPreviewState() {
        activePreviewRequestId += 1;
        if (previewContentHost) {
            previewContentHost.replaceChildren();
        }
        if (previewDownloadLink) {
            previewDownloadLink.href = "#";
        }
        activePreviewItems = [];
        activePreviewIndex = -1;
        syncPreviewNavigation();
    }

    function syncPreviewNavigation() {
        const total = activePreviewItems.length;
        const hasItems = total > 0 && activePreviewIndex >= 0;

        if (previewPosition) {
            previewPosition.textContent = hasItems ? `${activePreviewIndex + 1} / ${total}` : "0 / 0";
        }
        if (previewPrevButton) {
            previewPrevButton.disabled = !hasItems || activePreviewIndex <= 0;
        }
        if (previewNextButton) {
            previewNextButton.disabled = !hasItems || activePreviewIndex >= total - 1;
        }
    }

    function movePreview(step) {
        if (activePreviewIndex < 0) {
            return;
        }
        const nextIndex = activePreviewIndex + step;
        if (nextIndex < 0 || nextIndex >= activePreviewItems.length) {
            return;
        }
        populatePreviewState(activePreviewItems[nextIndex]);
    }

    function showShareResult() {
        if (!shareResult || !shareResultText) {
            return;
        }
        const text = bodyDataset.shareSuccess;
        if (!text) {
            shareResult.hidden = true;
            return;
        }
        shareResultText.textContent = text;
        shareResult.hidden = false;
    }

    function showCollaborativeSuccessToast() {
        const text = bodyDataset.collaborativeSuccess;
        if (!text) {
            return;
        }
        showToast(text, "Совместный доступ");
    }

    function showPendingUploadToast() {
        const message = window.sessionStorage.getItem("agtydrive_upload_toast_message");
        if (!message) {
            return;
        }
        window.sessionStorage.removeItem("agtydrive_upload_toast_message");
        showToast(message, "Загрузка");
    }

    function prepareShareDeleteState() {
        if (shareDeleteResourceTypeInput) {
            shareDeleteResourceTypeInput.value = shareResourceTypeInput ? shareResourceTypeInput.value : "";
        }
        if (shareDeleteResourceIdInput) {
            shareDeleteResourceIdInput.value = shareResourceIdInput ? shareResourceIdInput.value : "";
        }
        if (shareDeleteItemName) {
            const itemLabel = shareItemLabel ? shareItemLabel.textContent : "Файл";
            const itemName = shareFileNameInput ? shareFileNameInput.value : "";
            shareDeleteItemName.textContent = `${itemLabel}: ${itemName || "без названия"}`;
        }
    }

    modalTriggers.forEach((trigger) => {
        trigger.addEventListener("click", function (event) {
            if (this.hasAttribute("data-share-resource-id")) {
                event.preventDefault();
                event.stopPropagation();
                const modalName = this.getAttribute("data-open-modal");
                if (modalName === itemActionsModalName) {
                    resetItemActionsState();
                    populateItemActionsState(this);
                } else {
                    resetShareState();
                    populateShareState(this);
                    if (modalName === "share-delete-modal") {
                        prepareShareDeleteState();
                    }
                }
            }
            if (this.getAttribute("data-open-modal") === "item-delete-modal"
                && this.hasAttribute("data-item-resource-id")) {
                event.preventDefault();
                event.stopPropagation();
                resetItemActionsState();
                populateItemActionsState(this);
            }
            if (this.getAttribute("data-open-modal") === "collaborative-access-modal"
                && this.hasAttribute("data-collab-folder-id")) {
                event.preventDefault();
                event.stopPropagation();
                resetCollaborativeState();
                populateCollaborativeState(this);
            }
            if (this.getAttribute("data-open-modal") === webDavModalName
                && this.hasAttribute("data-webdav-folder-id")) {
                event.preventDefault();
                event.stopPropagation();
                resetWebDavState();
                populateWebDavState(this);
            }
            openModal(this.getAttribute("data-open-modal"));
            if (this.getAttribute("data-open-modal") === uploadModalName) {
                ensureUploadFolderSelected();
            }
        });
    });

    modalClosers.forEach((closer) => {
        closer.addEventListener("click", function () {
            if (this.getAttribute("data-close-modal") === "preview-modal") {
                resetPreviewState();
            }
            closeModal(this.getAttribute("data-close-modal"));
        });
    });

    modalBackdrops.forEach((backdrop) => {
        backdrop.addEventListener("click", function (event) {
            if (event.target === backdrop) {
                if (backdrop.getAttribute("data-modal") === "preview-modal") {
                    resetPreviewState();
                }
                closeModal(backdrop.getAttribute("data-modal"));
            }
        });
    });

    copyTriggers.forEach((trigger) => {
        trigger.addEventListener("click", async function () {
            let value = this.getAttribute("data-copy-text") || "";
            if (value.startsWith("/")) {
                value = `${window.location.origin}${value}`;
            }
            const copied = await copyText(value);
            showToast(copied ? "Ссылка скопирована." : "Не удалось скопировать ссылку.", copied ? "Буфер обмена" : "Ошибка");
        });
    });

    previewTriggers.forEach((trigger) => {
        trigger.addEventListener("click", function (event) {
            event.preventDefault();
            populatePreviewState(this);
            openModal("preview-modal");
        });
    });

    inlineConfirmTriggers.forEach((trigger) => {
        trigger.addEventListener("click", function (event) {
            const message = this.getAttribute("data-inline-confirm");
            if (!message) {
                return;
            }
            event.preventDefault();
            openWarningModal({
                title: "Подтверждение действия",
                message,
                confirmText: "Продолжить",
                onConfirm: () => {
                    const form = trigger.closest("form");
                    if (form) {
                        if (typeof form.requestSubmit === "function") {
                            form.requestSubmit(trigger);
                            return;
                        }
                        form.submit();
                        return;
                    }
                    const href = trigger.getAttribute("href");
                    if (href) {
                        window.location.href = href;
                    }
                }
            });
        });
    });

    folderRows.forEach((row) => {
        row.addEventListener("dblclick", function (event) {
            if (event.target.closest("button, a, input, label, form")) {
                return;
            }
            const href = row.getAttribute("data-href");
            if (!href) {
                return;
            }
            window.location.href = href;
        });
    });

    bulkSelectItems.forEach((item) => {
        item.addEventListener("click", function (event) {
            event.stopPropagation();
        });
    });

    if (bulkToggleAll) {
        bulkToggleAll.addEventListener("change", function () {
            bulkSelectItems.forEach((item) => {
                item.checked = bulkToggleAll.checked;
            });
        });
    }

    if (bulkDeleteTrigger) {
        bulkDeleteTrigger.addEventListener("click", function () {
            if (!applyBulkSelectionToForm(bulkDeleteForm)) {
                showToast("Выберите хотя бы один объект.", "Действие недоступно");
                return;
            }
            openWarningModal({
                title: "Удалить выбранные объекты?",
                message: "Выбранные файлы и папки будут удалены из диска. Для папок это действие затронет все вложенные объекты.",
                confirmText: "Удалить",
                target: "Выбранные объекты",
                onConfirm: () => {
                    bulkDeleteForm.submit();
                }
            });
        });
    }

    if (bulkMoveTrigger) {
        bulkMoveTrigger.addEventListener("click", function () {
            if (!applyBulkSelectionToForm(bulkMoveForm)) {
                showToast("Выберите хотя бы один объект.", "Действие недоступно");
                return;
            }
            openModal("bulk-move-modal");
        });
    }

    if (uploadInput) {
        uploadInput.addEventListener("change", function () {
            updateUploadFileName(this.files);
            setSelectedUploadFiles(this.files);
        });
    }

    if (uploadForm) {
        uploadForm.addEventListener("submit", function (event) {
            event.preventDefault();
            if (uploadBatchRunning) {
                return;
            }
            if (!selectedUploadItems.length && uploadInput && uploadInput.files && uploadInput.files.length > 0) {
                setSelectedUploadFiles(uploadInput.files);
            }
            submitSelectedUploadFlow();
        });
    }

    if (warningModalConfirm) {
        warningModalConfirm.addEventListener("click", function () {
            const action = pendingWarningAction;
            closeModal(warningModalName);
            if (typeof action === "function") {
                action();
            }
        });
    }

    if (warningModalSkip) {
        warningModalSkip.addEventListener("click", function () {
            const action = pendingWarningSkipAction;
            closeModal(warningModalName);
            if (typeof action === "function") {
                action();
            }
        });
    }

    if (shareExpiresUnlimitedInput) {
        shareExpiresUnlimitedInput.addEventListener("change", syncShareExpiryState);
        syncShareExpiryState();
    }

    if (itemPropertiesExpiresEnabled) {
        itemPropertiesExpiresEnabled.addEventListener("change", syncItemPropertiesExpiryState);
        syncItemPropertiesExpiryState();
    }

    expirationGroups.forEach((form) => {
        initExpirationGroup(form);
    });

    pickerInputs.forEach((input) => {
        input.addEventListener("click", function () {
            openNativePicker(input);
        });
    });

    if (itemPropertiesAction) {
        itemPropertiesAction.addEventListener("click", function () {
            if (!activeItemTrigger) {
                return;
            }
            populatePropertiesState(activeItemTrigger);
            closeModal(itemActionsModalName);
            openModal(itemPropertiesModalName);
        });
    }

    showCollaborativeSuccessToast();
    showPendingUploadToast();

    if (shareForm) {
        shareForm.addEventListener("submit", (event) => {
            const resourceId = shareResourceIdInput ? shareResourceIdInput.value : "";
            const resourceType = shareResourceTypeInput ? shareResourceTypeInput.value : "";
            if (!resourceId || !resourceType) {
                event.preventDefault();
                if (shareResult && shareResultText) {
                    shareResult.hidden = false;
                    shareResult.classList.remove("success");
                    shareResult.classList.add("error");
                    shareResultText.textContent = "Не выбран файл или папка для публичной ссылки.";
                }
            }
        });
    }

    if (shareCopyLink) {
        shareCopyLink.addEventListener("click", async function () {
            if (!shareUrlInput || !shareUrlInput.value) {
                showToast("Нет ссылки для копирования.", "Буфер обмена");
                return;
            }
            const copied = await copyText(shareUrlInput.value);
            showToast(copied ? "Ссылка скопирована." : "Не удалось скопировать ссылку.", copied ? "Буфер обмена" : "Ошибка");
        });
    }

    if (shareDeleteTrigger) {
        shareDeleteTrigger.addEventListener("click", function () {
            prepareShareDeleteState();
            closeModal("share-modal");
            openModal("share-delete-modal");
        });
    }

    if (itemShareAction) {
        itemShareAction.addEventListener("click", function () {
            if (!activeItemTrigger) {
                return;
            }
            resetShareState();
            populateShareState(activeItemTrigger);
            closeModal(itemActionsModalName);
            openModal("share-modal");
        });
    }

    if (itemCollaborativeAction) {
        itemCollaborativeAction.addEventListener("click", function () {
            if (!activeItemTrigger) {
                return;
            }
            resetCollaborativeState();
            populateCollaborativeState(activeItemTrigger);
            closeModal(itemActionsModalName);
            openModal("collaborative-access-modal");
        });
    }

    if (itemWebDavAction) {
        itemWebDavAction.addEventListener("click", function () {
            if (!activeItemTrigger) {
                return;
            }
            resetWebDavState();
            populateWebDavState(activeItemTrigger);
            closeModal(itemActionsModalName);
            openModal(webDavModalName);
        });
    }

    if (itemRenameAction) {
        itemRenameAction.addEventListener("click", function () {
            if (!activeItemTrigger) {
                return;
            }
            populateRenameState(activeItemTrigger, false);
            closeModal(itemActionsModalName);
            openModal(itemRenameModalName);
        });
    }

    if (itemMoveAction) {
        itemMoveAction.addEventListener("click", function () {
            if (!activeItemTrigger) {
                return;
            }
            populateMoveState(activeItemTrigger, false);
            closeModal(itemActionsModalName);
            openModal(itemMoveModalName);
        });
    }

    if (itemDeleteAction) {
        itemDeleteAction.addEventListener("click", function () {
            if (!activeItemTrigger) {
                return;
            }
            closeModal(itemActionsModalName);
            openModal("item-delete-modal");
        });
    }

    if (previewPrevButton) {
        previewPrevButton.addEventListener("click", function () {
            movePreview(-1);
        });
    }

    if (previewNextButton) {
        previewNextButton.addEventListener("click", function () {
            movePreview(1);
        });
    }

    if (previewShareLink) {
        previewShareLink.addEventListener("click", function () {
            if (activePreviewIndex < 0 || !activePreviewItems[activePreviewIndex]) {
                return;
            }
            resetShareState();
            populateShareStateFromPreview(activePreviewItems[activePreviewIndex]);
            openModal("share-modal");
        });
    }

    if (webDavCopyUrl) {
        webDavCopyUrl.addEventListener("click", async function () {
            if (!webDavUrlInput || !webDavUrlInput.value) {
                showToast("Нет адреса для копирования.", "Буфер обмена");
                return;
            }
            const copied = await copyText(webDavUrlInput.value);
            showToast(copied ? "Адрес WebDAV скопирован." : "Не удалось скопировать адрес.", copied ? "Буфер обмена" : "Ошибка");
        });
    }

    if (webDavCopyLogin) {
        webDavCopyLogin.addEventListener("click", async function () {
            if (!webDavLoginInput || !webDavLoginInput.value) {
                showToast("Нет логина для копирования.", "Буфер обмена");
                return;
            }
            const copied = await copyText(webDavLoginInput.value);
            showToast(copied ? "Логин WebDAV скопирован." : "Не удалось скопировать логин.", copied ? "Буфер обмена" : "Ошибка");
        });
    }

    if (webDavCopyDavUrl) {
        webDavCopyDavUrl.addEventListener("click", async function () {
            if (!webDavDavUrlInput || !webDavDavUrlInput.value) {
                showToast("Нет dav:// адреса для копирования.", "Буфер обмена");
                return;
            }
            const copied = await copyText(webDavDavUrlInput.value);
            showToast(copied ? "Адрес dav:// скопирован." : "Не удалось скопировать адрес.", copied ? "Буфер обмена" : "Ошибка");
        });
    }

    if (webDavCopyWebDavUrl) {
        webDavCopyWebDavUrl.addEventListener("click", async function () {
            if (!webDavWebDavUrlInput || !webDavWebDavUrlInput.value) {
                showToast("Нет webdav:// адреса для копирования.", "Буфер обмена");
                return;
            }
            const copied = await copyText(webDavWebDavUrlInput.value);
            showToast(copied ? "Адрес webdav:// скопирован." : "Не удалось скопировать адрес.", copied ? "Буфер обмена" : "Ошибка");
        });
    }

    if (webDavDeleteButton) {
        webDavDeleteButton.addEventListener("click", function () {
            if (!webDavForm || !webDavFolderNameInput || !webDavDeleteButton.hasAttribute("data-delete-action")) {
                return;
            }
            const deleteAction = webDavDeleteButton.getAttribute("data-delete-action");
            const folderName = webDavFolderNameInput.value || "Папка";
            openWarningModal({
                title: "Удалить доступ WebDAV?",
                message: "WebDAV-адрес и доступ по этим учетным данным будут отключены сразу.",
                confirmText: "Удалить доступ",
                target: folderName,
                onConfirm: () => {
                    webDavForm.action = deleteAction;
                    webDavForm.submit();
                }
            });
        });
    }

    const openShareResourceId = bodyDataset.shareOpenResourceId;
    const openShareResourceType = bodyDataset.shareOpenResourceType;
    if (openShareResourceId && openShareResourceType) {
        const trigger = document.querySelector(`[data-share-resource-type="${openShareResourceType}"][data-share-resource-id="${openShareResourceId}"]`);
        if (trigger) {
            resetShareState();
            populateShareState(trigger);
            showShareResult();
            openModal("share-modal");
        }
    }

    const itemOpenModal = bodyDataset.itemOpenModal;
    const itemOpenResourceId = bodyDataset.itemOpenResourceId;
    const itemOpenResourceType = bodyDataset.itemOpenResourceType;
    if (itemOpenModal && itemOpenResourceId && itemOpenResourceType) {
        const trigger = document.querySelector(`[data-item-resource-type="${itemOpenResourceType}"][data-item-resource-id="${itemOpenResourceId}"]`);
        if (trigger) {
            resetItemActionsState();
            populateItemActionsState(trigger);
            if (itemOpenModal === itemRenameModalName) {
                populateRenameState(trigger, true);
            }
            if (itemOpenModal === itemMoveModalName) {
                populateMoveState(trigger, true);
            }
            if (itemOpenModal === itemPropertiesModalName) {
                populatePropertiesState(trigger);
            }
            if (itemOpenModal === webDavModalName) {
                resetWebDavState();
                populateWebDavState(trigger);
            }
            openModal(itemOpenModal);
        }
    }

    if (pasteForm) {
        pasteForm.addEventListener("submit", function (event) {
            event.preventDefault();
            submitClipboardPayload();
        });
    }

    window.addEventListener("keydown", function (event) {
        if (!previewModal || previewModal.hidden) {
            return;
        }
        if (event.key === "ArrowLeft") {
            event.preventDefault();
            movePreview(-1);
        }
        if (event.key === "ArrowRight") {
            event.preventDefault();
            movePreview(1);
        }
        if (event.key === "Escape") {
            resetPreviewState();
            closeModal("preview-modal");
        }
    });

    window.addEventListener("paste", function (event) {
        if (!uploadForm || !uploadInput || uploadInput.disabled || isEditableTarget(event.target)) {
            return;
        }
        const clipboardData = event.clipboardData;
        if (!clipboardData) {
            return;
        }

        const pastedFiles = Array.from(clipboardData.files || []).filter(Boolean);
        if (pastedFiles.length > 0) {
            const singleImageFile = pastedFiles.length === 1
                    && pastedFiles[0].type.startsWith("image/")
                    && (!pastedFiles[0].name || /^image\.[a-z0-9]+$/i.test(pastedFiles[0].name));
            event.preventDefault();
            if (singleImageFile) {
                openPasteModal({
                    kind: "image",
                    blob: pastedFiles[0],
                    mimeType: pastedFiles[0].type || "image/png",
                    size: pastedFiles[0].size || 0,
                    suggestedName: suggestClipboardFilename("image", pastedFiles[0].type || "image/png")
                });
                return;
            }
            applyDroppedFiles(pastedFiles);
            return;
        }

        const items = Array.from(clipboardData.items || []);
        const imageItem = items.find((item) => item.kind === "file" && item.type.startsWith("image/"));
        if (imageItem) {
            const imageFile = imageItem.getAsFile();
            if (imageFile) {
                event.preventDefault();
                openPasteModal({
                    kind: "image",
                    blob: imageFile,
                    mimeType: imageFile.type || "image/png",
                    size: imageFile.size || 0,
                    suggestedName: suggestClipboardFilename("image", imageFile.type || "image/png")
                });
                return;
            }
        }

        const textItem = items.find((item) => item.kind === "string" && item.type === "text/plain");
        if (textItem) {
            event.preventDefault();
            textItem.getAsString((value) => {
                if (!value || !value.trim()) {
                    return;
                }
                const blob = new Blob([value], {type: "text/plain;charset=utf-8"});
                openPasteModal({
                    kind: "text",
                    blob,
                    mimeType: "text/plain",
                    size: blob.size,
                    previewText: value.length > 4000 ? `${value.slice(0, 4000)}\n…` : value,
                    suggestedName: suggestClipboardFilename("text", "text/plain")
                });
            });
        }
    });

    function applyDroppedFiles(files) {
        if (!uploadInput || !files || files.length === 0) {
            return;
        }
        if (!assignFilesToUploadInput(files)) {
            return;
        }
        setSelectedUploadFiles(files);
        ensureUploadFolderSelected();
        openModal(uploadModalName);
    }

    dropTargets.forEach((target) => {
        ["dragenter", "dragover"].forEach((eventName) => {
            target.addEventListener(eventName, function (event) {
                event.preventDefault();
                target.classList.add("is-dragover");
            });
        });

        ["dragleave", "drop"].forEach((eventName) => {
            target.addEventListener(eventName, function (event) {
                event.preventDefault();
                if (eventName === "drop") {
                    applyDroppedFiles(event.dataTransfer.files);
                }
                target.classList.remove("is-dragover");
            });
        });
    });

    ["dragenter", "dragover"].forEach((eventName) => {
        window.addEventListener(eventName, function (event) {
            event.preventDefault();
            dragDepth += 1;
            showGlobalDragOverlay();
        });
    });

    ["dragleave"].forEach((eventName) => {
        window.addEventListener(eventName, function (event) {
            event.preventDefault();
            dragDepth = Math.max(0, dragDepth - 1);
            if (dragDepth === 0) {
                hideGlobalDragOverlay();
            }
        });
    });

    window.addEventListener("drop", function (event) {
        event.preventDefault();
        resetGlobalDragState();
        applyDroppedFiles(event.dataTransfer.files);
    });

    window.addEventListener("dragend", function () {
        resetGlobalDragState();
    });

    window.addEventListener("blur", function () {
        resetGlobalDragState();
    });

    document.addEventListener("dragleave", function (event) {
        if (event.clientX === 0 && event.clientY === 0) {
            resetGlobalDragState();
        }
    });

    document.addEventListener("dragexit", function () {
        resetGlobalDragState();
    });

    document.addEventListener("mouseout", function (event) {
        if (shouldResetDragStateForPointerExit(event)) {
            resetGlobalDragState();
        }
    });

    document.addEventListener("mouseleave", function (event) {
        if (shouldResetDragStateForPointerExit(event)) {
            resetGlobalDragState();
        }
    }, true);

    document.documentElement.addEventListener("mouseleave", function () {
        if (body.classList.contains("drag-active")) {
            resetGlobalDragState();
        }
    });

    if (dragOverlay) {
        ["dragleave", "mouseleave", "mouseout"].forEach((eventName) => {
            dragOverlay.addEventListener(eventName, function (event) {
                if (shouldResetDragStateForPointerExit(event)) {
                    resetGlobalDragState();
                }
            });
        });
    }

    async function copyText(value) {
        if (!value) {
            return false;
        }
        if (navigator.clipboard && window.isSecureContext) {
            try {
                await navigator.clipboard.writeText(value);
                return true;
            } catch (_) {
                return fallbackCopyText(value);
            }
        }
        return fallbackCopyText(value);
    }

    function fallbackCopyText(value) {
        const textarea = document.createElement("textarea");
        textarea.value = value;
        textarea.setAttribute("readonly", "");
        textarea.style.position = "fixed";
        textarea.style.opacity = "0";
        textarea.style.pointerEvents = "none";
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        try {
            return document.execCommand("copy");
        } catch (_) {
            return false;
        } finally {
            document.body.removeChild(textarea);
        }
    }
})();
