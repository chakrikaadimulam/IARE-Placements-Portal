(function () {
    const EXPERIENCES_API = "/api/admin/interview-experiences";
    const DRIVES_API = "/api/admin/placement-drives";
    const DEFAULT_EMPTY_MESSAGE = "No interview experiences added yet. Add the first record to start building your knowledge base.";
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
        const successElement = document.getElementById("experienceSuccess");
        const errorElement = document.getElementById("experienceError");

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
        document.getElementById("experienceSuccess").textContent = "";
        document.getElementById("experienceSuccess").classList.add("hidden");
        document.getElementById("experienceError").textContent = "";
    }

    function setLoading(isLoading) {
        const submitButton = document.getElementById("submitExperienceButton");
        submitButton.disabled = isLoading;
        submitButton.textContent = isLoading
            ? "Saving..."
            : (document.getElementById("editingExperienceId").value ? "Update Experience" : "Add Experience");
    }

    function renderDriveOptions() {
        const select = document.getElementById("experiencePlacementDriveId");
        select.innerHTML = '<option value="">Select placement drive</option>';

        drives.forEach(function (drive) {
            const option = document.createElement("option");
            option.value = String(drive.id);
            option.textContent = drive.companyName + " - " + drive.driveTitle + " (" + drive.hiringYear + ")";
            select.appendChild(option);
        });
    }

    function renderDrivePreview(placementDriveId) {
        const preview = document.getElementById("experienceDrivePreview");
        const drive = drives.find(function (item) {
            return String(item.id) === String(placementDriveId);
        });

        if (!drive) {
            preview.classList.add("hidden");
            preview.innerHTML = "";
            return;
        }

        const logoMarkup = drive.companyLogoUrl
            ? '<img class="experience-drive-logo" src="' + escapeHtml(drive.companyLogoUrl) + '" alt="' + escapeHtml(drive.companyName) + ' logo">'
            : '<div class="experience-drive-logo"></div>';

        preview.innerHTML = [
            logoMarkup,
            "<div>",
            "<strong>" + escapeHtml(drive.companyName) + "</strong>",
            "<p>" + escapeHtml(drive.driveTitle) + " | " + escapeHtml(drive.hiringYear) + " | " + formatDate(drive.hiringDate) + "</p>",
            "</div>"
        ].join("");
        preview.classList.remove("hidden");
    }

    async function loadDrives() {
        drives = await fetchJson(DRIVES_API, { method: "GET" });
        renderDriveOptions();
    }

    function resetForm() {
        document.getElementById("interviewExperienceForm").reset();
        document.getElementById("editingExperienceId").value = "";
        document.getElementById("submitExperienceButton").textContent = "Add Experience";
        document.getElementById("submitExperienceButton").disabled = false;
        document.getElementById("cancelExperienceEditButton").classList.add("hidden");
        renderDrivePreview("");
        clearFeedback();
    }

    function populateForm(experience) {
        document.getElementById("editingExperienceId").value = String(experience.id);
        document.getElementById("experiencePlacementDriveId").value = String(experience.placementDriveId);
        document.getElementById("experienceStudentName").value = experience.studentName || "";
        document.getElementById("experienceStudentPhotoUrl").value = experience.studentPhotoUrl || "";
        document.getElementById("experienceRoleOffered").value = experience.roleOffered || "";
        document.getElementById("experienceDifficultyLevel").value = experience.difficultyLevel || "";
        document.getElementById("experienceRoundsFaced").value = experience.roundsFaced || "";
        document.getElementById("experienceQuestionsAsked").value = experience.questionsAsked || "";
        document.getElementById("experienceCodingQuestions").value = experience.codingQuestions || "";
        document.getElementById("experienceTechnicalTopics").value = experience.technicalTopics || "";
        document.getElementById("experienceHrQuestions").value = experience.hrQuestions || "";
        document.getElementById("experiencePreparationTips").value = experience.preparationTips || "";
        document.getElementById("experienceFinalResult").value = experience.finalResult || "";
        document.getElementById("experienceDate").value = experience.experienceDate || "";
        document.getElementById("submitExperienceButton").textContent = "Update Experience";
        document.getElementById("cancelExperienceEditButton").classList.remove("hidden");
        renderDrivePreview(experience.placementDriveId);
        clearFeedback();
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function buildPayload(form) {
        return {
            placementDriveId: form.elements.placementDriveId.value ? Number(form.elements.placementDriveId.value) : null,
            studentName: form.elements.studentName.value.trim(),
            studentPhotoUrl: form.elements.studentPhotoUrl.value.trim(),
            roleOffered: form.elements.roleOffered.value.trim(),
            difficultyLevel: form.elements.difficultyLevel.value,
            roundsFaced: form.elements.roundsFaced.value.trim(),
            questionsAsked: form.elements.questionsAsked.value.trim(),
            codingQuestions: form.elements.codingQuestions.value.trim(),
            technicalTopics: form.elements.technicalTopics.value.trim(),
            hrQuestions: form.elements.hrQuestions.value.trim(),
            preparationTips: form.elements.preparationTips.value.trim(),
            finalResult: form.elements.finalResult.value,
            experienceDate: form.elements.experienceDate.value
        };
    }

    function validatePayload(payload) {
        if (!payload.placementDriveId || !payload.studentName || !payload.roleOffered || !payload.difficultyLevel
            || !payload.roundsFaced || !payload.questionsAsked || !payload.preparationTips
            || !payload.finalResult || !payload.experienceDate) {
            return "Please fill all required interview experience fields.";
        }
        return "";
    }

    function getDifficultyClass(value) {
        return "difficulty-" + String(value || "").toLowerCase();
    }

    function getResultClass(value) {
        return "result-" + String(value || "").toLowerCase().replace(/\s+/g, "-");
    }

    function getLogoMarkup(experience) {
        if (experience.companyLogoUrl) {
            return '<img class="experience-company-logo" src="' + escapeHtml(experience.companyLogoUrl) + '" alt="' + escapeHtml(experience.companyName) + ' logo">';
        }
        return '<div class="experience-company-logo"></div>';
    }

    function getPhotoMarkup(experience) {
        if (experience.studentPhotoUrl) {
            return '<img class="experience-student-photo" src="' + escapeHtml(experience.studentPhotoUrl) + '" alt="' + escapeHtml(experience.studentName) + ' photo">';
        }
        return '<div class="experience-student-photo"></div>';
    }

    function getShortText(value, maxLength) {
        const text = value || "";
        return text.length > maxLength ? text.slice(0, maxLength).trim() + "..." : text;
    }

    function buildTextGroup(label, value) {
        return [
            '<div class="experience-text-group">',
            "<h4>" + escapeHtml(label) + "</h4>",
            "<p>" + escapeHtml(value || "Not provided.") + "</p>",
            "</div>"
        ].join("");
    }

    function openExperienceModal(experience) {
        const modal = document.getElementById("experienceDetailsModal");
        const body = document.getElementById("experienceModalBody");
        const logoMarkup = experience.companyLogoUrl
            ? '<img class="experience-modal-logo" src="' + escapeHtml(experience.companyLogoUrl) + '" alt="' + escapeHtml(experience.companyName) + ' logo">'
            : '<div class="experience-modal-logo"></div>';
        const photoMarkup = experience.studentPhotoUrl
            ? '<img class="experience-modal-student-photo" src="' + escapeHtml(experience.studentPhotoUrl) + '" alt="' + escapeHtml(experience.studentName) + ' photo">'
            : '<div class="experience-modal-student-photo"></div>';

        body.innerHTML = [
            '<div class="experience-modal-hero">',
            logoMarkup,
            "<div>",
            "<h3>" + escapeHtml(experience.companyName) + "</h3>",
            "<p>" + escapeHtml(experience.driveTitle) + " | " + escapeHtml(experience.hiringYear) + " | " + formatDate(experience.hiringDate) + "</p>",
            '<div class="experience-card-badges">',
            '<span class="role-badge">' + escapeHtml(experience.roleOffered) + "</span>",
            '<span class="difficulty-badge ' + getDifficultyClass(experience.difficultyLevel) + '">' + escapeHtml(experience.difficultyLevel) + "</span>",
            '<span class="result-badge ' + getResultClass(experience.finalResult) + '">' + escapeHtml(experience.finalResult) + "</span>",
            "</div>",
            "</div>",
            "</div>",
            '<div class="experience-modal-copy">',
            '<div class="experience-card-student">' + photoMarkup + "<div><h4>" + escapeHtml(experience.studentName) + "</h4><p>Experience Date: " + formatDate(experience.experienceDate) + " | Rounds Faced: " + escapeHtml(experience.roundsFaced) + "</p></div></div>",
            "</div>",
            '<div class="experience-text-grid">',
            buildTextGroup("Questions Asked", experience.questionsAsked),
            buildTextGroup("Coding Questions", experience.codingQuestions),
            buildTextGroup("Technical Topics", experience.technicalTopics),
            buildTextGroup("HR Questions", experience.hrQuestions),
            buildTextGroup("Preparation Tips", experience.preparationTips),
            "</div>"
        ].join("");

        modal.classList.remove("hidden");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeExperienceModal() {
        const modal = document.getElementById("experienceDetailsModal");
        modal.classList.add("hidden");
        modal.setAttribute("aria-hidden", "true");
    }

    async function loadExperiences() {
        const loadingElement = document.getElementById("adminExperienceLoading");
        const list = document.getElementById("adminExperienceList");
        const emptyState = document.getElementById("adminExperienceEmpty");

        loadingElement.classList.remove("hidden");
        list.innerHTML = "";
        emptyState.classList.add("hidden");
        emptyState.textContent = DEFAULT_EMPTY_MESSAGE;

        try {
            const experiences = await fetchJson(EXPERIENCES_API, { method: "GET" });
            loadingElement.classList.add("hidden");

            if (!experiences.length) {
                emptyState.classList.remove("hidden");
                return;
            }

            experiences.forEach(function (experience) {
                const card = document.createElement("article");
                card.className = "admin-experience-item";
                card.innerHTML = [
                    '<div class="experience-card-header">',
                    '<div class="experience-card-header-main">',
                    getLogoMarkup(experience),
                    "<div>",
                    "<h3>" + escapeHtml(experience.driveTitle) + "</h3>",
                    "<p>" + escapeHtml(experience.companyName) + " | Hiring Year " + escapeHtml(experience.hiringYear) + "</p>",
                    "</div>",
                    "</div>",
                    '<span class="status-badge ' + (experience.active ? "status-active" : "status-disabled") + '">' + (experience.active ? "Active" : "Disabled") + "</span>",
                    "</div>",
                    '<div class="experience-card-badges">',
                    '<span class="role-badge">' + escapeHtml(experience.roleOffered) + "</span>",
                    '<span class="difficulty-badge ' + getDifficultyClass(experience.difficultyLevel) + '">' + escapeHtml(experience.difficultyLevel) + "</span>",
                    '<span class="result-badge ' + getResultClass(experience.finalResult) + '">' + escapeHtml(experience.finalResult) + "</span>",
                    "</div>",
                    '<div class="experience-card-student">',
                    getPhotoMarkup(experience),
                    "<div>",
                    "<h4>" + escapeHtml(experience.studentName) + "</h4>",
                    "<p>" + escapeHtml(experience.roundsFaced) + "</p>",
                    "</div>",
                    "</div>",
                    '<div class="experience-card-meta">',
                    "<span>Experience Date: " + formatDate(experience.experienceDate) + "</span>",
                    "<span>Questions: " + escapeHtml(getShortText(experience.questionsAsked, 120)) + "</span>",
                    "</div>",
                    '<div class="experience-card-actions">',
                    '<button class="mini-btn details-experience-btn" type="button" data-experience-json="' + encodeURIComponent(JSON.stringify(experience)) + '">View Details</button>',
                    '<button class="mini-btn toggle-experience-btn" type="button" data-experience-id="' + experience.id + '" data-active="' + experience.active + '">'
                        + (experience.active ? "Disable" : "Enable") + "</button>",
                    '<button class="mini-btn edit-experience-btn" type="button" data-experience-json="' + encodeURIComponent(JSON.stringify(experience)) + '">Edit</button>',
                    '<button class="mini-btn delete-btn" type="button" data-experience-id="' + experience.id + '">Delete</button>',
                    "</div>"
                ].join("");
                list.appendChild(card);
            });

            bindExperienceActions();
        } catch (error) {
            loadingElement.classList.add("hidden");
            emptyState.textContent = error.message || "Unable to load interview experiences right now.";
            emptyState.classList.remove("hidden");
        }
    }

    function bindExperienceActions() {
        document.querySelectorAll(".details-experience-btn").forEach(function (button) {
            button.addEventListener("click", function () {
                openExperienceModal(JSON.parse(decodeURIComponent(button.dataset.experienceJson)));
            });
        });

        document.querySelectorAll(".edit-experience-btn").forEach(function (button) {
            button.addEventListener("click", function () {
                populateForm(JSON.parse(decodeURIComponent(button.dataset.experienceJson)));
            });
        });

        document.querySelectorAll(".delete-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                if (!window.confirm("Delete this interview experience record?")) {
                    return;
                }

                try {
                    clearFeedback();
                    await fetchJson(EXPERIENCES_API + "/" + button.dataset.experienceId, { method: "DELETE" });
                    if (document.getElementById("editingExperienceId").value === button.dataset.experienceId) {
                        resetForm();
                    }
                    setFeedback("Interview experience deleted successfully.", false);
                    await loadExperiences();
                } catch (error) {
                    setFeedback(error.message || "Unable to delete interview experience.", true);
                }
            });
        });

        document.querySelectorAll(".toggle-experience-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                const currentState = button.dataset.active === "true";

                try {
                    clearFeedback();
                    await fetchJson(EXPERIENCES_API + "/" + button.dataset.experienceId + "/status?active=" + (!currentState), {
                        method: "PATCH"
                    });
                    setFeedback("Interview experience status updated successfully.", false);
                    await loadExperiences();
                } catch (error) {
                    setFeedback(error.message || "Unable to update interview experience status.", true);
                }
            });
        });
    }

    function setupModal() {
        document.getElementById("closeExperienceModalButton").addEventListener("click", closeExperienceModal);
        document.querySelectorAll("[data-close-modal='true']").forEach(function (element) {
            element.addEventListener("click", closeExperienceModal);
        });
    }

    function setupForm() {
        const form = document.getElementById("interviewExperienceForm");
        if (!form) {
            return;
        }

        document.getElementById("experiencePlacementDriveId").addEventListener("change", function () {
            renderDrivePreview(this.value);
        });

        document.getElementById("cancelExperienceEditButton").addEventListener("click", resetForm);

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const payload = buildPayload(form);
            const validationMessage = validatePayload(payload);
            if (validationMessage) {
                setFeedback(validationMessage, true);
                return;
            }

            const editingExperienceId = document.getElementById("editingExperienceId").value;

            try {
                clearFeedback();
                setLoading(true);
                await fetchJson(editingExperienceId ? EXPERIENCES_API + "/" + editingExperienceId : EXPERIENCES_API, {
                    method: editingExperienceId ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                resetForm();
                setFeedback(editingExperienceId ? "Interview experience updated successfully." : "Interview experience created successfully.", false);
                await loadExperiences();
            } catch (error) {
                setFeedback(error.message || "Unable to save interview experience.", true);
            } finally {
                setLoading(false);
            }
        });
    }

    document.addEventListener("DOMContentLoaded", async function () {
        if (!document.getElementById("interviewExperienceForm")) {
            return;
        }

        try {
            await loadDrives();
            setupModal();
            setupForm();
            await loadExperiences();
        } catch (error) {
            setFeedback(error.message || "Unable to initialize interview experience management.", true);
        }
    });
})();
