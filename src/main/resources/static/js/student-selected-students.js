(function () {
    const SELECTED_STUDENTS_API = "/api/student/selected-students";
    let allStudents = [];

    async function fetchSelectedStudents() {
        const response = await fetch(SELECTED_STUDENTS_API);
        const payload = await response.json().catch(function () {
            return [];
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load selected student records.");
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

    function getCompanyLogoMarkup(student) {
        if (student.companyLogoUrl) {
            return '<img class="student-company-logo" src="' + escapeHtml(student.companyLogoUrl) + '" alt="' + escapeHtml(student.companyName) + ' logo">';
        }
        return '<div class="student-company-logo"></div>';
    }

    function getPhotoMarkup(student) {
        if (student.photoUrl) {
            return '<img class="student-photo" src="' + escapeHtml(student.photoUrl) + '" alt="' + escapeHtml(student.studentName) + ' photo">';
        }
        return '<div class="student-photo"></div>';
    }

    function populateFilters(students) {
        populateSelect("selectedDriveFilter", students.map(function (student) {
            return student.companyName + " - " + student.driveTitle + " (" + student.hiringYear + ")";
        }));
        populateSelect("selectedBranchFilter", students.map(function (student) { return student.branch; }));
        populateSelect("selectedSectionFilter", students.map(function (student) { return student.section; }));
        populateSelect("selectedGenderFilter", students.map(function (student) { return student.gender; }));
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

    function renderStudents(students) {
        const loadingElement = document.getElementById("studentSelectedStudentsLoading");
        const list = document.getElementById("studentSelectedStudentsList");
        const emptyState = document.getElementById("studentSelectedStudentsEmpty");

        loadingElement.classList.add("hidden");
        list.innerHTML = "";

        if (!students.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");

        students.forEach(function (student) {
            const driveLabel = student.companyName + " - " + student.driveTitle + " (" + student.hiringYear + ")";

            const card = document.createElement("article");
            card.className = "student-selected-card";
            card.innerHTML = [
                '<div class="student-selected-header">',
                getCompanyLogoMarkup(student),
                "<div>",
                "<h3>" + escapeHtml(student.companyName) + "</h3>",
                "<p>" + escapeHtml(student.driveTitle) + " | " + escapeHtml(student.hiringYear) + "</p>",
                "</div>",
                "</div>",
                '<div class="student-badge-row">',
                '<span class="offer-badge">' + escapeHtml(student.offerType) + "</span>",
                '<span class="meta-badge">' + escapeHtml(student.packageOffered) + "</span>",
                '<span class="meta-badge">' + escapeHtml(student.roleOffered) + "</span>",
                "</div>",
                '<div class="student-student-row">',
                getPhotoMarkup(student),
                '<div class="student-profile"><h4>' + escapeHtml(student.studentName) + '</h4><p>' + escapeHtml(student.rollNumber) + '</p></div>',
                '</div>',
                '<div class="student-meta">',
                "<span>Drive: " + escapeHtml(driveLabel) + "</span>",
                "<span>Branch: " + escapeHtml(student.branch) + "</span>",
                "<span>Section: " + escapeHtml(student.section) + "</span>",
                "<span>Gender: " + escapeHtml(student.gender) + "</span>",
                "<span>Selection Date: " + formatDate(student.selectionDate) + "</span>",
                "</div>"
            ].join("");
            list.appendChild(card);
        });
    }

    function filterStudents() {
        const searchValue = document.getElementById("selectedSearchInput").value.trim().toLowerCase();
        const driveValue = document.getElementById("selectedDriveFilter").value;
        const branchValue = document.getElementById("selectedBranchFilter").value;
        const sectionValue = document.getElementById("selectedSectionFilter").value;
        const genderValue = document.getElementById("selectedGenderFilter").value;
        const offerTypeValue = document.getElementById("selectedOfferTypeFilter").value;

        const filteredStudents = allStudents.filter(function (student) {
            const driveLabel = student.companyName + " - " + student.driveTitle + " (" + student.hiringYear + ")";
            const matchesSearch = !searchValue
                || student.studentName.toLowerCase().includes(searchValue)
                || student.rollNumber.toLowerCase().includes(searchValue)
                || student.companyName.toLowerCase().includes(searchValue)
                || student.driveTitle.toLowerCase().includes(searchValue);
            const matchesDrive = !driveValue || driveLabel === driveValue;
            const matchesBranch = !branchValue || student.branch === branchValue;
            const matchesSection = !sectionValue || student.section === sectionValue;
            const matchesGender = !genderValue || student.gender === genderValue;
            const matchesOfferType = !offerTypeValue || student.offerType === offerTypeValue;

            return matchesSearch && matchesDrive && matchesBranch && matchesSection && matchesGender && matchesOfferType;
        });

        renderStudents(filteredStudents);
    }

    function setupFilters() {
        [
            "selectedSearchInput",
            "selectedDriveFilter",
            "selectedBranchFilter",
            "selectedSectionFilter",
            "selectedGenderFilter",
            "selectedOfferTypeFilter"
        ].forEach(function (id) {
            document.getElementById(id).addEventListener("input", filterStudents);
            document.getElementById(id).addEventListener("change", filterStudents);
        });
    }

    function showError(message) {
        const loadingElement = document.getElementById("studentSelectedStudentsLoading");
        const emptyState = document.getElementById("studentSelectedStudentsEmpty");

        loadingElement.classList.add("hidden");
        emptyState.textContent = message;
        emptyState.classList.remove("hidden");
    }

    document.addEventListener("DOMContentLoaded", async function () {
        try {
            allStudents = await fetchSelectedStudents();
            populateFilters(allStudents);
            setupFilters();
            renderStudents(allStudents);
        } catch (error) {
            showError(error.message || "Unable to load selected student records right now.");
        }
    });
})();
