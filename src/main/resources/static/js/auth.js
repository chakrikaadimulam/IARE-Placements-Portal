(function () {
    const CREDENTIALS = {
        admin: {
            username: "admin",
            password: "admin123",
            redirectUrl: "/admin-dashboard",
            loginUrl: "/admin"
        },
        student: {
            username: "student",
            password: "student123",
            redirectUrl: "/student-dashboard",
            loginUrl: "/student"
        }
    };

    const STORAGE_KEY = "placementPortalAuth";

    function saveAuthState(role, username) {
        sessionStorage.setItem(
            STORAGE_KEY,
            JSON.stringify({
                role: role,
                username: username,
                loginTime: new Date().toISOString()
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

    function setupLogin(formId, role, errorId) {
        const form = document.getElementById(formId);
        if (!form) {
            return;
        }

        const activeSession = getAuthState();
        if (activeSession && activeSession.role === role) {
            window.location.replace(CREDENTIALS[role].redirectUrl);
            return;
        }

        const errorElement = document.getElementById(errorId);

        form.addEventListener("submit", function (event) {
            event.preventDefault();

            const username = form.elements.username.value.trim();
            const password = form.elements.password.value.trim();
            const validUser = CREDENTIALS[role];

            if (username === validUser.username && password === validUser.password) {
                saveAuthState(role, username);
                if (errorElement) {
                    errorElement.textContent = "";
                }
                window.location.assign(validUser.redirectUrl);
                return;
            }

            clearAuthState();
            if (errorElement) {
                errorElement.textContent = "Invalid username or password. Please try again.";
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

    document.addEventListener("DOMContentLoaded", function () {
        setupLogin("adminLoginForm", "admin", "adminError");
        setupLogin("studentLoginForm", "student", "studentError");
        protectDashboard();

        document.querySelectorAll(".logout-trigger").forEach(function (button) {
            button.addEventListener("click", function () {
                logout(button.dataset.logoutRole);
            });
        });
    });
})();
