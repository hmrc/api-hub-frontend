export function onDomLoaded() {
    document.getElementById('copyButton').addEventListener('click', function() {
        const emails = this.getAttribute('data-emails');
        navigator.clipboard.writeText(emails).catch(err => {
            console.error('Failed to copy emails to clipboard', err);
        });
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
