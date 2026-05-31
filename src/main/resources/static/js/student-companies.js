(function () {
    const COMPANIES_API = "/api/companies";
    const PAGE_SIZE = 20;
    const companiesPageCache = new Map();
    let currentPage = 0;
    let totalPages = 0;
    let totalElements = 0;
    let currentPageCompanies = [];
    let isCompaniesLoading = false;
    let activeCompaniesRequest = null;

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function safeText(value, fallback) {
        const normalized = String(value == null ? "" : value).trim();
        return normalized || fallback;
    }

    function buildFallbackLogo(companyName) {
        return '<div class="company-logo text-logo"><span>' + escapeHtml(safeText(companyName, "C").charAt(0).toUpperCase()) + "</span></div>";
    }

    async function fetchCompanies(page, signal) {
        const safePage = Math.max(0, Number(page) || 0);
        const url = COMPANIES_API + "?page=" + safePage + "&size=" + PAGE_SIZE;
        const response = await fetch(url, {
            method: "GET",
            signal,
            headers: {
                Accept: "application/json"
            }
        });

        const payload = await response.json().catch(function () {
            return null;
        });

        if (!response.ok) {
            throw new Error(payload && payload.message ? payload.message : "Unable to load companies. Please try again.");
        }

        if (!payload || !Array.isArray(payload.content)) {
            throw new Error("Invalid companies response received.");
        }

        return payload;
    }

    function populateSelect(id, values, defaultLabel) {
        const select = document.getElementById(id);
        if (!select) return;

        const uniqueValues = Array.from(new Set(values.filter(Boolean))).sort();
        select.innerHTML = '<option value="">' + defaultLabel + "</option>";

        uniqueValues.forEach(function (value) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            select.appendChild(option);
        });
    }

    function populateFilters(companies) {
        populateSelect("typeFilter", companies.map(function (company) {
            return safeText(company.companyType, "");
        }), "All Types");

        populateSelect("industryFilter", companies.map(function (company) {
            return safeText(company.industry, "");
        }), "All Industries");
    }

    function getLogoMarkup(company) {
        const companyName = safeText(company.companyName, "C");

        if (company.logoUrl) {
            return [
                '<div class="company-logo">',
                '<img src="', escapeHtml(company.logoUrl),
                '" alt="', escapeHtml(companyName), ' logo" loading="lazy" decoding="async" width="60" height="60"',
                ' onerror="this.closest(\'.company-logo\').outerHTML=', "'",
                buildFallbackLogo(companyName).replace(/'/g, "\\'"),
                "'", ';">',
                '</div>'
            ].join("");
        }

        return buildFallbackLogo(companyName);
    }

    function buildSearchText(company) {
        return [
            safeText(company.companyName, ""),
            safeText(company.companyType, ""),
            safeText(company.industry, ""),
            safeText(company.headquarters, "")
        ].join(" ").toLowerCase();
    }

    function updateCompanyCount(count) {
        const companyCount = document.getElementById("companyCount");
        if (companyCount) {
            companyCount.textContent = String(count);
        }
    }

    function updatePaginationControls() {
        const prevButton = document.getElementById("companiesPrevButton");
        const nextButton = document.getElementById("companiesNextButton");
        const pageInfo = document.getElementById("companiesPageInfo");

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

    function setListLoading(isLoading, initialLoad) {
        const loadingElement = document.getElementById("studentCompaniesLoading");
        const list = document.getElementById("companiesGrid");
        const loadingHint = document.getElementById("companiesPageLoadingHint");
        if (loadingElement) {
            loadingElement.classList.toggle("hidden", !(isLoading && initialLoad));
        }
        if (list) {
            list.classList.toggle("hidden", isLoading && initialLoad);
        }
        if (loadingHint) {
            loadingHint.classList.toggle("hidden", !(isLoading && !initialLoad));
        }
    }

    function renderCompanies(companies) {
        const loadingElement = document.getElementById("studentCompaniesLoading");
        const list = document.getElementById("companiesGrid");
        const emptyState = document.getElementById("studentCompaniesEmpty");

        if (loadingElement) {
            loadingElement.classList.add("hidden");
        }
        if (!list || !emptyState) {
            return;
        }

        let markup = "";
        updateCompanyCount(companies.length);
        updatePaginationControls();

        if (!companies.length) {
            list.classList.add("hidden");
            emptyState.classList.remove("hidden");
            emptyState.innerHTML = [
                '<i data-lucide="building-2"></i>',
                '<h3>No companies available yet.</h3>',
                '<p>Try again later or check another page.</p>'
            ].join("");
            initLucideIcons();
            return;
        }

        list.classList.remove("hidden");
        emptyState.classList.add("hidden");

        companies.forEach(function (company) {
            const companyName = safeText(company.companyName, "N/A");
            const companyType = safeText(company.companyType, "N/A");
            const industry = safeText(company.industry, "N/A");
            const description = safeText(company.description, "N/A");
            const headquarters = safeText(company.headquarters, "N/A");
            const foundedYear = safeText(company.foundedYear, "N/A");

            markup += [
                '<article class="company-card card">',
                '<div class="company-header">',
                getLogoMarkup(company),
                '<div class="company-titles">',
                "<h3>", escapeHtml(companyName), "</h3>",
                '<div class="card-badges">',
                '<span class="badge badge-orange">', escapeHtml(companyType), "</span>",
                '<span class="badge badge-teal">', escapeHtml(industry), "</span>",
                "</div>",
                "</div>",
                "</div>",
                '<div class="company-description"><p>', escapeHtml(description), "</p></div>",
                '<div class="company-meta">',
                '<span class="meta-item">HQ: ', escapeHtml(headquarters), "</span>",
                '<span class="meta-item">Founded: ', escapeHtml(foundedYear), "</span>",
                "</div>",
                company.websiteUrl
                    ? '<a href="' + escapeHtml(company.websiteUrl) + '" target="_blank" rel="noopener noreferrer" class="btn-visit ripple-container">Visit Website</a>'
                    : '<span class="btn-visit" aria-disabled="true">Visit Website</span>',
                '</article>'
            ].join("");
        });
        list.innerHTML = markup;

        initCardObserver();
        initLucideIcons();
    }

    function filterCompanies() {
        const searchInput = document.getElementById("searchInput");
        const typeFilter = document.getElementById("typeFilter");
        const industryFilter = document.getElementById("industryFilter");
        const searchTerm = searchInput ? searchInput.value.trim().toLowerCase() : "";
        const typeValue = typeFilter ? typeFilter.value : "";
        const industryValue = industryFilter ? industryFilter.value : "";

        const filteredCompanies = currentPageCompanies.filter(function (company) {
            const matchesSearch = !searchTerm || buildSearchText(company).includes(searchTerm);
            const matchesType = !typeValue || safeText(company.companyType, "N/A") === typeValue;
            const matchesIndustry = !industryValue || safeText(company.industry, "N/A") === industryValue;

            return matchesSearch && matchesType && matchesIndustry;
        });

        renderCompanies(filteredCompanies);
    }

    function showError(message) {
        const loadingElement = document.getElementById("studentCompaniesLoading");
        const emptyState = document.getElementById("studentCompaniesEmpty");
        const list = document.getElementById("companiesGrid");

        if (loadingElement) {
            loadingElement.classList.add("hidden");
        }
        if (list) {
            list.classList.add("hidden");
        }
        if (emptyState) {
            emptyState.innerHTML = [
                '<i data-lucide="building-2"></i>',
                '<h3>No companies found</h3>',
                '<p>', escapeHtml(message || "Unable to load companies. Please try again."), '</p>'
            ].join("");
            emptyState.classList.remove("hidden");
        }

        updateCompanyCount(0);
        initLucideIcons();
    }

    function setupFilters() {
        const searchInput = document.getElementById("searchInput");
        const typeFilter = document.getElementById("typeFilter");
        const industryFilter = document.getElementById("industryFilter");

        if (searchInput) searchInput.addEventListener("input", filterCompanies);
        if (typeFilter) typeFilter.addEventListener("change", filterCompanies);
        if (industryFilter) industryFilter.addEventListener("change", filterCompanies);
    }

    function bindRippleEffects() {
        const list = document.getElementById("companiesGrid");
        if (!list || list.dataset.rippleBound === "true") {
            return;
        }

        list.addEventListener("click", function (event) {
            const rippleTarget = event.target.closest(".ripple-container");
            if (rippleTarget) {
                addRipple(event, rippleTarget);
            }
        });
        list.dataset.rippleBound = "true";
    }

    function addRipple(event, element) {
        const ripple = document.createElement("span");
        ripple.className = "ripple";
        const rect = element.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        ripple.style.width = size + "px";
        ripple.style.height = size + "px";
        ripple.style.left = event.clientX - rect.left - size / 2 + "px";
        ripple.style.top = event.clientY - rect.top - size / 2 + "px";
        element.appendChild(ripple);
        setTimeout(function () {
            ripple.remove();
        }, 600);
    }

    function initScrollProgress() {
        const scrollProgress = document.getElementById("scrollProgress");
        if (!scrollProgress) return;

        window.addEventListener("scroll", function () {
            const winScroll = document.body.scrollTop || document.documentElement.scrollTop;
            const height = document.documentElement.scrollHeight - document.documentElement.clientHeight;
            const scrolled = height > 0 ? (winScroll / height) * 100 : 0;
            scrollProgress.style.width = scrolled + "%";
        }, { passive: true });
    }

    let observer = null;

    function initCardObserver() {
        const cards = document.querySelectorAll(".company-card");

        if (!("IntersectionObserver" in window)) {
            cards.forEach(function (card) {
                card.classList.add("visible");
            });
            return;
        }

        if (!observer) {
            observer = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        entry.target.classList.add("visible");
                    } else {
                        entry.target.classList.remove("visible");
                    }
                });
            }, {
                threshold: 0.05,
                rootMargin: "0px 0px -30px 0px"
            });
        }

        cards.forEach(function (card) {
            observer.unobserve(card);
            observer.observe(card);
        });
    }

    function setupBackButton() {
        const backButton = document.getElementById("companiesBackBtn");
        if (!backButton) return;

        const params = new URLSearchParams(window.location.search);
        if (params.get("from") === "dashboard-companies-card") {
            backButton.href = "/student-dashboard#companies-card";
        }
    }

    function initLucideIcons() {
        if (window.lucide && typeof window.lucide.createIcons === "function") {
            window.lucide.createIcons();
        }
    }

    function storePageInCache(page, payload) {
        companiesPageCache.set(Number(page) || 0, payload);
    }

    function getCachedPage(page) {
        return companiesPageCache.get(Number(page) || 0) || null;
    }

    async function prefetchCompaniesPage(page) {
        const safePage = Number(page) || 0;
        if (safePage < 0 || safePage >= totalPages || getCachedPage(safePage)) {
            return;
        }

        const controller = new AbortController();
        try {
            const payload = await fetchCompanies(safePage, controller.signal);
            storePageInCache(safePage, payload);
        } catch (error) {
            console.error("Failed to prefetch companies page:", safePage, error);
        }
    }

    function applyPagePayload(payload) {
        currentPage = Number(payload.page) || 0;
        totalPages = Number(payload.totalPages) || 0;
        totalElements = Number(payload.totalElements) || 0;
        currentPageCompanies = Array.isArray(payload.content) ? payload.content : [];

        populateFilters(currentPageCompanies);
        filterCompanies();
        void prefetchCompaniesPage(currentPage + 1);
    }

    async function loadCompanies(page, options) {
        options = options || {};
        const safePage = Math.max(0, Number(page) || 0);
        const initialLoad = Boolean(options.initialLoad);

        if (isCompaniesLoading && !options.force) {
            return;
        }

        const cachedPage = getCachedPage(safePage);
        if (cachedPage) {
            applyPagePayload(cachedPage);
            console.time("companies-page-load");
            console.timeEnd("companies-page-load");
            return;
        }

        if (activeCompaniesRequest) {
            activeCompaniesRequest.abort();
        }

        activeCompaniesRequest = new AbortController();
        isCompaniesLoading = true;
        updatePaginationControls();
        setListLoading(true, initialLoad);
        console.time("companies-page-load");
        try {
            const payload = await fetchCompanies(safePage, activeCompaniesRequest.signal);
            storePageInCache(safePage, payload);
            applyPagePayload(payload);

            if (!currentPageCompanies.length && currentPage > 0 && totalPages > 0) {
                await loadCompanies(totalPages - 1, { force: true });
                return;
            }
        } catch (error) {
            if (error && error.name === "AbortError") {
                return;
            }
            console.error("Failed to load paginated companies:", error);
            totalPages = 0;
            totalElements = 0;
            currentPageCompanies = [];
            updatePaginationControls();
            showError("Unable to load companies. Please try again.");
        } finally {
            console.timeEnd("companies-page-load");
            isCompaniesLoading = false;
            activeCompaniesRequest = null;
            updatePaginationControls();
            setListLoading(false, initialLoad);
        }
    }

    function setupPagination() {
        const prevButton = document.getElementById("companiesPrevButton");
        const nextButton = document.getElementById("companiesNextButton");

        if (prevButton) {
            prevButton.addEventListener("click", function () {
                if (currentPage > 0) {
                    loadCompanies(currentPage - 1, { initialLoad: false });
                }
            });
        }

        if (nextButton) {
            nextButton.addEventListener("click", function () {
                if (currentPage < totalPages - 1) {
                    loadCompanies(currentPage + 1, { initialLoad: false });
                }
            });
        }
    }

    document.addEventListener("DOMContentLoaded", async function () {
        initLucideIcons();
        initScrollProgress();
        setupBackButton();
        bindRippleEffects();
        setupFilters();
        setupPagination();
        updatePaginationControls();

        await loadCompanies(0, { initialLoad: true });
    });
})();
