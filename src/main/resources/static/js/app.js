"use strict";

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("form[data-confirm], form[data-loading]").forEach((form) => {
    form.addEventListener("submit", (event) => {
      if (form.dataset.confirm && !window.confirm(form.dataset.confirm)) {
        event.preventDefault();
        return;
      }
      if (form.dataset.loading !== undefined) setSubmitting(form);
    });
  });
});

function setSubmitting(form) {
  const button = form.querySelector("button[type='submit']");
  if (!button || button.disabled) return;
  button.dataset.originalLabel = button.textContent.trim();
  button.textContent = button.dataset.loadingLabel || "Please wait…";
  button.disabled = true;
  button.setAttribute("aria-busy", "true");
}
