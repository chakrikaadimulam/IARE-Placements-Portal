(function () {
    const STATISTICS_API = "/api/admin/placement-statistics";
    const DRIVES_API = "/api/admin/placement-drives";
    const DEFAULT_EMPTY_MESSAGE = "No placement statistics added yet. Create your first statistics record to get started.";
    let drives = [];

    async function fetchJson(url, options) {
        const response = await fetch(url, {
            headers: {
                "Content-Type": "application/json"
            },
            ...options
        });

        if (response.status === 204) {
            return null;
        }

        const payload = await response.json().catch(function () {
            return null;
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Request failed.");
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
        const successElement = document.getElementById("statisticsSuccess");
        const errorElement = document.getElementById("statisticsError");

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
        const successElement = document.getElementById("statisticsSuccess");
        const errorElement = document.getElementById("statisticsError");

        successElement.textContent = "";
        successElement.classList.add("hidden");
        errorElement.textContent = "";
    }

    function setLoading(isLoading) {
        const submitButton = document.getElementById("submitStatisticsButton");
        submitButton.disabled = isLoading;
        submitButton.textContent = isLoading
            ? "Saving..."
            : (document.getElementById("editingStatisticsId").value ? "Update Statistics" : "Add Statistics");
    }

    function renderDriveOptions() {
        const select = document.getElementById("placementDriveId");
        select.innerHTML = '<option value="">Select placement drive</option>';

        drives.forEach(function (drive) {
            const option = document.createElement("option");
            option.value = String(drive.id);
            option.textContent = drive.companyName + " - " + drive.driveTitle + " (" + drive.hiringYear + ")";
            select.appendChild(option);
        });
    }

    function renderDrivePreview(placementDriveId) {
        const preview = document.getElementById("drivePreview");
        if (!placementDriveId) {
            preview.classList.add("hidden");
            preview.innerHTML = "";
            return;
        }

        const drive = drives.find(function (item) {
            return String(item.id) === String(placementDriveId);
        });

        if (!drive) {
            preview.classList.add("hidden");
            preview.innerHTML = "";
            return;
        }

        const logoMarkup = drive.companyLogoUrl
            ? '<img class="drive-preview-logo" src="' + escapeHtml(drive.companyLogoUrl) + '" alt="' + escapeHtml(drive.companyName) + ' logo">'
            : '<div class="drive-preview-logo"></div>';

        preview.innerHTML = [
            logoMarkup,
            "<div>",
            "<strong>" + escapeHtml(drive.companyName) + "</strong>",
            "<p>" + escapeHtml(drive.driveTitle) + " | " + escapeHtml(drive.hiringYear) + " | " + formatDate(drive.hiringDate) + " | " + escapeHtml(drive.driveStatus) + "</p>",
            "</div>"
        ].join("");
        preview.classList.remove("hidden");
    }

    async function loadDrives() {
        drives = await fetchJson(DRIVES_API, { method: "GET" });
        renderDriveOptions();
    }

    function resetForm() {
        const form = document.getElementById("placementStatisticsForm");
        form.reset();
        document.getElementById("editingStatisticsId").value = "";
        document.getElementById("submitStatisticsButton").textContent = "Add Statistics";
        document.getElementById("submitStatisticsButton").disabled = false;
        document.getElementById("cancelStatisticsEditButton").classList.add("hidden");
        renderDrivePreview("");
        clearFeedback();
    }

    function populateForm(statistics) {
        document.getElementById("editingStatisticsId").value = String(statistics.id);
        document.getElementById("placementDriveId").value = String(statistics.placementDriveId);
        document.getElementById("studentsApplied").value = statistics.studentsApplied ?? 0;
        document.getElementById("studentsAttended").value = statistics.studentsAttended ?? 0;
        document.getElementById("studentsShortlisted").value = statistics.studentsShortlisted ?? 0;
        document.getElementById("studentsSelected").value = statistics.studentsSelected ?? 0;
        document.getElementById("maleSelected").value = statistics.maleSelected ?? 0;
        document.getElementById("femaleSelected").value = statistics.femaleSelected ?? 0;
        document.getElementById("highestPackage").value = statistics.highestPackage ?? "";
        document.getElementById("averagePackage").value = statistics.averagePackage ?? "";
        document.getElementById("lowestPackage").value = statistics.lowestPackage ?? "";
        document.getElementById("submitStatisticsButton").textContent = "Update Statistics";
        document.getElementById("cancelStatisticsEditButton").classList.remove("hidden");
        renderDrivePreview(statistics.placementDriveId);
        clearFeedback();
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function buildPayload(form) {
        return {
            placementDriveId: form.elements.placementDriveId.value ? Number(form.elements.placementDriveId.value) : null,
            studentsApplied: form.elements.studentsApplied.value ? Number(form.elements.studentsApplied.value) : 0,
            studentsAttended: form.elements.studentsAttended.value ? Number(form.elements.studentsAttended.value) : 0,
            studentsShortlisted: form.elements.studentsShortlisted.value ? Number(form.elements.studentsShortlisted.value) : 0,
            studentsSelected: form.elements.studentsSelected.value ? Number(form.elements.studentsSelected.value) : 0,
            maleSelected: form.elements.maleSelected.value ? Number(form.elements.maleSelected.value) : 0,
            femaleSelected: form.elements.femaleSelected.value ? Number(form.elements.femaleSelected.value) : 0,
            highestPackage: form.elements.highestPackage.value ? Number(form.elements.highestPackage.value) : null,
            averagePackage: form.elements.averagePackage.value ? Number(form.elements.averagePackage.value) : null,
            lowestPackage: form.elements.lowestPackage.value ? Number(form.elements.lowestPackage.value) : null
        };
    }

    function validatePayload(payload) {
        if (!payload.placementDriveId) {
            return "Please select a placement drive.";
        }
        if (payload.studentsApplied < 0 || payload.studentsAttended < 0 || payload.studentsShortlisted < 0
            || payload.studentsSelected < 0 || payload.maleSelected < 0 || payload.femaleSelected < 0) {
            return "All student count fields must be 0 or positive.";
        }
        if (payload.studentsAttended > payload.studentsApplied) {
            return "Students Attended cannot be greater than Students Applied.";
        }
        if (payload.studentsShortlisted > payload.studentsAttended) {
            return "Students Shortlisted cannot be greater than Students Attended.";
        }
        if (payload.studentsSelected > payload.studentsShortlisted) {
            return "Students Selected cannot be greater than Students Shortlisted.";
        }
        if (payload.maleSelected + payload.femaleSelected > payload.studentsSelected) {
            return "Male Selected + Female Selected cannot be greater than Students Selected.";
        }

        const packageValues = [payload.highestPackage, payload.averagePackage, payload.lowestPackage];
        if (packageValues.some(function (value) { return value !== null && value < 0; })) {
            return "Package values must be 0 or positive.";
        }
        if (payload.highestPackage !== null && payload.averagePackage !== null && payload.highestPackage < payload.averagePackage) {
            return "Highest Package must be greater than or equal to Average Package.";
        }
        if (payload.averagePackage !== null && payload.lowestPackage !== null && payload.averagePackage < payload.lowestPackage) {
            return "Average Package must be greater than or equal to Lowest Package.";
        }
        return "";
    }

    function getStatusClass(status) {
        const normalized = (status || "").toLowerCase();
        if (normalized === "upcoming") {
            return "status-upcoming";
        }
        if (normalized === "ongoing") {
            return "status-ongoing";
        }
        if (normalized === "completed") {
            return "status-completed";
        }
        return "status-closed";
    }

    function getLogoMarkup(statistics) {
        if (statistics.companyLogoUrl) {
            return '<img class="statistics-company-logo" src="' + escapeHtml(statistics.companyLogoUrl) + '" alt="' + escapeHtml(statistics.companyName) + ' logo">';
        }
        return '<div class="statistics-company-logo"></div>';
    }

    function packageText(value) {
        return value === null || value === undefined ? "N/A" : value + " LPA";
    }

    async function loadStatistics() {
        const loadingElement = document.getElementById("adminStatisticsLoading");
        const list = document.getElementById("adminStatisticsList");
        const emptyState = document.getElementById("adminStatisticsEmpty");

        loadingElement.classList.remove("hidden");
        list.innerHTML = "";
        emptyState.classList.add("hidden");
        emptyState.textContent = DEFAULT_EMPTY_MESSAGE;

        try {
            const statisticsRecords = await fetchJson(STATISTICS_API, { method: "GET" });
            loadingElement.classList.add("hidden");

            if (!statisticsRecords.length) {
                emptyState.classList.remove("hidden");
                return;
            }

            statisticsRecords.forEach(function (statistics) {
                const card = document.createElement("article");
                card.className = "admin-statistics-item";
                card.innerHTML = [
                    '<div class="statistics-header">',
                    '<div class="statistics-company-block">',
                    getLogoMarkup(statistics),
                    "<div>",
                    "<h3>" + escapeHtml(statistics.driveTitle) + "</h3>",
                    "<p>" + escapeHtml(statistics.companyName) + " | " + escapeHtml(statistics.hiringYear) + " | " + formatDate(statistics.hiringDate) + "</p>",
                    "</div>",
                    "</div>",
                    '<span class="active-badge ' + (statistics.active ? "active-enabled" : "active-disabled") + '">' + (statistics.active ? "Active" : "Disabled") + "</span>",
                    "</div>",
                    '<div class="statistics-badges">',
                    '<span class="status-badge ' + getStatusClass(statistics.driveStatus) + '">' + escapeHtml(statistics.driveStatus) + "</span>",
                    '<span class="stat-pill">Applied: ' + escapeHtml(statistics.studentsApplied) + "</span>",
                    '<span class="stat-pill">Selected: ' + escapeHtml(statistics.studentsSelected) + "</span>",
                    "</div>",
                    '<div class="statistics-meta">',
                    '<span>Attended: ' + escapeHtml(statistics.studentsAttended) + "</span>",
                    '<span>Shortlisted: ' + escapeHtml(statistics.studentsShortlisted) + "</span>",
                    '<span>Male Selected: ' + escapeHtml(statistics.maleSelected) + "</span>",
                    '<span>Female Selected: ' + escapeHtml(statistics.femaleSelected) + "</span>",
                    "</div>",
                    '<div class="statistics-count-grid">',
                    '<span class="stat-pill">Highest: ' + escapeHtml(packageText(statistics.highestPackage)) + "</span>",
                    '<span class="stat-pill">Average: ' + escapeHtml(packageText(statistics.averagePackage)) + "</span>",
                    '<span class="stat-pill">Lowest: ' + escapeHtml(packageText(statistics.lowestPackage)) + "</span>",
                    "</div>",
                    '<div class="statistics-actions">',
                    '<button class="mini-btn toggle-statistics-btn" type="button" data-statistics-id="' + statistics.id + '" data-active="' + statistics.active + '">'
                        + (statistics.active ? "Disable" : "Enable") + "</button>",
                    '<button class="mini-btn edit-statistics-btn" type="button" data-statistics-json="' + encodeURIComponent(JSON.stringify(statistics)) + '">Edit</button>',
                    '<button class="mini-btn delete-btn" type="button" data-statistics-id="' + statistics.id + '">Delete</button>',
                    "</div>"
                ].join("");
                list.appendChild(card);
            });

            bindStatisticsActions();
        } catch (error) {
            loadingElement.classList.add("hidden");
            emptyState.textContent = error.message || "Unable to load placement statistics right now.";
            emptyState.classList.remove("hidden");
        }
    }

    function bindStatisticsActions() {
        document.querySelectorAll(".edit-statistics-btn").forEach(function (button) {
            button.addEventListener("click", function () {
                populateForm(JSON.parse(decodeURIComponent(button.dataset.statisticsJson)));
            });
        });

        document.querySelectorAll(".delete-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                if (!window.confirm("Delete this placement statistics record?")) {
                    return;
                }

                try {
                    clearFeedback();
                    await fetchJson(STATISTICS_API + "/" + button.dataset.statisticsId, { method: "DELETE" });
                    if (document.getElementById("editingStatisticsId").value === button.dataset.statisticsId) {
                        resetForm();
                    }
                    setFeedback("Placement statistics deleted successfully.", false);
                    await loadStatistics();
                } catch (error) {
                    setFeedback(error.message || "Unable to delete placement statistics.", true);
                }
            });
        });

        document.querySelectorAll(".toggle-statistics-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                const currentState = button.dataset.active === "true";

                try {
                    clearFeedback();
                    await fetchJson(STATISTICS_API + "/" + button.dataset.statisticsId + "/status?active=" + (!currentState), {
                        method: "PATCH"
                    });
                    setFeedback("Placement statistics status updated successfully.", false);
                    await loadStatistics();
                } catch (error) {
                    setFeedback(error.message || "Unable to update placement statistics status.", true);
                }
            });
        });
    }

    function setupForm() {
        const form = document.getElementById("placementStatisticsForm");
        if (!form) {
            return;
        }

        document.getElementById("placementDriveId").addEventListener("change", function () {
            renderDrivePreview(this.value);
        });

        document.getElementById("cancelStatisticsEditButton").addEventListener("click", resetForm);

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const payload = buildPayload(form);
            const validationMessage = validatePayload(payload);
            if (validationMessage) {
                setFeedback(validationMessage, true);
                return;
            }

            const editingStatisticsId = document.getElementById("editingStatisticsId").value;

            try {
                clearFeedback();
                setLoading(true);
                await fetchJson(editingStatisticsId ? STATISTICS_API + "/" + editingStatisticsId : STATISTICS_API, {
                    method: editingStatisticsId ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                resetForm();
                setFeedback(editingStatisticsId ? "Placement statistics updated successfully." : "Placement statistics created successfully.", false);
                await loadStatistics();
            } catch (error) {
                setFeedback(error.message || "Unable to save placement statistics.", true);
            } finally {
                setLoading(false);
            }
        });
    }

    document.addEventListener("DOMContentLoaded", async function () {
        if (!document.getElementById("placementStatisticsForm")) {
            return;
        }

        try {
            await loadDrives();
            setupForm();
            await loadStatistics();
        } catch (error) {
            setFeedback(error.message || "Unable to initialize placement statistics management.", true);
        }
    });
})();
