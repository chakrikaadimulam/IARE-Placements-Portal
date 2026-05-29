document.addEventListener("DOMContentLoaded", function () {
    const links = document.querySelectorAll(".sidebar-nav a");

    links.forEach(function (link) {
        link.addEventListener("click", function () {
            links.forEach(function (item) {
                item.classList.remove("active");
            });
            link.classList.add("active");
        });
    });
});
