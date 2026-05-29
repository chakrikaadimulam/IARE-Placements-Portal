(function () {
    const API_BASE_URL = "/api/admin/companies";
    const DEFAULT_EMPTY_MESSAGE = "No companies added yet. Create your first company profile to get started.";

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
        return new Date(dateString).toLocaleDateString("en-IN", {
            year: "numeric",
            month: "short",
            day: "numeric"
        });
    }

    function setFeedback(message, isError) {
        const successElement = document.getElementById("adminCompanySuccess");
        const errorElement = document.getElementById("adminCompanyError");

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
        const successElement = document.getElementById("adminCompanySuccess");
        const errorElement = document.getElementById("adminCompanyError");

        successElement.textContent = "";
        successElement.classList.add("hidden");
        errorElement.textContent = "";
    }

    function setLoading(isLoading) {
        const submitButton = document.getElementById("submitCompanyButton");
        if (!submitButton) {
            return;
        }

        submitButton.disabled = isLoading;
        submitButton.textContent = isLoading
            ? "Saving..."
            : (document.getElementById("editingCompanyId").value ? "Update Company" : "Add Company");
    }

    function resetForm() {
        const form = document.getElementById("adminCompanyForm");
        form.reset();
        document.getElementById("editingCompanyId").value = "";
        document.getElementById("submitCompanyButton").textContent = "Add Company";
        document.getElementById("submitCompanyButton").disabled = false;
        document.getElementById("cancelCompanyEditButton").classList.add("hidden");
        clearFeedback();
    }

    function populateForm(company) {
        document.getElementById("editingCompanyId").value = String(company.id);
        document.getElementById("companyName").value = company.companyName || "";
        document.getElementById("logoUrl").value = company.logoUrl || "";
        document.getElementById("websiteUrl").value = company.websiteUrl || "";
        document.getElementById("companyType").value = company.companyType || "";
        document.getElementById("industry").value = company.industry || "";
        document.getElementById("headquarters").value = company.headquarters || "";
        document.getElementById("foundedYear").value = company.foundedYear || "";
        document.getElementById("description").value = company.description || "";
        document.getElementById("submitCompanyButton").textContent = "Update Company";
        document.getElementById("cancelCompanyEditButton").classList.remove("hidden");
        clearFeedback();
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function buildPayload(form) {
        return {
            companyName: form.elements.companyName.value.trim(),
            logoUrl: form.elements.logoUrl.value.trim(),
            websiteUrl: form.elements.websiteUrl.value.trim(),
            companyType: form.elements.companyType.value,
            industry: form.elements.industry.value,
            headquarters: form.elements.headquarters.value.trim(),
            foundedYear: form.elements.foundedYear.value ? Number(form.elements.foundedYear.value) : null,
            description: form.elements.description.value.trim()
        };
    }

    function validatePayload(payload) {
        const currentYear = new Date().getFullYear();

        if (!payload.companyName || !payload.companyType || !payload.industry || !payload.description) {
            return "Company Name, Company Type, Industry, and Company Description are required.";
        }
        if (payload.foundedYear && payload.foundedYear > currentYear) {
            return "Founded Year cannot be in the future.";
        }
        return "";
    }

    function getStatusBadge(active) {
        return active
            ? '<span class="status-badge status-active">Active</span>'
            : '<span class="status-badge status-disabled">Disabled</span>';
    }

    function getLogoMarkup(company) {
        if (company.logoUrl) {
            return '<img class="company-logo" src="' + escapeHtml(company.logoUrl) + '" alt="' + escapeHtml(company.companyName) + ' logo">';
        }
        return '<div class="company-logo"></div>';
    }

    async function loadCompanies() {
        const loadingElement = document.getElementById("adminCompaniesLoading");
        const list = document.getElementById("adminCompanyList");
        const emptyState = document.getElementById("adminCompanyEmpty");

        loadingElement.classList.remove("hidden");
        list.innerHTML = "";
        emptyState.classList.add("hidden");
        emptyState.textContent = DEFAULT_EMPTY_MESSAGE;

        try {
            const companies = await fetchJson(API_BASE_URL, { method: "GET" });
            loadingElement.classList.add("hidden");

            if (!companies.length) {
                emptyState.classList.remove("hidden");
                return;
            }

            companies.forEach(function (company) {
                const websiteMarkup = company.websiteUrl
                    ? '<a class="company-link" href="' + escapeHtml(company.websiteUrl) + '" target="_blank" rel="noopener noreferrer">Visit Website</a>'
                    : '<span class="company-link">Website not provided</span>';

                const card = document.createElement("article");
                card.className = "admin-company-item";
                card.innerHTML = [
                    '<div class="company-item-header">',
                    '<div class="company-item-title">',
                    getLogoMarkup(company),
                    "<div>",
                    "<h3>" + escapeHtml(company.companyName) + "</h3>",
                    websiteMarkup,
                    "</div>",
                    "</div>",
                    getStatusBadge(company.active),
                    "</div>",
                    '<div class="company-item-meta">',
                    '<span class="type-badge">' + escapeHtml(company.companyType) + "</span>",
                    '<span class="industry-badge">' + escapeHtml(company.industry) + "</span>",
                    (company.headquarters ? "<span>HQ: " + escapeHtml(company.headquarters) + "</span>" : ""),
                    (company.foundedYear ? "<span>Founded: " + escapeHtml(company.foundedYear) + "</span>" : ""),
                    "<span>Created: " + formatDate(company.createdAt) + "</span>",
                    "</div>",
                    '<p class="company-description">' + escapeHtml(company.description).replace(/\n/g, "<br>") + "</p>",
                    '<div class="company-item-actions">',
                    '<button class="mini-btn toggle-company-btn" type="button" data-company-id="' + company.id + '" data-active="' + company.active + '">'
                        + (company.active ? "Disable" : "Enable") + "</button>",
                    '<button class="mini-btn edit-company-btn" type="button" data-company-json="' + encodeURIComponent(JSON.stringify(company)) + '">Edit</button>',
                    '<button class="mini-btn delete-btn" type="button" data-company-id="' + company.id + '">Delete</button>',
                    "</div>"
                ].join("");
                list.appendChild(card);
            });

            bindCompanyActions();
        } catch (error) {
            loadingElement.classList.add("hidden");
            emptyState.textContent = error.message || "Unable to load companies right now.";
            emptyState.classList.remove("hidden");
        }
    }

    function bindCompanyActions() {
        document.querySelectorAll(".edit-company-btn").forEach(function (button) {
            button.addEventListener("click", function () {
                populateForm(JSON.parse(decodeURIComponent(button.dataset.companyJson)));
            });
        });

        document.querySelectorAll(".delete-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                if (!window.confirm("Delete this company record?")) {
                    return;
                }

                try {
                    clearFeedback();
                    await fetchJson(API_BASE_URL + "/" + button.dataset.companyId, {
                        method: "DELETE"
                    });
                    if (document.getElementById("editingCompanyId").value === button.dataset.companyId) {
                        resetForm();
                    }
                    setFeedback("Company deleted successfully.", false);
                    await loadCompanies();
                } catch (error) {
                    setFeedback(error.message || "Unable to delete company.", true);
                }
            });
        });

        document.querySelectorAll(".toggle-company-btn").forEach(function (button) {
            button.addEventListener("click", async function () {
                const currentState = button.dataset.active === "true";

                try {
                    clearFeedback();
                    await fetchJson(API_BASE_URL + "/" + button.dataset.companyId + "/status?active=" + (!currentState), {
                        method: "PATCH"
                    });
                    setFeedback("Company status updated successfully.", false);
                    await loadCompanies();
                } catch (error) {
                    setFeedback(error.message || "Unable to update company status.", true);
                }
            });
        });
    }

    function setupForm() {
        const form = document.getElementById("adminCompanyForm");
        if (!form) {
            return;
        }

        document.getElementById("cancelCompanyEditButton").addEventListener("click", resetForm);

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const payload = buildPayload(form);
            const validationMessage = validatePayload(payload);
            if (validationMessage) {
                setFeedback(validationMessage, true);
                return;
            }

            const editingCompanyId = document.getElementById("editingCompanyId").value;

            try {
                clearFeedback();
                setLoading(true);
                await fetchJson(editingCompanyId ? API_BASE_URL + "/" + editingCompanyId : API_BASE_URL, {
                    method: editingCompanyId ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                resetForm();
                setFeedback(editingCompanyId ? "Company updated successfully." : "Company created successfully.", false);
                await loadCompanies();
            } catch (error) {
                setFeedback(error.message || "Unable to save company.", true);
            } finally {
                setLoading(false);
            }
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        if (!document.getElementById("adminCompanyForm")) {
            return;
        }

        setupForm();
        loadCompanies();
    });
})();
