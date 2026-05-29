(function () {
    const EXPERIENCES_API = "/api/student/interview-experiences";
    let allExperiences = [];

    async function fetchExperiences() {
        const response = await fetch(EXPERIENCES_API);
        const payload = await response.json().catch(function () {
            return [];
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load interview experiences.");
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

    function getDifficultyClass(value) {
        return "difficulty-" + String(value || "").toLowerCase();
    }

    function getResultClass(value) {
        return "result-" + String(value || "").toLowerCase().replace(/\s+/g, "-");
    }

    function getCompanyLogoMarkup(experience) {
        if (experience.companyLogoUrl) {
            return '<img class="student-company-logo" src="' + escapeHtml(experience.companyLogoUrl) + '" alt="' + escapeHtml(experience.companyName) + ' logo">';
        }
        return '<div class="student-company-logo"></div>';
    }

    function getPhotoMarkup(experience) {
        if (experience.studentPhotoUrl) {
            return '<img class="student-photo" src="' + escapeHtml(experience.studentPhotoUrl) + '" alt="' + escapeHtml(experience.studentName) + ' photo">';
        }
        return '<div class="student-photo"></div>';
    }

    function populateFilters(experiences) {
        populateSelect("experienceDriveFilter", experiences.map(function (experience) {
            return experience.companyName + " - " + experience.driveTitle + " (" + experience.hiringYear + ")";
        }));
        populateSelect("experienceYearFilter", experiences.map(function (experience) {
            return String(experience.hiringYear);
        }));
    }

    function populateSelect(id, values) {
        const select = document.getElementById(id);
        const firstOption = select.options[0].outerHTML;
        const uniqueValues = Array.from(new Set(values.filter(Boolean))).sort();

        select.innerHTML = firstOption;
        uniqueValues.forEach(function (value) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            select.appendChild(option);
        });
    }

    function buildContentBlock(title, content, isFullWidth) {
        return [
            '<div class="experience-content-block ' + (isFullWidth ? "full-width" : "") + '">',
            "<h5>" + escapeHtml(title) + "</h5>",
            "<p>" + escapeHtml(content || "Not provided.") + "</p>",
            "</div>"
        ].join("");
    }

    function renderExperiences(experiences) {
        const loadingElement = document.getElementById("studentExperienceLoading");
        const list = document.getElementById("studentExperienceList");
        const emptyState = document.getElementById("studentExperienceEmpty");

        loadingElement.classList.add("hidden");
        list.innerHTML = "";

        if (!experiences.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");

        experiences.forEach(function (experience) {
            const driveLabel = experience.companyName + " - " + experience.driveTitle + " (" + experience.hiringYear + ")";
            const websiteButton = experience.companyWebsiteUrl
                ? '<div class="experience-actions"><a class="secondary-btn" href="' + escapeHtml(experience.companyWebsiteUrl) + '" target="_blank" rel="noopener noreferrer">Visit Website</a></div>'
                : "";

            const card = document.createElement("article");
            card.className = "student-experience-card";
            card.innerHTML = [
                '<div class="student-experience-header">',
                getCompanyLogoMarkup(experience),
                "<div>",
                "<h3>" + escapeHtml(experience.companyName) + "</h3>",
                "<p>" + escapeHtml(experience.driveTitle) + " | " + escapeHtml(experience.hiringYear) + " | " + formatDate(experience.hiringDate) + "</p>",
                "</div>",
                "</div>",
                '<div class="student-experience-badges">',
                '<span class="role-badge">' + escapeHtml(experience.roleOffered) + "</span>",
                '<span class="difficulty-badge ' + getDifficultyClass(experience.difficultyLevel) + '">' + escapeHtml(experience.difficultyLevel) + "</span>",
                '<span class="result-badge ' + getResultClass(experience.finalResult) + '">' + escapeHtml(experience.finalResult) + "</span>",
                "</div>",
                '<div class="student-experience-student">',
                getPhotoMarkup(experience),
                '<div class="student-student-profile"><h4>' + escapeHtml(experience.studentName) + '</h4><p>' + escapeHtml(experience.roleOffered) + '</p></div>',
                "</div>",
                '<div class="student-experience-meta">',
                "<span>Drive: " + escapeHtml(driveLabel) + "</span>",
                "<span>Rounds Faced: " + escapeHtml(experience.roundsFaced) + "</span>",
                "<span>Experience Date: " + formatDate(experience.experienceDate) + "</span>",
                "</div>",
                '<div class="experience-content-grid">',
                buildContentBlock("Questions Asked", experience.questionsAsked, true),
                buildContentBlock("Coding Questions", experience.codingQuestions, false),
                buildContentBlock("Technical Topics", experience.technicalTopics, false),
                buildContentBlock("HR Questions", experience.hrQuestions, false),
                buildContentBlock("Preparation Tips", experience.preparationTips, true),
                "</div>",
                websiteButton
            ].join("");
            list.appendChild(card);
        });
    }

    function filterExperiences() {
        const searchValue = document.getElementById("experienceSearchInput").value.trim().toLowerCase();
        const driveValue = document.getElementById("experienceDriveFilter").value;
        const difficultyValue = document.getElementById("experienceDifficultyFilter").value;
        const resultValue = document.getElementById("experienceFinalResultFilter").value;
        const yearValue = document.getElementById("experienceYearFilter").value;

        const filteredExperiences = allExperiences.filter(function (experience) {
            const driveLabel = experience.companyName + " - " + experience.driveTitle + " (" + experience.hiringYear + ")";
            const technicalTopics = (experience.technicalTopics || "").toLowerCase();
            const matchesSearch = !searchValue
                || experience.studentName.toLowerCase().includes(searchValue)
                || experience.companyName.toLowerCase().includes(searchValue)
                || experience.driveTitle.toLowerCase().includes(searchValue)
                || experience.roleOffered.toLowerCase().includes(searchValue)
                || technicalTopics.includes(searchValue);
            const matchesDrive = !driveValue || driveLabel === driveValue;
            const matchesDifficulty = !difficultyValue || experience.difficultyLevel === difficultyValue;
            const matchesResult = !resultValue || experience.finalResult === resultValue;
            const matchesYear = !yearValue || String(experience.hiringYear) === yearValue;

            return matchesSearch && matchesDrive && matchesDifficulty && matchesResult && matchesYear;
        });

        renderExperiences(filteredExperiences);
    }

    function setupFilters() {
        [
            "experienceSearchInput",
            "experienceDriveFilter",
            "experienceDifficultyFilter",
            "experienceFinalResultFilter",
            "experienceYearFilter"
        ].forEach(function (id) {
            document.getElementById(id).addEventListener("input", filterExperiences);
            document.getElementById(id).addEventListener("change", filterExperiences);
        });
    }

    function showError(message) {
        const loadingElement = document.getElementById("studentExperienceLoading");
        const emptyState = document.getElementById("studentExperienceEmpty");

        loadingElement.classList.add("hidden");
        emptyState.textContent = message;
        emptyState.classList.remove("hidden");
    }

    document.addEventListener("DOMContentLoaded", async function () {
        try {
            allExperiences = await fetchExperiences();
            populateFilters(allExperiences);
            setupFilters();
            renderExperiences(allExperiences);
        } catch (error) {
            showError(error.message || "Unable to load interview experiences right now.");
        }
    });
})();
