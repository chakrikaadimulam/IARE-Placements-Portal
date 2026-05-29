(function () {
    const STATISTICS_API = "/api/student/placement-statistics";
    let allStatistics = [];

    async function fetchStatistics() {
        const response = await fetch(STATISTICS_API);
        const payload = await response.json().catch(function () {
            return [];
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load placement statistics.");
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

    function getLogoMarkup(statistics) {
        if (statistics.companyLogoUrl) {
            return '<img class="student-statistics-logo" src="' + escapeHtml(statistics.companyLogoUrl) + '" alt="' + escapeHtml(statistics.companyName) + ' logo">';
        }
        return '<div class="student-statistics-logo"></div>';
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

    function populateYearFilter(statisticsRecords) {
        const yearFilter = document.getElementById("statisticsYearFilter");
        const years = Array.from(new Set(statisticsRecords.map(function (record) {
            return record.hiringYear;
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

    function safePercentage(numerator, denominator) {
        if (!denominator || denominator <= 0) {
            return 0;
        }
        return Math.round((numerator / denominator) * 100);
    }

    function packageText(value) {
        return value === null || value === undefined ? "N/A" : value + " LPA";
    }

    function renderProgressRow(label, numerator, denominator) {
        const percentage = safePercentage(numerator, denominator);
        return [
            '<div class="progress-row">',
            '<div class="chart-label">' + escapeHtml(label) + ' - ' + escapeHtml(percentage) + '%</div>',
            '<div class="progress-track"><div class="progress-fill" style="width:' + percentage + '%"></div></div>',
            "</div>"
        ].join("");
    }

    function renderGenderSplit(maleSelected, femaleSelected) {
        const total = maleSelected + femaleSelected;
        const malePercentage = safePercentage(maleSelected, total);
        const femalePercentage = safePercentage(femaleSelected, total);

        return [
            '<div class="gender-split">',
            '<div class="chart-label">Gender Split</div>',
            '<div class="gender-bar">',
            '<div class="male-bar" style="width:' + malePercentage + '%"></div>',
            '<div class="female-bar" style="width:' + femalePercentage + '%"></div>',
            '</div>',
            '<div class="gender-meta">',
            '<span>Male: ' + escapeHtml(maleSelected) + ' (' + malePercentage + '%)</span>',
            '<span>Female: ' + escapeHtml(femaleSelected) + ' (' + femalePercentage + '%)</span>',
            '</div>',
            '</div>'
        ].join("");
    }

    function renderStatistics(records) {
        const loadingElement = document.getElementById("studentStatisticsLoading");
        const list = document.getElementById("studentStatisticsList");
        const emptyState = document.getElementById("studentStatisticsEmpty");

        loadingElement.classList.add("hidden");
        list.innerHTML = "";

        if (!records.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");

        records.forEach(function (statistics) {
            const card = document.createElement("article");
            card.className = "student-statistics-card";
            card.innerHTML = [
                '<div class="student-statistics-header">',
                getLogoMarkup(statistics),
                "<div>",
                "<h3>" + escapeHtml(statistics.driveTitle) + "</h3>",
                "<p>" + escapeHtml(statistics.companyName) + " | " + escapeHtml(statistics.hiringYear) + " | " + formatDate(statistics.hiringDate) + "</p>",
                "</div>",
                "</div>",
                '<div class="badge-row">',
                '<span class="type-badge">' + escapeHtml(statistics.companyType) + "</span>",
                '<span class="industry-badge">' + escapeHtml(statistics.industry) + "</span>",
                '<span class="status-badge ' + getStatusClass(statistics.driveStatus) + '">' + escapeHtml(statistics.driveStatus) + "</span>",
                "</div>",
                '<div class="statistics-metrics">',
                '<span class="metric-pill">Applied: ' + escapeHtml(statistics.studentsApplied) + '</span>',
                '<span class="metric-pill">Attended: ' + escapeHtml(statistics.studentsAttended) + '</span>',
                '<span class="metric-pill">Shortlisted: ' + escapeHtml(statistics.studentsShortlisted) + '</span>',
                '<span class="metric-pill">Selected: ' + escapeHtml(statistics.studentsSelected) + '</span>',
                '<span class="metric-pill">Male: ' + escapeHtml(statistics.maleSelected) + '</span>',
                '<span class="metric-pill">Female: ' + escapeHtml(statistics.femaleSelected) + '</span>',
                '</div>',
                '<div class="package-metrics">',
                '<span class="metric-pill">Highest: ' + escapeHtml(packageText(statistics.highestPackage)) + '</span>',
                '<span class="metric-pill">Average: ' + escapeHtml(packageText(statistics.averagePackage)) + '</span>',
                '<span class="metric-pill">Lowest: ' + escapeHtml(packageText(statistics.lowestPackage)) + '</span>',
                '</div>',
                '<div class="chart-group">',
                renderProgressRow("Attended from Applied", statistics.studentsAttended, statistics.studentsApplied),
                renderProgressRow("Shortlisted from Attended", statistics.studentsShortlisted, statistics.studentsAttended),
                renderProgressRow("Selected from Shortlisted", statistics.studentsSelected, statistics.studentsShortlisted),
                renderGenderSplit(statistics.maleSelected, statistics.femaleSelected),
                '</div>'
            ].join("");
            list.appendChild(card);
        });
    }

    function filterStatistics() {
        const searchValue = document.getElementById("statisticsSearchInput").value.trim().toLowerCase();
        const yearValue = document.getElementById("statisticsYearFilter").value;
        const statusValue = document.getElementById("statisticsStatusFilter").value;

        const filteredRecords = allStatistics.filter(function (record) {
            const matchesSearch = !searchValue
                || record.companyName.toLowerCase().includes(searchValue)
                || record.driveTitle.toLowerCase().includes(searchValue);
            const matchesYear = !yearValue || String(record.hiringYear) === yearValue;
            const matchesStatus = !statusValue || record.driveStatus === statusValue;

            return matchesSearch && matchesYear && matchesStatus;
        });

        renderStatistics(filteredRecords);
    }

    function showError(message) {
        const loadingElement = document.getElementById("studentStatisticsLoading");
        const emptyState = document.getElementById("studentStatisticsEmpty");

        loadingElement.classList.add("hidden");
        emptyState.textContent = message;
        emptyState.classList.remove("hidden");
    }

    function setupFilters() {
        ["statisticsSearchInput", "statisticsYearFilter", "statisticsStatusFilter"].forEach(function (id) {
            document.getElementById(id).addEventListener("input", filterStatistics);
            document.getElementById(id).addEventListener("change", filterStatistics);
        });
    }

    document.addEventListener("DOMContentLoaded", async function () {
        try {
            allStatistics = await fetchStatistics();
            populateYearFilter(allStatistics);
            setupFilters();
            renderStatistics(allStatistics);
        } catch (error) {
            showError(error.message || "Unable to load placement statistics right now.");
        }
    });
})();
