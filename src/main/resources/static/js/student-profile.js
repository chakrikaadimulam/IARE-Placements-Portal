(function () {
    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function initials(name) {
        return String(name || "Student")
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 2)
            .map(function (part) { return part.charAt(0).toUpperCase(); })
            .join("");
    }

    async function fetchJson(url) {
        const response = await fetch(url);
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
            throw new Error(payload && payload.message ? payload.message : (rawText || "Request failed."));
        }

        return payload;
    }

    function buildField(label, value) {
        return [
            '<div class="profile-field">',
            '<span class="field-label">' + escapeHtml(label) + "</span>",
            '<span class="field-value">' + escapeHtml(value || "-") + "</span>",
            "</div>"
        ].join("");
    }

    function renderGrid(elementId, fields) {
        document.getElementById(elementId).innerHTML = fields.map(function (field) {
            return buildField(field[0], field[1]);
        }).join("");
    }

    function renderProfile(student) {
        const profilePhoto = document.getElementById("studentProfilePhoto");
        const profileFallback = document.getElementById("studentProfilePhotoFallback");

        document.getElementById("studentProfileName").textContent = student.studentName || "Student";
        document.getElementById("studentProfileMeta").textContent = [
            student.rollNo || "-",
            student.branch || "-",
            student.semester ? "Semester " + student.semester : "Semester -",
            student.section || "Section -"
        ].join(" | ");
        document.getElementById("studentProfileContact").textContent = [
            student.studentEmailId || "No email",
            student.studentPhone || "No phone"
        ].join(" | ");

        profileFallback.textContent = initials(student.studentName);
        if (student.photoUrl) {
            profilePhoto.src = student.photoUrl;
            profilePhoto.classList.remove("hidden");
            profilePhoto.addEventListener("error", function () {
                profilePhoto.classList.add("hidden");
                profileFallback.classList.remove("hidden");
            }, { once: true });
            profileFallback.classList.add("hidden");
        } else {
            profilePhoto.classList.add("hidden");
            profileFallback.classList.remove("hidden");
        }

        renderGrid("studentAcademicGrid", [
            ["Roll No", student.rollNo],
            ["Branch", student.branch],
            ["Semester", student.semester],
            ["Section", student.section],
            ["Gender", student.gender],
            ["Status", student.status],
            ["DOB", student.dob],
            ["DOJ", student.doj],
            ["Admission Category", student.admissionCategory],
            ["Fee Category", student.feeCategory],
            ["CET Rank", student.cetRank],
            ["SSC Marks", student.sscMarks],
            ["SSC %", student.sscPercentage],
            ["Inter Marks", student.interMarks],
            ["Inter %", student.interPercentage],
            ["UG Marks", student.ugMarks],
            ["UG %", student.ugPercentage]
        ]);

        renderGrid("studentFamilyGrid", [
            ["Student Email", student.studentEmailId],
            ["Student Phone", student.studentPhone],
            ["Parent Phone", student.parentPhone],
            ["Mother Phone", student.motherPhone],
            ["Father Name", student.fatherName],
            ["Mother Name", student.motherName],
            ["Father Occupation", student.fatherOccupation],
            ["Occupation Type", student.occupationType],
            ["Income", student.income],
            ["Aadhar", student.aadhar]
        ]);

        renderGrid("studentAddressGrid", [
            ["Current Address", student.currentAddress],
            ["Permanent Address", student.permanentAddress],
            ["Current State", student.currentState],
            ["Current District", student.currentDistrict],
            ["Current Pincode", student.currentPincode],
            ["Permanent State", student.permanentState],
            ["Permanent District", student.permanentDistrict],
            ["Permanent Pincode", student.permanentPincode],
            ["Religion", student.religion],
            ["Caste", student.caste],
            ["Sub Caste", student.subCaste],
            ["Domicile State", student.domicileState],
            ["SSC State", student.sscState],
            ["Inter State", student.interState],
            ["Place of Birth", student.placeOfBirth],
            ["Moles", student.moles]
        ]);
    }

    document.addEventListener("DOMContentLoaded", async function () {
        const loading = document.getElementById("studentProfileLoading");
        const errorBox = document.getElementById("studentProfileError");
        const content = document.getElementById("studentProfileContent");
        const authState = window.PlacementPortalAuth ? window.PlacementPortalAuth.getAuthState() : null;

        if (!authState || authState.role !== "student") {
            window.location.replace("/student");
            return;
        }

        try {
            const profileUrl = authState.studentId && Number(authState.studentId) > 0
                ? "/api/student/profile/" + encodeURIComponent(authState.studentId)
                : "/api/student/profile/roll/" + encodeURIComponent(authState.rollNo || authState.username || "");
            const student = await fetchJson(profileUrl);
            renderProfile(student);
            loading.classList.add("hidden");
            content.classList.remove("hidden");
        } catch (error) {
            loading.classList.add("hidden");
            errorBox.textContent = error.message || "Unable to load your profile right now.";
            errorBox.classList.remove("hidden");
        }
    });
})();
