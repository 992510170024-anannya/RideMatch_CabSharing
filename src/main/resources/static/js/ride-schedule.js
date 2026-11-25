"use strict";

document.addEventListener("DOMContentLoaded", () => {
  const dateInput = document.querySelector("#travelDate");
  const timeInput = document.querySelector("#departureTime");
  const endTimeInput = document.querySelector("#departureTimeEnd");
  const flexible = document.querySelector("#flexibleTime");
  const rangeField = document.querySelector("#timeRangeField");
  if (dateInput && !dateInput.value) {
    const today = new Date();
    dateInput.value = today.getFullYear() + "-" + String(today.getMonth() + 1).padStart(2, "0")
      + "-" + String(today.getDate()).padStart(2, "0");
  }

  document.querySelectorAll("[data-date-choice]").forEach((button) => button.addEventListener("click", () => {
    if (!dateInput) return;
    const date = new Date();
    if (button.dataset.dateChoice === "tomorrow") date.setDate(date.getDate() + 1);
    if (button.dataset.dateChoice === "custom") return dateInput.focus();
    dateInput.value = date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0")
      + "-" + String(date.getDate()).padStart(2, "0");
  }));

  document.querySelectorAll("[data-time-choice]").forEach((button) => button.addEventListener("click", () => {
    if (!timeInput) return;
    if (button.dataset.timeChoice === "now") {
      const now = new Date();
      now.setMinutes(now.getMinutes() + 5);
      timeInput.value = String(now.getHours()).padStart(2, "0") + ":"
        + String(now.getMinutes()).padStart(2, "0");
    } else {
      timeInput.value = button.dataset.timeChoice;
    }
  }));

  if (flexible && rangeField) {
    const updateRange = () => {
      rangeField.hidden = !flexible.checked;
      if (endTimeInput) endTimeInput.required = flexible.checked;
    };
    flexible.addEventListener("change", updateRange);
    if (endTimeInput && endTimeInput.value) flexible.checked = true;
    updateRange();
  }
});
