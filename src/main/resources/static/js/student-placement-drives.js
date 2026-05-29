(function () {
    const DRIVES_API = "/api/student/placement-drives";
    let allDrives = [];

    async function fetchDrives() {
        const response = await fetch(DRIVES_API);
        const payload = await response.json().catch(function () {
            return [];
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load placement drives.");
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

    function getLogoMarkup(drive) {
        if (drive.companyLogoUrl) {
            return '<img class="student-drive-logo" src="' + escapeHtml(drive.companyLogoUrl) + '" alt="' + escapeHtml(drive.companyName) + ' logo">';
        }
        return '<div class="student-drive-logo"></div>';
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

    function populateYearFilter(drives) {
        const yearFilter = document.getElementById("driveYearFilter");
        const years = Array.from(new Set(drives.map(function (drive) {
            return drive.hiringYear;
        }))).sort(function (first, second) {
            return second - first;
        });

        yearFilter.innerHTML = '<option value="">All Hiring Years</option>';
        years.forEach(function (year) {
            const option = document.createElement("option");
            option.value = String(year);
            option.textContent = String(year);
            yearFilter.appendChild(option);
        });
    }

    function filterDrives() {
        const searchValue = document.getElementById("driveSearchInput").value.trim().toLowerCase();
        const yearValue = document.getElementById("driveYearFilter").value;
        const statusValue = document.getElementById("driveStatusFilter").value;
        const jobTypeValue = document.getElementById("driveJobTypeFilter").value;

        const filteredDrives = allDrives.filter(function (drive) {
            const matchesSearch = !searchValue
                || drive.companyName.toLowerCase().includes(searchValue)
                || drive.driveTitle.toLowerCase().includes(searchValue);
            const matchesYear = !yearValue || String(drive.hiringYear) === yearValue;
            const matchesStatus = !statusValue || drive.driveStatus === statusValue;
            const matchesJobType = !jobTypeValue || drive.jobType === jobTypeValue;

            return matchesSearch && matchesYear && matchesStatus && matchesJobType;
        });

        renderDrives(filteredDrives);
    }

    function renderDrives(drives) {
        const loadingElement = document.getElementById("studentDrivesLoading");
        const list = document.getElementById("studentDriveList");
        const emptyState = document.getElementById("studentDriveEmpty");

        loadingElement.classList.add("hidden");
        list.innerHTML = "";

        if (!drives.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");

        drives.forEach(function (drive) {
            const websiteMarkup = drive.companyWebsiteUrl
                ? '<a class="primary-btn company-website-btn" href="' + escapeHtml(drive.companyWebsiteUrl) + '" target="_blank" rel="noopener noreferrer">Visit Website</a>'
                : "";

            const card = document.createElement("article");
            card.className = "student-drive-card";
            card.innerHTML = [
                '<div class="student-drive-header">',
                getLogoMarkup(drive),
                "<div>",
                "<h3>" + escapeHtml(drive.driveTitle) + "</h3>",
                "<p>" + escapeHtml(drive.companyName) + " | Hiring Year " + escapeHtml(drive.hiringYear) + "</p>",
                "</div>",
                "</div>",
                '<div class="student-drive-badges">',
                '<span class="status-badge ' + getStatusClass(drive.driveStatus) + '">' + escapeHtml(drive.driveStatus) + "</span>",
                '<span class="mode-badge">' + escapeHtml(drive.hiringMode) + "</span>",
                '<span class="job-badge">' + escapeHtml(drive.jobType) + "</span>",
                "</div>",
                '<div class="student-drive-meta">',
                "<span>Hiring Date: " + formatDate(drive.hiringDate) + "</span>",
                (drive.hiringLocation ? "<span>Location: " + escapeHtml(drive.hiringLocation) + "</span>" : ""),
                "<span>Eligible Branches: " + escapeHtml(drive.eligibleBranches) + "</span>",
                "<span>Eligible CGPA: " + escapeHtml(drive.eligibleCgpa) + "</span>",
                "<span>Backlogs Allowed: " + (drive.backlogsAllowed ? "Yes" : "No") + "</span>",
                (drive.backlogsAllowed && drive.maxBacklogs !== null ? "<span>Max Backlogs: " + escapeHtml(drive.maxBacklogs) + "</span>" : ""),
                "<span>CTC: " + escapeHtml(drive.ctcPackage) + "</span>",
                (drive.stipend ? "<span>Stipend: " + escapeHtml(drive.stipend) + "</span>" : ""),
                (drive.numberOfRounds !== null ? "<span>Rounds: " + escapeHtml(drive.numberOfRounds) + "</span>" : ""),
                (drive.roundNames ? "<span>Round Names: " + escapeHtml(drive.roundNames) + "</span>" : ""),
                (drive.registrationDeadline ? "<span>Registration Deadline: " + formatDate(drive.registrationDeadline) + "</span>" : ""),
                (drive.examDate ? "<span>Exam Date: " + formatDate(drive.examDate) + "</span>" : ""),
                (drive.interviewDate ? "<span>Interview Date: " + formatDate(drive.interviewDate) + "</span>" : ""),
                "</div>",
                (drive.description ? "<p>" + escapeHtml(drive.description).replace(/\n/g, "<br>") + "</p>" : ""),
                websiteMarkup
            ].join("");
            list.appendChild(card);
        });
    }

    function showError(message) {
        const loadingElement = document.getElementById("studentDrivesLoading");
        const emptyState = document.getElementById("studentDriveEmpty");

        loadingElement.classList.add("hidden");
        emptyState.textContent = message;
        emptyState.classList.remove("hidden");
    }

    function setupFilters() {
        ["driveSearchInput", "driveYearFilter", "driveStatusFilter", "driveJobTypeFilter"].forEach(function (id) {
            document.getElementById(id).addEventListener("input", filterDrives);
            document.getElementById(id).addEventListener("change", filterDrives);
        });
    }

    document.addEventListener("DOMContentLoaded", async function () {
        try {
            allDrives = await fetchDrives();
            populateYearFilter(allDrives);
            setupFilters();
            renderDrives(allDrives);
        } catch (error) {
            showError(error.message || "Unable to load placement drives right now.");
        }
    });
})();
