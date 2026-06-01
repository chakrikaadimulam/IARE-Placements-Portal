(function () {
    const RESUME_ANALYZE_API = "/api/resume/analyze";
    const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    let selectedFile = null;

    function addRipple(event, element) {
        const ripple = document.createElement("span");
        ripple.className = "ripple";
        const rect = element.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        ripple.style.cssText = "width:" + size + "px;height:" + size + "px;left:" + (event.clientX - rect.left - size / 2) + "px;top:" + (event.clientY - rect.top - size / 2) + "px";
        element.appendChild(ripple);
        ripple.addEventListener("animationend", function () {
            ripple.remove();
        });
    }

    function initScrollProgress() {
        const progress = document.getElementById("scrollProgress");
        if (!progress) {
            return;
        }

        window.addEventListener("scroll", function () {
            const scrolled = document.documentElement.scrollTop;
            const max = document.documentElement.scrollHeight - document.documentElement.clientHeight;
            progress.style.width = max > 0 ? Math.round((scrolled / max) * 100) + "%" : "0%";
        }, { passive: true });
    }

    function showError(message) {
        const errorState = document.getElementById("errorState");
        errorState.textContent = message;
        errorState.classList.add("visible");
    }

    function hideError() {
        document.getElementById("errorState").classList.remove("visible");
    }

    function checkReady() {
        document.getElementById("analyseBtn").disabled = !selectedFile;
    }

    function setStep(stepNumber) {
        [1, 2, 3].forEach(function (index) {
            const dot = document.getElementById("step" + index + "dot");
            const label = document.getElementById("step" + index + "lbl");

            if (index < stepNumber) {
                dot.className = "step-dot done";
                dot.innerHTML = "";
                label.className = "step-label";
            } else if (index === stepNumber) {
                dot.className = "step-dot active";
                dot.innerHTML = "<span>" + index + "</span>";
                label.className = "step-label active";
            } else {
                dot.className = "step-dot";
                dot.innerHTML = "<span>" + index + "</span>";
                label.className = "step-label";
            }
        });

        document.getElementById("line12").classList.toggle("done", stepNumber > 1);
        document.getElementById("line23").classList.toggle("done", stepNumber > 2);
    }

    function formatFileSize(sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        }
        if (sizeInBytes < 1024 * 1024) {
            return (sizeInBytes / 1024).toFixed(1) + " KB";
        }
        return (sizeInBytes / (1024 * 1024)).toFixed(1) + " MB";
    }

    function updateFileUi(file) {
        const fileMeta = document.getElementById("fileMeta");
        const fileName = document.getElementById("fileName");
        const fileSize = document.getElementById("fileSize");
        const dropTitle = document.getElementById("dropTitle");
        const dropZone = document.getElementById("dropZone");

        if (!file) {
            fileMeta.hidden = true;
            dropTitle.textContent = "Drag and drop your PDF resume here";
            dropZone.classList.remove("upload-ready");
            return;
        }

        fileName.textContent = file.name;
        fileSize.textContent = formatFileSize(file.size);
        fileMeta.hidden = false;
        dropTitle.textContent = "PDF resume ready for AI analysis";
        dropZone.classList.add("upload-ready");
    }

    function validateFile(file) {
        if (!file) {
            return "Please upload a PDF resume.";
        }

        const lowerName = String(file.name || "").toLowerCase();
        if (!lowerName.endsWith(".pdf")) {
            return "Please upload a PDF resume.";
        }

        if (file.size > MAX_FILE_SIZE_BYTES) {
            return "Please upload a PDF resume under 5 MB.";
        }

        return "";
    }

    function processFile(file) {
        const validationMessage = validateFile(file);
        if (validationMessage) {
            selectedFile = null;
            updateFileUi(null);
            checkReady();
            showError(validationMessage);
            return;
        }

        selectedFile = file;
        hideError();
        updateFileUi(file);
        checkReady();
    }

    async function analyzeResume() {
        const formData = new FormData();
        formData.append("resume", selectedFile);

        const response = await fetch(RESUME_ANALYZE_API, {
            method: "POST",
            body: formData
        });

        const payload = await response.json().catch(function () {
            return null;
        });

        if (!response.ok) {
            throw new Error(extractErrorMessage(payload));
        }

        return payload;
    }

    function extractErrorMessage(payload) {
        const message = payload && (payload.message || payload.error);
        if (!message) {
            return "AI service unavailable, please try again.";
        }

        if (/pdf/i.test(message)) {
            return "Please upload a PDF resume";
        }
        if (/could not be read/i.test(message)) {
            return "Resume text could not be read";
        }
        if (/AI service unavailable/i.test(message) || /unavailable/i.test(message)) {
            return "AI service unavailable, please try again";
        }

        return message;
    }

    function getScoreGrade(score) {
        if (score >= 85) {
            return { label: "Excellent", className: "excellent", color: "green" };
        }
        if (score >= 70) {
            return { label: "Good", className: "good", color: "green" };
        }
        if (score >= 55) {
            return { label: "Average", className: "average", color: "amber" };
        }
        return { label: "Needs Work", className: "poor", color: "red" };
    }

    function renderList(targetId, items, emptyMessage) {
        const target = document.getElementById(targetId);
        const safeItems = Array.isArray(items) ? items.filter(Boolean) : [];
        target.innerHTML = safeItems.length
            ? safeItems.map(function (item) {
                return "<li>" + escapeHtml(item) + "</li>";
            }).join("")
            : '<li class="empty-item">' + escapeHtml(emptyMessage) + "</li>";
    }

    function renderPills(targetId, items, emptyMessage) {
        const target = document.getElementById(targetId);
        const safeItems = Array.isArray(items) ? items.filter(Boolean) : [];
        target.innerHTML = safeItems.length
            ? safeItems.map(function (item) {
                return '<span class="result-pill">' + escapeHtml(item) + "</span>";
            }).join("")
            : '<span class="empty-pill">' + escapeHtml(emptyMessage) + "</span>";
    }

    function renderResults(result) {
        const score = Number(result.overallScore || 0);
        const scoreGrade = getScoreGrade(score);
        const ring = document.getElementById("scoreRing");
        const circumference = 283;
        const offset = circumference - (score / 100) * circumference;

        ring.style.strokeDashoffset = offset;
        ring.classList.remove("green", "amber", "red");
        ring.classList.add(scoreGrade.color);

        document.getElementById("scoreVal").textContent = String(score);

        const gradeElement = document.getElementById("scoreGrade");
        gradeElement.className = "score-grade " + scoreGrade.className;
        gradeElement.textContent = scoreGrade.label;

        document.getElementById("summaryText").textContent = result.shortSummary || "No summary available.";
        document.getElementById("shortSummaryText").textContent = result.shortSummary || "No summary available.";
        document.getElementById("finalAdviceText").textContent = result.finalAdvice || "No final advice available.";

        renderList("strengthsList", result.strengths, "No strengths provided.");
        renderList("mistakesList", result.mistakes, "No mistakes highlighted.");
        renderPills("missingSkillsList", result.missingSkills, "No missing skills highlighted.");
        renderList("atsSuggestionsList", result.atsSuggestions, "No ATS suggestions provided.");
        renderList("projectImprovementsList", result.projectImprovements, "No project improvements suggested.");
        renderList("grammarFormattingIssuesList", result.grammarFormattingIssues, "No grammar or formatting issues highlighted.");

        document.getElementById("loadingSection").classList.remove("visible");
        document.getElementById("loadingSection").setAttribute("aria-hidden", "true");
        document.getElementById("resultsSection").classList.add("visible");
        setStep(3);
        document.getElementById("resultsSection").scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function resetPage() {
        selectedFile = null;
        document.getElementById("fileInput").value = "";
        document.getElementById("uploadSection").style.display = "block";
        document.getElementById("loadingSection").classList.remove("visible");
        document.getElementById("loadingSection").setAttribute("aria-hidden", "true");
        document.getElementById("resultsSection").classList.remove("visible");
        hideError();
        updateFileUi(null);
        checkReady();
        setStep(1);
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function bindEvents() {
        const dropZone = document.getElementById("dropZone");
        const fileInput = document.getElementById("fileInput");
        const analyseButton = document.getElementById("analyseBtn");
        const exportButton = document.getElementById("exportBtn");
        const resetButton = document.getElementById("resetBtn");

        dropZone.addEventListener("click", function () {
            fileInput.click();
        });

        dropZone.addEventListener("dragover", function (event) {
            event.preventDefault();
            dropZone.classList.add("drag-over");
        });

        dropZone.addEventListener("dragleave", function () {
            dropZone.classList.remove("drag-over");
        });

        dropZone.addEventListener("drop", function (event) {
            event.preventDefault();
            dropZone.classList.remove("drag-over");
            if (event.dataTransfer.files && event.dataTransfer.files[0]) {
                processFile(event.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener("change", function (event) {
            if (event.target.files && event.target.files[0]) {
                processFile(event.target.files[0]);
            }
        });

        analyseButton.addEventListener("click", async function (event) {
            addRipple(event, analyseButton);

            const validationMessage = validateFile(selectedFile);
            if (validationMessage) {
                showError(validationMessage);
                return;
            }

            hideError();
            setStep(2);
            document.getElementById("uploadSection").style.display = "none";
            document.getElementById("resultsSection").classList.remove("visible");
            document.getElementById("loadingSection").classList.add("visible");
            document.getElementById("loadingSection").setAttribute("aria-hidden", "false");

            try {
                const analysis = await analyzeResume();
                renderResults(analysis);
            } catch (error) {
                console.error("Resume analysis failed", error);
                document.getElementById("uploadSection").style.display = "block";
                document.getElementById("loadingSection").classList.remove("visible");
                document.getElementById("loadingSection").setAttribute("aria-hidden", "true");
                setStep(1);
                showError(error.message || "AI service unavailable, please try again");
            }
        });

        exportButton.addEventListener("click", function (event) {
            addRipple(event, exportButton);
            window.print();
        });

        resetButton.addEventListener("click", function (event) {
            addRipple(event, resetButton);
            resetPage();
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        const authState = window.PlacementPortalAuth
            ? window.PlacementPortalAuth.getAuthState()
            : null;

        if (!authState || String(authState.role || "").toLowerCase() !== "student") {
            window.location.replace("/student-login.html");
            return;
        }

        initScrollProgress();
        bindEvents();
        setStep(1);
    });
})();
