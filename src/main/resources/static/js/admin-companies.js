(function () {
    const API_BASE_URL = "/api/admin/companies";
    const PAGED_API_BASE_URL = "/api/admin/companies/paged";
    const UPLOAD_API_URL = "/api/admin/companies/upload-excel";
    const DEFAULT_EMPTY_MESSAGE = "No companies added yet. Create your first company profile to get started.";
    const PAGE_SIZE = 20;
    const adminCompaniesPageCache = new Map();
    let currentPage = 0;
    let totalPages = 0;
    let isCompaniesLoading = false;
    let activeCompaniesRequest = null;

    async function fetchJson(url, options) {
        const requestOptions = { ...(options || {}) };
        const headers = { ...(requestOptions.headers || {}) };

        if (!(requestOptions.body instanceof FormData) && !headers["Content-Type"]) {
            headers["Content-Type"] = "application/json";
        }

        const response = await fetch(url, {
            ...requestOptions,
            headers
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

    function setUploadFeedback(message, isError) {
        const successElement = document.getElementById("companyUploadSuccess");
        const errorElement = document.getElementById("companyUploadError");
        if (!successElement || !errorElement) {
            return;
        }

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

    function clearUploadFeedback() {
        const successElement = document.getElementById("companyUploadSuccess");
        const errorElement = document.getElementById("companyUploadError");
        if (!successElement || !errorElement) {
            return;
        }

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

    function setUploadLoading(isLoading) {
        const uploadButton = document.getElementById("companyUploadButton");
        if (!uploadButton) {
            return;
        }

        uploadButton.disabled = isLoading;
        uploadButton.textContent = isLoading ? "Uploading..." : "Upload Companies";
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
            return '<img class="company-logo" src="' + escapeHtml(company.logoUrl) + '" alt="' + escapeHtml(company.companyName) + ' logo" loading="lazy" decoding="async" width="64" height="64">';
        }
        return '<div class="company-logo"></div>';
    }

    function setPageLoading(isLoading, initialLoad) {
        const loadingElement = document.getElementById("adminCompaniesLoading");
        const loadingHint = document.getElementById("adminCompaniesPageLoadingHint");
        const list = document.getElementById("adminCompanyList");

        if (loadingElement) {
            loadingElement.classList.toggle("hidden", !(isLoading && initialLoad));
        }
        if (loadingHint) {
            loadingHint.classList.toggle("hidden", !(isLoading && !initialLoad));
        }
        if (list && initialLoad) {
            list.classList.toggle("hidden", isLoading);
        }
    }

    function updatePaginationControls() {
        const prevButton = document.getElementById("adminCompaniesPrevButton");
        const nextButton = document.getElementById("adminCompaniesNextButton");
        const pageInfo = document.getElementById("adminCompaniesPageInfo");

        if (prevButton) {
            prevButton.disabled = isCompaniesLoading || currentPage <= 0;
        }
        if (nextButton) {
            nextButton.disabled = isCompaniesLoading || totalPages === 0 || currentPage >= totalPages - 1;
        }
        if (pageInfo) {
            pageInfo.textContent = "Page " + (totalPages === 0 ? 0 : currentPage + 1) + " of " + totalPages;
        }
    }

    function storePageInCache(page, payload) {
        adminCompaniesPageCache.set(Number(page) || 0, payload);
    }

    function clearCompaniesCache() {
        adminCompaniesPageCache.clear();
    }

    function getCachedPage(page) {
        return adminCompaniesPageCache.get(Number(page) || 0) || null;
    }

    async function fetchCompanyPage(page, signal) {
        return fetchJson(PAGED_API_BASE_URL + "?page=" + page + "&size=" + PAGE_SIZE, {
            method: "GET",
            signal
        });
    }

    async function prefetchCompanyPage(page) {
        const safePage = Number(page) || 0;
        if (safePage < 0 || safePage >= totalPages || getCachedPage(safePage)) {
            return;
        }

        const controller = new AbortController();
        try {
            const payload = await fetchCompanyPage(safePage, controller.signal);
            storePageInCache(safePage, payload);
        } catch (error) {
            console.error("Failed to prefetch admin companies page:", safePage, error);
        }
    }

    function renderCompanyList(companies) {
        const list = document.getElementById("adminCompanyList");
        const emptyState = document.getElementById("adminCompanyEmpty");
        if (!list || !emptyState) {
            return;
        }

        if (!companies.length) {
            list.innerHTML = "";
            emptyState.classList.remove("hidden");
            return;
        }

        emptyState.classList.add("hidden");

        const markup = companies.map(function (company) {
            const websiteMarkup = company.websiteUrl
                ? '<a class="company-link" href="' + escapeHtml(company.websiteUrl) + '" target="_blank" rel="noopener noreferrer">Visit Website</a>'
                : '<span class="company-link">Website not provided</span>';

            return [
                '<article class="admin-company-item">',
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
                (company.createdAt ? "<span>Created: " + formatDate(company.createdAt) + "</span>" : ""),
                "</div>",
                '<p class="company-description">' + escapeHtml(company.description || "").replace(/\n/g, "<br>") + "</p>",
                '<div class="company-item-actions">',
                '<button class="mini-btn toggle-company-btn" type="button" data-company-id="' + company.id + '" data-active="' + company.active + '">'
                    + (company.active ? "Disable" : "Enable") + "</button>",
                '<button class="mini-btn edit-company-btn" type="button" data-company-json="' + encodeURIComponent(JSON.stringify(company)) + '">Edit</button>',
                '<button class="mini-btn delete-btn" type="button" data-company-id="' + company.id + '">Delete</button>',
                "</div>",
                "</article>"
            ].join("");
        }).join("");

        list.innerHTML = markup;
        bindCompanyActions();
    }

    async function loadCompanies(page, options) {
        const emptyState = document.getElementById("adminCompanyEmpty");
        const safePage = Math.max(0, Number(page) || 0);
        const initialLoad = Boolean(options && options.initialLoad);

        if (isCompaniesLoading && !(options && options.force)) {
            return;
        }

        if (emptyState) {
            emptyState.classList.add("hidden");
            emptyState.textContent = DEFAULT_EMPTY_MESSAGE;
        }

        const cachedPage = getCachedPage(safePage);
        if (cachedPage) {
            currentPage = Number(cachedPage.page) || 0;
            totalPages = Number(cachedPage.totalPages) || 0;
            updatePaginationControls();
            renderCompanyList(Array.isArray(cachedPage.content) ? cachedPage.content : []);
            void prefetchCompanyPage(currentPage + 1);
            return;
        }

        if (activeCompaniesRequest) {
            activeCompaniesRequest.abort();
        }

        activeCompaniesRequest = new AbortController();
        isCompaniesLoading = true;
        updatePaginationControls();
        setPageLoading(true, initialLoad);
        console.time("admin-companies-page-load");

        try {
            const response = await fetchCompanyPage(safePage, activeCompaniesRequest.signal);
            const companies = Array.isArray(response && response.content) ? response.content : [];
            currentPage = Number(response && response.page) || 0;
            totalPages = Number(response && response.totalPages) || 0;
            storePageInCache(currentPage, response);
            updatePaginationControls();

            if (!companies.length && currentPage > 0 && totalPages > 0) {
                await loadCompanies(totalPages - 1, { force: true, initialLoad: false });
                return;
            }

            renderCompanyList(companies);
            void prefetchCompanyPage(currentPage + 1);
        } catch (error) {
            if (error && error.name === "AbortError") {
                return;
            }
            console.error("Failed to load admin companies page:", error);
            if (emptyState) {
                emptyState.textContent = error.message || "Unable to load companies. Please try again.";
                emptyState.classList.remove("hidden");
            }
        } finally {
            console.timeEnd("admin-companies-page-load");
            isCompaniesLoading = false;
            activeCompaniesRequest = null;
            updatePaginationControls();
            setPageLoading(false, initialLoad);
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
                    clearCompaniesCache();
                    setFeedback("Company deleted successfully.", false);
                    await loadCompanies(currentPage, { initialLoad: false });
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
                    clearCompaniesCache();
                    setFeedback("Company status updated successfully.", false);
                    await loadCompanies(currentPage, { initialLoad: false });
                } catch (error) {
                    setFeedback(error.message || "Unable to update company status.", true);
                }
            });
        });
    }

    function setUploadSummary(summary) {
        const summaryCard = document.getElementById("companyUploadSummary");
        const totalRows = document.getElementById("companySummaryTotalRows");
        const inserted = document.getElementById("companySummaryInserted");
        const updated = document.getElementById("companySummaryUpdated");
        const skipped = document.getElementById("companySummarySkipped");
        const errorList = document.getElementById("companyUploadErrors");

        if (!summaryCard || !totalRows || !inserted || !updated || !skipped || !errorList) {
            return;
        }

        totalRows.textContent = String(summary.totalRows || 0);
        inserted.textContent = String(summary.insertedCount || 0);
        updated.textContent = String(summary.updatedCount || 0);
        skipped.textContent = String(summary.skippedCount || 0);

        errorList.innerHTML = "";
        (summary.errors || []).forEach(function (error) {
            const item = document.createElement("li");
            item.textContent = "Row " + error.rowNumber + ": " + error.reason;
            errorList.appendChild(item);
        });

        if (!(summary.errors || []).length) {
            const item = document.createElement("li");
            item.textContent = "No row errors.";
            errorList.appendChild(item);
        }

        summaryCard.classList.remove("hidden");
    }

    function buildUploadErrorMessage(payload) {
        if (!payload) {
            return "Unable to upload company Excel.";
        }

        if (payload.message && payload.error) {
            return payload.message + ": " + payload.error;
        }

        return payload.message || payload.error || "Unable to upload company Excel.";
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
                clearCompaniesCache();
                setFeedback(editingCompanyId ? "Company updated successfully." : "Company created successfully.", false);
                await loadCompanies(currentPage, { initialLoad: false });
            } catch (error) {
                setFeedback(error.message || "Unable to save company.", true);
            } finally {
                setLoading(false);
            }
        });
    }

    function setupUpload() {
        const form = document.getElementById("companyUploadForm");
        const fileInput = document.getElementById("companyExcelFile");
        const fileName = document.getElementById("companyUploadFileName");

        if (!form || !fileInput || !fileName) {
            return;
        }

        fileInput.addEventListener("change", function () {
            fileName.textContent = fileInput.files && fileInput.files[0] ? fileInput.files[0].name : "No file selected";
        });

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            if (!fileInput.files || !fileInput.files[0]) {
                setUploadFeedback("Please select an Excel file first.", true);
                return;
            }

            clearUploadFeedback();
            setUploadLoading(true);

            try {
                const formData = new FormData();
                formData.append("file", fileInput.files[0]);

                const response = await fetch(UPLOAD_API_URL, {
                    method: "POST",
                    body: formData
                });

                const payload = await response.json().catch(function () {
                    return null;
                });

                if (!response.ok) {
                    throw new Error(buildUploadErrorMessage(payload));
                }

                setUploadSummary(payload || {});
                setUploadFeedback("Companies uploaded successfully.", false);
                form.reset();
                fileName.textContent = "No file selected";
                clearCompaniesCache();
                await loadCompanies(currentPage, { initialLoad: false });
            } catch (error) {
                setUploadFeedback(error.message || "Unable to upload company Excel.", true);
            } finally {
                setUploadLoading(false);
            }
        });
    }

    function setupPagination() {
        const prevButton = document.getElementById("adminCompaniesPrevButton");
        const nextButton = document.getElementById("adminCompaniesNextButton");

        if (prevButton) {
            prevButton.addEventListener("click", function () {
                if (currentPage > 0) {
                    loadCompanies(currentPage - 1);
                }
            });
        }

        if (nextButton) {
            nextButton.addEventListener("click", function () {
                if (currentPage < totalPages - 1) {
                    loadCompanies(currentPage + 1);
                }
            });
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        if (!document.getElementById("adminCompanyForm")) {
            return;
        }

        setupForm();
        setupUpload();
        setupPagination();
        updatePaginationControls();
        loadCompanies(0, { initialLoad: true });
    });
})();
