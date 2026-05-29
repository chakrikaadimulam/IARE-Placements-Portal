(function () {
    const ACTIVE_NOTICES_API = "/api/student/notices/active";

    async function fetchActiveNotices() {
        const response = await fetch(ACTIVE_NOTICES_API);
        const payload = await response.json().catch(function () {
            return [];
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load notices.");
        }

        return payload;
    }

    function formatDate(dateString) {
        return new Date(dateString).toLocaleDateString("en-IN", {
            year: "numeric",
            month: "short",
            day: "numeric"
        });
    }

    function escapeHtml(value) {
        return value
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function renderStudentNoticesPage(notices) {
        const container = document.getElementById("studentNoticesList");
        const emptyState = document.getElementById("studentNoticesEmpty");
        if (!container || !emptyState) {
            return;
        }

        container.innerHTML = "";
        if (!notices.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");
        notices.forEach(function (notice) {
            const card = document.createElement("article");
            card.className = "student-notice-card";
            card.innerHTML = [
                '<span class="status-badge status-active">Active</span>',
                "<h3>" + escapeHtml(notice.title) + "</h3>",
                "<p>" + escapeHtml(notice.message).replace(/\n/g, "<br>") + "</p>",
                '<div class="notice-meta">',
                "<span>Valid From: " + formatDate(notice.validFrom) + "</span>",
                "<span>Valid To: " + formatDate(notice.validTo) + "</span>",
                "</div>"
            ].join("");
            container.appendChild(card);
        });
    }

    function renderDashboardPreview(notices) {
        const container = document.getElementById("studentDashboardNoticePreview");
        const emptyState = document.getElementById("studentDashboardNoticeEmpty");
        if (!container || !emptyState) {
            return;
        }

        container.innerHTML = "";
        const previewNotices = notices.slice(0, 3);

        if (!previewNotices.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");
        previewNotices.forEach(function (notice) {
            const card = document.createElement("article");
            card.className = "notice-preview-card";
            card.innerHTML = [
                '<span class="status-badge status-active">Active</span>',
                "<h3>" + escapeHtml(notice.title) + "</h3>",
                "<p>" + escapeHtml(notice.message).replace(/\n/g, "<br>") + "</p>",
                '<div class="notice-meta">',
                "<span>Until " + formatDate(notice.validTo) + "</span>",
                "</div>"
            ].join("");
            container.appendChild(card);
        });
    }

    function showStudentError(message) {
        const emptyState = document.getElementById("studentNoticesEmpty");
        const previewEmptyState = document.getElementById("studentDashboardNoticeEmpty");

        if (emptyState) {
            emptyState.textContent = message;
            emptyState.classList.remove("hidden");
        }

        if (previewEmptyState) {
            previewEmptyState.textContent = message;
            previewEmptyState.classList.remove("hidden");
        }
    }

    document.addEventListener("DOMContentLoaded", async function () {
        try {
            const notices = await fetchActiveNotices();
            renderStudentNoticesPage(notices);
            renderDashboardPreview(notices);
        } catch (error) {
            showStudentError(error.message || "Unable to load notices right now.");
        }
    });
})();
