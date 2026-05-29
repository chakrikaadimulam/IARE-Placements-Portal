(function () {
    const COMPANIES_API = "/api/student/companies";

    async function fetchCompanies() {
        const response = await fetch(COMPANIES_API);
        const payload = await response.json().catch(function () {
            return [];
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load companies.");
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

    function getLogoMarkup(company) {
        if (company.logoUrl) {
            return '<img class="student-company-logo" src="' + escapeHtml(company.logoUrl) + '" alt="' + escapeHtml(company.companyName) + ' logo">';
        }
        return '<div class="student-company-logo"></div>';
    }

    function renderCompanies(companies) {
        const loadingElement = document.getElementById("studentCompaniesLoading");
        const list = document.getElementById("studentCompaniesList");
        const emptyState = document.getElementById("studentCompaniesEmpty");

        loadingElement.classList.add("hidden");
        list.innerHTML = "";

        if (!companies.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");

        companies.forEach(function (company) {
            const websiteMarkup = company.websiteUrl
                ? '<a class="primary-btn company-website-btn" href="' + escapeHtml(company.websiteUrl) + '" target="_blank" rel="noopener noreferrer">Visit Website</a>'
                : "";

            const card = document.createElement("article");
            card.className = "student-company-card";
            card.innerHTML = [
                '<div class="student-company-header">',
                getLogoMarkup(company),
                "<div>",
                "<h3>" + escapeHtml(company.companyName) + "</h3>",
                '<div class="student-badge-row">',
                '<span class="company-type-badge">' + escapeHtml(company.companyType) + "</span>",
                '<span class="industry-badge">' + escapeHtml(company.industry) + "</span>",
                "</div>",
                "</div>",
                "</div>",
                "<p>" + escapeHtml(company.description).replace(/\n/g, "<br>") + "</p>",
                '<div class="student-company-meta">',
                (company.headquarters ? "<span>HQ: " + escapeHtml(company.headquarters) + "</span>" : ""),
                (company.foundedYear ? "<span>Founded: " + escapeHtml(company.foundedYear) + "</span>" : ""),
                "</div>",
                websiteMarkup
            ].join("");
            list.appendChild(card);
        });
    }

    function showError(message) {
        const loadingElement = document.getElementById("studentCompaniesLoading");
        const emptyState = document.getElementById("studentCompaniesEmpty");

        loadingElement.classList.add("hidden");
        emptyState.textContent = message;
        emptyState.classList.remove("hidden");
    }

    document.addEventListener("DOMContentLoaded", async function () {
        try {
            const companies = await fetchCompanies();
            renderCompanies(companies);
        } catch (error) {
            showError(error.message || "Unable to load companies right now.");
        }
    });
})();
