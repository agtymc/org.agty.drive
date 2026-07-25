(function () {
    const body = document.body;
    const bodyDataset = body ? body.dataset : {};
    const modalBackdrops = document.querySelectorAll("[data-modal]");
    const modalTriggers = document.querySelectorAll("[data-open-modal]");
    const modalClosers = document.querySelectorAll("[data-close-modal]");
    const uploadForm = document.querySelector("[data-upload-form]");
    const uploadInput = uploadForm ? uploadForm.querySelector(".upload-file-input") : null;
    const uploadFolderSelect = uploadForm ? uploadForm.querySelector('select[name="folderId"]') : null;
    const uploadFileName = document.querySelector("[data-upload-file-name]");
    const uploadModalName = "upload-modal";
    const itemActionsModalName = "item-actions-modal";
    const itemActionsModal = document.querySelector(`[data-modal="${itemActionsModalName}"]`);
    const itemActionsName = itemActionsModal ? itemActionsModal.querySelector("[data-item-actions-name]") : null;
    const itemActionsIcon = itemActionsModal ? itemActionsModal.querySelector("[data-item-actions-icon]") : null;
    const itemShareAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-share-action]") : null;
    const itemRenameAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-rename-action]") : null;
    const itemMoveAction = itemActionsModal ? itemActionsModal.querySelector("[data-item-move-action]") : null;
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
    const shareItemLabel = shareForm ? shareForm.querySelector("[data-share-item-label-view]") : null;
    const shareExpiresHoursInput = shareForm ? shareForm.querySelector("[data-share-expires-hours]") : null;
    const shareExpiresUnlimitedInput = shareForm ? shareForm.querySelector("[data-share-expires-unlimited]") : null;
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
    const bulkDeleteForm = document.querySelector("[data-bulk-delete-form]");
    const bulkMoveForm = document.querySelector("[data-bulk-move-form]");
    const bulkDeleteTrigger = document.querySelector("[data-bulk-delete-trigger]");
    const bulkMoveTrigger = document.querySelector("[data-bulk-move-trigger]");
    const bulkToggleAll = document.querySelector("[data-bulk-toggle-all]");
    const bulkSelectItems = document.querySelectorAll("[data-bulk-select-item]");
    const dropTargets = document.querySelectorAll("[data-upload-dropzone], [data-upload-dropzone-inner]");
    const dragOverlay = document.querySelector("[data-drag-overlay]");
    const folderRows = document.querySelectorAll("[data-folder-row]");
    let activeItemTrigger = null;
    let activeShareTrigger = null;
    let dragDepth = 0;

    function openModal(name) {
        const modal = document.querySelector(`[data-modal="${name}"]`);
        if (!modal) {
            return;
        }
        modal.hidden = false;
        body.classList.add("modal-open");
    }

    function closeModal(name) {
        const modal = document.querySelector(`[data-modal="${name}"]`);
        if (!modal) {
            return;
        }
        modal.hidden = true;
        if (![...modalBackdrops].some((item) => !item.hidden)) {
            body.classList.remove("modal-open");
        }
    }

    function updateUploadFileName(files) {
        if (!uploadFileName) {
            return;
        }
        if (!files || files.length === 0) {
            uploadFileName.textContent = "Файл еще не выбран";
            return;
        }
        uploadFileName.textContent = files[0].name;
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
        if (!uploadFolderSelect) {
            return;
        }
        if (uploadFolderSelect.value) {
            return;
        }
        const firstAvailableOption = [...uploadFolderSelect.options].find((option) => option.value);
        if (firstAvailableOption) {
            uploadFolderSelect.value = firstAvailableOption.value;
        }
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
        if (shareItemLabel) {
            shareItemLabel.textContent = trigger.getAttribute("data-share-item-label") || "Файл";
        }

        const shareUrl = trigger.getAttribute("data-share-url");
        const absoluteUrl = shareUrl ? `${window.location.origin}${shareUrl}` : "";
        const allowPreview = trigger.getAttribute("data-share-preview") === "true";
        const allowDownload = trigger.getAttribute("data-share-download") === "true";

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
                if (this.getAttribute("data-open-modal") === itemActionsModalName) {
                    resetItemActionsState();
                    populateItemActionsState(this);
                } else {
                    resetShareState();
                    populateShareState(this);
                }
            }
            openModal(this.getAttribute("data-open-modal"));
            if (this.getAttribute("data-open-modal") === uploadModalName) {
                ensureUploadFolderSelected();
            }
        });
    });

    modalClosers.forEach((closer) => {
        closer.addEventListener("click", function () {
            closeModal(this.getAttribute("data-close-modal"));
        });
    });

    modalBackdrops.forEach((backdrop) => {
        backdrop.addEventListener("click", function (event) {
            if (event.target === backdrop) {
                closeModal(backdrop.getAttribute("data-modal"));
            }
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
                window.alert("Выберите хотя бы один объект.");
                return;
            }
            if (!window.confirm("Удалить выбранные объекты?")) {
                return;
            }
            bulkDeleteForm.submit();
        });
    }

    if (bulkMoveTrigger) {
        bulkMoveTrigger.addEventListener("click", function () {
            if (!applyBulkSelectionToForm(bulkMoveForm)) {
                window.alert("Выберите хотя бы один объект.");
                return;
            }
            openModal("bulk-move-modal");
        });
    }

    if (uploadInput) {
        uploadInput.addEventListener("change", function () {
            updateUploadFileName(this.files);
        });
    }

    if (shareExpiresUnlimitedInput) {
        shareExpiresUnlimitedInput.addEventListener("change", syncShareExpiryState);
        syncShareExpiryState();
    }

    if (shareForm) {
        shareForm.addEventListener("submit", (event) => {
            if (activeShareTrigger) {
                populateShareState(activeShareTrigger);
            }

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
        shareCopyLink.addEventListener("click", function () {
            if (!shareUrlInput || !shareUrlInput.value) {
                return;
            }
            copyText(shareUrlInput.value);
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
            openModal(itemOpenModal);
        }
    }

    function applyDroppedFiles(files) {
        if (!uploadInput || !files || files.length === 0) {
            return;
        }
        const transfer = new DataTransfer();
        Array.from(files).forEach((file) => transfer.items.add(file));
        uploadInput.files = transfer.files;
        updateUploadFileName(transfer.files);
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
        dragDepth = 0;
        hideGlobalDragOverlay();
        applyDroppedFiles(event.dataTransfer.files);
    });

    function copyText(value) {
        if (!value) {
            return;
        }
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(value).catch(() => fallbackCopyText(value));
            return;
        }
        fallbackCopyText(value);
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
            document.execCommand("copy");
        } catch (_) {
        }
        document.body.removeChild(textarea);
    }
})();
