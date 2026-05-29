(function () {
    const RESOURCES_API = "/api/student/preparation-resources";
    const PDF_TYPES = [
        { key: "hasAptitudePdf", type: "aptitude", label: "Aptitude Material" },
        { key: "hasCodingPdf", type: "coding", label: "Coding Material" },
        { key: "hasTechnicalPdf", type: "technical", label: "Technical Topics" },
        { key: "hasHrPdf", type: "hr", label: "HR Preparation" }
    ];
    let allResources = [];

    async function fetchResources() {
        const response = await fetch(RESOURCES_API);
        const payload = await response.json().catch(function () {
            return [];
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load preparation resources.");
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

    function getLogoMarkup(resource) {
        if (resource.companyLogoUrl) {
            return '<img class="student-company-logo" src="' + escapeHtml(resource.companyLogoUrl) + '" alt="' + escapeHtml(resource.companyName) + ' logo">';
        }
        return '<div class="student-company-logo"></div>';
    }

    function populateFilters(resources) {
        populateSelect("resourceDriveFilter", resources.map(function (resource) {
            return resource.companyName + " - " + resource.driveTitle + " (" + resource.hiringYear + ")";
        }));
        populateSelect("resourceYearFilter", resources.map(function (resource) {
            return String(resource.hiringYear);
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

    function buildPdfActionUrl(resourceId, type, action) {
        return "/api/student/preparation-resources/" + encodeURIComponent(resourceId) + "/pdf/" + encodeURIComponent(type) + "/" + action;
    }

    function buildPdfSection(resource, pdfType) {
        if (!resource[pdfType.key]) {
            return [
                '<div class="student-pdf-item">',
                '<h4 class="student-pdf-title">' + escapeHtml(pdfType.label) + "</h4>",
                '<span class="resource-link-btn disabled" aria-disabled="true">No PDF Available</span>',
                "</div>"
            ].join("");
        }

        return [
            '<div class="student-pdf-item">',
            '<h4 class="student-pdf-title">' + escapeHtml(pdfType.label) + "</h4>",
            '<div class="student-pdf-actions">',
            '<button class="resource-link-btn" type="button" data-pdf-action="view" data-resource-id="' + escapeHtml(resource.id) + '" data-pdf-type="' + escapeHtml(pdfType.type) + '">View PDF</button>',
            '<button class="resource-link-btn secondary" type="button" data-pdf-action="download" data-resource-id="' + escapeHtml(resource.id) + '" data-pdf-type="' + escapeHtml(pdfType.type) + '">Download PDF</button>',
            "</div>",
            "</div>"
        ].join("");
    }

    function buildPdfButtons(resource) {
        return PDF_TYPES.map(function (pdfType) {
            return buildPdfSection(resource, pdfType);
        }).join("");
    }

    function handlePdfAction(button) {
        const resourceId = button.dataset.resourceId;
        const pdfType = button.dataset.pdfType;
        if (!resourceId || !pdfType) {
            return;
        }

        const pdfActionUrl = buildPdfActionUrl(resourceId, pdfType, button.dataset.pdfAction);

        if (button.dataset.pdfAction === "view") {
            window.open(pdfActionUrl, "_blank", "noopener");
            return;
        }

        window.location.href = pdfActionUrl;
    }

    function bindPdfActions() {
        const list = document.getElementById("studentResourceList");
        list.addEventListener("click", function (event) {
            const button = event.target.closest("[data-pdf-action]");
            if (!button) {
                return;
            }
            handlePdfAction(button);
        });
    }

    function renderResources(resources) {
        const loadingElement = document.getElementById("studentResourceLoading");
        const list = document.getElementById("studentResourceList");
        const emptyState = document.getElementById("studentResourceEmpty");

        loadingElement.classList.add("hidden");
        list.innerHTML = "";

        if (!resources.length) {
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");

        resources.forEach(function (resource) {
            const card = document.createElement("article");
            card.className = "student-resource-card";
            card.innerHTML = [
                '<div class="student-resource-header">',
                getLogoMarkup(resource),
                "<div>",
                "<h3>" + escapeHtml(resource.companyName) + "</h3>",
                "<p>" + escapeHtml(resource.driveTitle) + " | " + escapeHtml(resource.hiringYear) + "</p>",
                "</div>",
                "</div>",
                '<div class="student-resource-meta">',
                '<span class="resource-tag">' + escapeHtml(resource.resourceTitle) + "</span>",
                "</div>",
                (resource.description ? '<p class="resource-description">' + escapeHtml(resource.description).replace(/\n/g, "<br>") + "</p>" : ""),
                '<div class="student-pdf-links">' + buildPdfButtons(resource) + "</div>"
            ].join("");
            list.appendChild(card);
        });
    }

    function filterResources() {
        const searchValue = document.getElementById("resourceSearchInput").value.trim().toLowerCase();
        const driveValue = document.getElementById("resourceDriveFilter").value;
        const yearValue = document.getElementById("resourceYearFilter").value;

        const filteredResources = allResources.filter(function (resource) {
            const driveLabel = resource.companyName + " - " + resource.driveTitle + " (" + resource.hiringYear + ")";
            const matchesSearch = !searchValue
                || resource.companyName.toLowerCase().includes(searchValue)
                || resource.driveTitle.toLowerCase().includes(searchValue)
                || resource.resourceTitle.toLowerCase().includes(searchValue);
            const matchesDrive = !driveValue || driveLabel === driveValue;
            const matchesYear = !yearValue || String(resource.hiringYear) === yearValue;

            return matchesSearch && matchesDrive && matchesYear;
        });

        renderResources(filteredResources);
    }

    function setupFilters() {
        ["resourceSearchInput", "resourceDriveFilter", "resourceYearFilter"].forEach(function (id) {
            document.getElementById(id).addEventListener("input", filterResources);
            document.getElementById(id).addEventListener("change", filterResources);
        });
    }

    function showError(message) {
        const loadingElement = document.getElementById("studentResourceLoading");
        const emptyState = document.getElementById("studentResourceEmpty");

        loadingElement.classList.add("hidden");
        emptyState.textContent = message;
        emptyState.classList.remove("hidden");
    }

    document.addEventListener("DOMContentLoaded", async function () {
        try {
            allResources = await fetchResources();
            populateFilters(allResources);
            setupFilters();
            bindPdfActions();
            renderResources(allResources);
        } catch (error) {
            showError(error.message || "Unable to load preparation resources right now.");
        }
    });
})();
