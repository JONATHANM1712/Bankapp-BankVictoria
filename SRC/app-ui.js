document.addEventListener("DOMContentLoaded", function () {

    const toastMessages = document.querySelectorAll(".toast-message");

    toastMessages.forEach(function (toast) {
        setTimeout(function () {
            toast.classList.add("toast-hide");
        }, 3500);
    });

    const forms = document.querySelectorAll(".form-loading");

    forms.forEach(function (form) {
        form.addEventListener("submit", function () {

            if (!form.checkValidity()) {
                return;
            }

            const overlay = document.getElementById("loadingOverlay");

            if (overlay) {
                overlay.style.display = "flex";
            }
        });
    });

    const validationForms = document.querySelectorAll(".form-validasi");

    validationForms.forEach(function (form) {
        form.addEventListener("submit", function (event) {

            const ktp = document.getElementById("nomorKtp");
            const rekening = document.getElementById("nomorRekening");
            const saldo = document.getElementById("saldo");

            if (ktp && ktp.value.length !== 16) {
                alert("Nomor KTP harus 16 digit.");
                event.preventDefault();
                hideLoading();
                return;
            }

            if (rekening && (rekening.value.length < 8 || rekening.value.length > 10)) {
                alert("Nomor rekening harus 8 sampai 10 digit.");
                event.preventDefault();
                hideLoading();
                return;
            }

            if (saldo && Number(saldo.value) < 0) {
                alert("Saldo tidak boleh negatif.");
                event.preventDefault();
                hideLoading();
            }
        });
    });

    function hideLoading() {
        const overlay = document.getElementById("loadingOverlay");

        if (overlay) {
            overlay.style.display = "none";
        }
    }
});