(function () {
    const CREDENTIALS = {
        admin: {
            username: "admin",
            password: "admin123",
            redirectUrl: "/admin-dashboard",
            loginUrl: "/admin"
        },
        student: {
            redirectUrl: "/student-dashboard",
            loginUrl: "/student",
            apiLoginUrl: "/api/student/auth/login"
        }
    };

    const STORAGE_KEY = "placementPortalAuth";

    function saveAuthState(authState) {
        sessionStorage.setItem(
            STORAGE_KEY,
            JSON.stringify({
                loginTime: new Date().toISOString(),
                ...authState
            })
        );
    }

    function getAuthState() {
        const storedValue = sessionStorage.getItem(STORAGE_KEY);
        if (!storedValue) {
            return null;
        }

        try {
            return JSON.parse(storedValue);
        } catch (error) {
            sessionStorage.removeItem(STORAGE_KEY);
            return null;
        }
    }

    function clearAuthState() {
        sessionStorage.removeItem(STORAGE_KEY);
    }

    async function postJson(url, payload) {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const rawText = await response.text();
        let data = null;

        if (rawText) {
            try {
                data = JSON.parse(rawText);
            } catch (error) {
                data = null;
            }
        }

        if (!response.ok) {
            throw new Error(data && data.message ? data.message : (rawText || "Request failed."));
        }

        return data;
    }

    function setError(errorElement, message) {
        if (errorElement) {
            errorElement.textContent = message || "";
        }
    }

    function setupAdminLogin(formId, errorId) {
        const form = document.getElementById(formId);
        if (!form) {
            return;
        }

        const activeSession = getAuthState();
        if (activeSession && activeSession.role === "admin") {
            window.location.replace(CREDENTIALS.admin.redirectUrl);
            return;
        }

        const errorElement = document.getElementById(errorId);

        form.addEventListener("submit", function (event) {
            event.preventDefault();

            const username = form.elements.username.value.trim();
            const password = form.elements.password.value.trim();
            const validUser = CREDENTIALS.admin;

            if (username === validUser.username && password === validUser.password) {
                saveAuthState({
                    role: "admin",
                    username: username
                });
                setError(errorElement, "");
                window.location.assign(validUser.redirectUrl);
                return;
            }

            clearAuthState();
            setError(errorElement, "Invalid username or password. Please try again.");
        });
    }

    function setupStudentLogin(formId, errorId) {
        const form = document.getElementById(formId);
        if (!form) {
            return;
        }

        const activeSession = getAuthState();
        if (activeSession && activeSession.role === "student") {
            window.location.replace(CREDENTIALS.student.redirectUrl);
            return;
        }

        const errorElement = document.getElementById(errorId);

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const submitButton = form.querySelector("button[type='submit']");
            const rollNo = form.elements.username.value.trim();
            const password = form.elements.password.value.trim();

            if (!rollNo || !password) {
                setError(errorElement, "Roll No and DOB password are required.");
                return;
            }

            try {
                setError(errorElement, "");
                submitButton.disabled = true;
                submitButton.textContent = "Signing In...";

                const result = await postJson(CREDENTIALS.student.apiLoginUrl, {
                    rollNo: rollNo,
                    password: password
                });

                if (!result || !result.success) {
                    clearAuthState();
                    setError(errorElement, result && result.message ? result.message : "Invalid roll number or password.");
                    return;
                }

                saveAuthState({
                    role: "student",
                    username: result.rollNo,
                    studentId: result.studentId,
                    rollNo: result.rollNo,
                    studentName: result.studentName,
                    branch: result.branch,
                    semester: result.semester,
                    section: result.section,
                    photoUrl: result.photoUrl
                });

                window.location.assign(CREDENTIALS.student.redirectUrl);
            } catch (error) {
                clearAuthState();
                setError(errorElement, error.message || "Unable to login right now.");
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = "Login as Student";
            }
        });
    }

    function protectDashboard() {
        const pageRole = document.body.dataset.portalRole;
        if (!pageRole) {
            return;
        }

        const activeSession = getAuthState();
        if (!activeSession || activeSession.role !== pageRole) {
            window.location.replace(CREDENTIALS[pageRole].loginUrl);
        }
    }

    function logout(role) {
        clearAuthState();
        window.location.assign(CREDENTIALS[role].loginUrl);
    }

    function hydrateSessionDetails() {
        const activeSession = getAuthState();
        if (!activeSession) {
            return;
        }

        document.querySelectorAll("[data-session-student-name]").forEach(function (element) {
            element.textContent = activeSession.studentName || activeSession.rollNo || "Student";
        });
        document.querySelectorAll("[data-session-roll-no]").forEach(function (element) {
            element.textContent = activeSession.rollNo || "-";
        });
    }

    window.PlacementPortalAuth = {
        getAuthState: getAuthState,
        clearAuthState: clearAuthState
    };

    document.addEventListener("DOMContentLoaded", function () {
        setupAdminLogin("adminLoginForm", "adminError");
        setupStudentLogin("studentLoginForm", "studentError");
        protectDashboard();
        hydrateSessionDetails();

        document.querySelectorAll(".logout-trigger").forEach(function (button) {
            button.addEventListener("click", function () {
                logout(button.dataset.logoutRole);
            });
        });
    });
})();
