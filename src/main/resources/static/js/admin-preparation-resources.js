(function () {
    const RESOURCES_API = "/api/admin/preparation-resources";
    const DRIVES_API = "/api/admin/placement-drives";
    const DEFAULT_EMPTY_MESSAGE = "No preparation resources added yet. Upload the first drive-wise preparation pack to get started.";
    const FILE_MAPPINGS = [
        { inputId: "aptitudePdf", nameId: "aptitudePdfName", linkId: "aptitudeExistingLink", label: "Aptitude Material", urlKey: "aptitudePdfUrl", type: "aptitude" },
        { inputId: "codingPdf", nameId: "codingPdfName", linkId: "codingExistingLink", label: "Coding Material", urlKey: "codingPdfUrl", type: "coding" },
        { inputId: "technicalPdf", nameId: "technicalPdfName", linkId: "technicalExistingLink", label: "Technical Topics", urlKey: "technicalPdfUrl", type: "technical" },
        { inputId: "hrPdf", nameId: "hrPdfName", linkId: "hrExistingLink", label: "HR Preparation", urlKey: "hrPdfUrl", type: "hr" }
    ];
    let drives = [];
    let editingResource = null;

    async function fetchJson(url, options) {
        const response = await fetch(url, options);

        if (response.status === 204) {
            return null;
        }

        const rawText = await response.text();
        let payload = null;

        if (rawText) {
            try {
                payload = JSON.parse(rawText);
            } catch (error) {
                payload = null;
            }
        }

        if (!response.ok) {
            const message = payload && payload.message
                ? payload.message
                : (rawText || "Request failed.");
            throw new Error(message);
        }

        return payload;
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function formatDate(dateString) {
        if (!dateString) {
            return "";
        }
        return new Date(dateString).toLocaleDateString("en-IN", {
            year: "numeric",
            month: "short",
            day: "numeric"
        });
    }

    function setFeedback(message, isError) {
        const successElement = document.getElementById("resourceSuccess");
        const errorElement = document.getElementById("resourceError");

        if (isError) {
            successElement.textContent = "";
            successElement.classList.add("hidden");
            errorElement.textContent = message;
            return;
        }

        errorElement.textContent = "";
        successElement.textContent = message;
        successElement.classList.remove("hidden");
    }

    function clearFeedback() {
        document.getElementById("resourceSuccess").textContent = "";
        document.getElementById("resourceSuccess").classList.add("hidden");
        document.getElementById("resourceError").textContent = "";
    }

    function setLoading(isLoading) {
        const submitButton = document.getElementById("submitResourceButton");
        submitButton.disabled = isLoading;
        submitButton.textContent = isLoading
            ? "Uploading..."
            : (document.getElementById("editingResourceId").value ? "Update Resource" : "Add Resource");
    }

    function renderDriveOptions() {
        const select = document.getElementById("resourcePlacementDriveId");
        select.innerHTML = '<option value="">Select placement drive</option>';

        drives.forEach(function (drive) {
            const option = document.createElement("option");
            option.value = String(drive.id);
            option.textContent = drive.companyName + " - " + drive.driveTitle + " (" + drive.hiringYear + ")";
            select.appendChild(option);
        });
    }

    function renderDrivePreview(placementDriveId) {
        const preview = document.getElementById("resourceDrivePreview");
        const drive = drives.find(function (item) {
            return String(item.id) === String(placementDriveId);
        });

        if (!drive) {
            preview.classList.add("hidden");
            preview.innerHTML = "";
            return;
        }

        const logoMarkup = drive.companyLogoUrl
            ? '<img class="resource-drive-logo" src="' + escapeHtml(drive.companyLogoUrl) + '" alt="' + escapeHtml(drive.companyName) + ' logo">'
            : '<div class="resource-drive-logo"></div>';

        preview.innerHTML = [
            logoMarkup,
            "<div>",
            "<strong>" + escapeHtml(drive.companyName) + "</strong>",
            "<p>" + escapeHtml(drive.driveTitle) + " | " + escapeHtml(drive.hiringYear) + "</p>",
            "</div>"
        ].join("");
        preview.classList.remove("hidden");
    }

    function updateFileName(inputId, nameId) {
        const input = document.getElementById(inputId);
        const nameElement = document.getElementById(nameId);
        nameElement.textContent = input.files && input.files[0] ? input.files[0].name : "No file selected";
    }

    function buildPdfActionUrl(resourceId, type, action) {
        return "/api/admin/preparation-resources/" + encodeURIComponent(resourceId) + "/pdf/" + encodeURIComponent(type) + "/" + action;
    }

    function setExistingFileLink(linkId, label, resourceId, type, url) {
        const linkContainer = document.getElementById(linkId);
        if (!url) {
            linkContainer.innerHTML = "";
            linkContainer.classList.add("hidden");
            return;
        }

        linkContainer.innerHTML = '<a href="' + escapeHtml(buildPdfActionUrl(resourceId, type, "view")) + '" target="_blank" rel="noopener noreferrer">Open existing ' + escapeHtml(label) + '</a>';
        linkContainer.classList.remove("hidden");
    }

    function resetFileStates() {
        FILE_MAPPINGS.forEach(function (mapping) {
            document.getElementById(mapping.inputId).value = "";
            document.getElementById(mapping.nameId).textContent = "No file selected";
            document.getElementById(mapping.linkId).innerHTML = "";
            document.getElementById(mapping.linkId).classList.add("hidden");
        });
    }

    async function loadDrives() {
        drives = await fetchJson(DRIVES_API, { method: "GET" });
        renderDriveOptions();
    }

    function resetForm() {
        document.getElementById("preparationResourceForm").reset();
        document.getElementById("editingResourceId").value = "";
        document.getElementById("submitResourceButton").textContent = "Add Resource";
        document.getElementById("submitResourceButton").disabled = false;
        document.getElementById("cancelResourceEditButton").classList.add("hidden");
        document.getElementById("resourceUploadHint").textContent = "Upload at least one PDF while creating a new resource pack. Each file can be up to 10MB.";
        renderDrivePreview("");
        resetFileStates();
        editingResource = null;
        clearFeedback();
    }

    function populateForm(resource) {
        editingResource = resource;
        document.getElementById("editingResourceId").value = String(resource.id);
        document.getElementById("resourcePlacementDriveId").value = String(resource.placementDriveId);
        document.getElementById("resourceTitle").value = resource.resourceTitle || "";
        document.getElementById("resourceDescription").value = resource.description || "";
        document.getElementById("submitResourceButton").textContent = "Update Resource";
        document.getElementById("cancelResourceEditButton").classList.remove("hidden");
        document.getElementById("resourceUploadHint").textContent = "Upload a new PDF in any category only if you want to replace the existing file for that category.";
        renderDrivePreview(resource.placementDriveId);
        resetFileStates();
        FILE_MAPPINGS.forEach(function (mapping) {
            setExistingFileLink(mapping.linkId, mapping.label, resource.id, mapping.type, resource[mapping.urlKey]);
        });
        clearFeedback();
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function buildFormData(form) {
        const formData = new FormData();
        formData.append("placementDriveId", form.elements.placementDriveId.value);
        formData.append("resourceTitle", form.elements.resourceTitle.value.trim());
        formData.append("description", form.elements.description.value.trim());

        FILE_MAPPINGS.forEach(function (mapping) {
            const input = document.getElementById(mapping.inputId);
            if (input.files && input.files[0]) {
                formData.append(mapping.inputId, input.files[0]);
            }
        });

        return formData;
    }

    function hasSelectedFile() {
        return FILE_MAPPINGS.some(function (mapping) {
            const input = document.getElementById(mapping.inputId);
            return input.files && input.files.length > 0;
        });
    }

    function validateForm(form) {
        if (!form.elements.placementDriveId.value || !form.elements.resourceTitle.value.trim()) {
            return "Placement Drive and Resource Title are required.";
        }
        if (!document.getElementById("editingResourceId").value && !hasSelectedFile()) {
            return "Upload at least one PDF file while creating a preparation resource.";
        }
        return "";
    }

    function getLogoMarkup(resource) {
        if (resource.companyLogoUrl) {
            return '<img class="resource-company-logo" src="' + escapeHtml(resource.companyLogoUrl) + '" alt="' + escapeHtml(resource.companyName) + ' logo">';
        }
        return '<div class="resource-company-logo"></div>';
    }

    function buildPdfButtons(resource) {
        return FILE_MAPPINGS.filter(function (mapping) {
            return Boolean(resource[mapping.urlKey]);
        }).map(function (mapping) {
            return '<a class="pdf-action-btn" href="' + escapeHtml(buildPdfActionUrl(resource.id, mapping.type, "view")) + '" target="_blank" rel="noopener noreferrer">' + escapeHtml(mapping.label) + '</a>';
        }).join("");
    }

    async function loadResources() {
        const loadingElement = document.getElementById("adminResourceLoading");
        const list = document.getElementById("adminResourceList");
        const emptyState = document.getElementById("adminResourceEmpty");

        loadingElement.classList.remove("hidden");
        list.innerHTML = "";
        emptyState.classList.add("hidden");
        emptyState.textContent = DEFAULT_EMPTY_MESSAGE;

        try {
            const resources = await fetchJson(RESOURCES_API, { method: "GET" });
            loadingElement.classList.add("hidden");

            if (!resources.length) {
                emptyState.classList.remove("hidden");
                return;
            }

            resources.forEach(function (resource) {
                const card = document.createElement("article");
                card.className = "admin-resource-item";
                card.innerHTML = [
                    '<div class="resource-header">',
                    '<div class="resource-title-group">',
                    getLogoMarkup(resource),
                    "<div>",
                    "<h3>" + escapeHtml(resource.resourceTitle) + "</h3>",
                    "<p>" + escapeHtml(resource.companyName) + " | " + escapeHtml(resource.driveTitle) + " | " + escapeHtml(resource.hiringYear) + "</p>",
                    "</div>",
                    "</div>",
                    '<span class="status-badge ' + (resource.active ? "status-active" : "status-disabled") + '">' + (resource.active ? "Active" : "Disabled") + "</span>",
                    "</div>",
                    '<div class="resource-meta">',
                    '<span class="resource-tag">Drive Resource Pack</span>',
                    "<span>Created: " + formatDate(resource.createdAt) + "</span>",
                    "</div>",
                    (resource.description ? '<p class="resource-description">' + escapeHtml(resource.description).replace(/\n/g, "<br>") + "</p>" : ""),
                    '<div class="resource-pdf-links">' + buildPdfButtons(resource) + "</div>",
                    '<div class="resource-actions">',
                    '<button class="mini-btn toggle-resource-btn" type="button" data-resource-id="' + resource.id + '" data-active="' + resource.active + '">'
                        + (resource.active ? "Disable" : "Enable") + "</button>",
                    '<button class="mini-btn edit-resource-btn" type="button" data-resource-json="' + encodeURIComponent(JSON.stringify(resource)) + '">Edit</button>',
                    '<button class="mini-btn delete-btn" type="button" data-resource-id="' + resource.id + '">Delete</button>',
                    "</div>"
                ].join("");
                list.appendChild(card);
            });

            bindResourceActions();
        } catch (error) {
            loadingElement.classList.add("hidden");
            emptyState.textContent = error.message || "Unable to load preparation resources right now.";
            emptyState.classList.remove("hidden");
        }
    }

    function bindResourceActions() {
        document.querySelectorAll(".edit-resource-btn").forEach(function (button) {
            button.addEventListener("click", function () {
                populateForm(JSON.parse(decodeURIComponent(button.dataset.resourceJson)));
            });
        });

        document.querySelectorAll(".delete-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                if (!window.confirm("Delete this preparation resource pack?")) {
                    return;
                }

                try {
                    clearFeedback();
                    await fetchJson(RESOURCES_API + "/" + button.dataset.resourceId, { method: "DELETE" });
                    if (document.getElementById("editingResourceId").value === button.dataset.resourceId) {
                        resetForm();
                    }
                    setFeedback("Preparation resource deleted successfully.", false);
                    await loadResources();
                } catch (error) {
                    setFeedback(error.message || "Unable to delete preparation resource.", true);
                }
            });
        });

        document.querySelectorAll(".toggle-resource-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                const currentState = button.dataset.active === "true";

                try {
                    clearFeedback();
                    await fetchJson(RESOURCES_API + "/" + button.dataset.resourceId + "/status?active=" + (!currentState), {
                        method: "PATCH"
                    });
                    setFeedback("Preparation resource status updated successfully.", false);
                    await loadResources();
                } catch (error) {
                    setFeedback(error.message || "Unable to update preparation resource status.", true);
                }
            });
        });
    }

    function setupFileInputs() {
        FILE_MAPPINGS.forEach(function (mapping) {
            document.getElementById(mapping.inputId).addEventListener("change", function () {
                updateFileName(mapping.inputId, mapping.nameId);
            });
        });
    }

    function setupForm() {
        const form = document.getElementById("preparationResourceForm");
        if (!form) {
            return;
        }

        document.getElementById("resourcePlacementDriveId").addEventListener("change", function () {
            renderDrivePreview(this.value);
        });

        document.getElementById("cancelResourceEditButton").addEventListener("click", resetForm);

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const validationMessage = validateForm(form);
            if (validationMessage) {
                setFeedback(validationMessage, true);
                return;
            }

            const editingResourceId = document.getElementById("editingResourceId").value;
            const formData = buildFormData(form);

            try {
                clearFeedback();
                setLoading(true);
                await fetchJson(editingResourceId ? RESOURCES_API + "/" + editingResourceId : RESOURCES_API, {
                    method: editingResourceId ? "PUT" : "POST",
                    body: formData
                });
                resetForm();
                setFeedback(editingResourceId ? "Preparation resource updated successfully." : "Preparation resource uploaded successfully.", false);
                await loadResources();
            } catch (error) {
                setFeedback(error.message || "Unable to save preparation resource.", true);
            } finally {
                setLoading(false);
            }
        });
    }

    document.addEventListener("DOMContentLoaded", async function () {
        if (!document.getElementById("preparationResourceForm")) {
            return;
        }

        try {
            await loadDrives();
            setupFileInputs();
            setupForm();
            await loadResources();
        } catch (error) {
            setFeedback(error.message || "Unable to initialize preparation resource management.", true);
        }
    });
})();
