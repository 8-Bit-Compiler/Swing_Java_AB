document.querySelectorAll('.notice-close').forEach(button => button.addEventListener('click', () => button.closest('.notice').remove()));
document.querySelectorAll('.auto-submit').forEach(select => select.addEventListener('change', () => select.form.requestSubmit()));
document.querySelectorAll('[data-remove]').forEach(button => button.addEventListener('click', () => { button.form.querySelector('input[name="quantity"]').value = 0; button.form.requestSubmit(); }));
