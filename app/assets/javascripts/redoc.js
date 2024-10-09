export function onDomLoaded() {
    const elRedocContainer = document.getElementById("redocContainer"),
        options = {
            hideHostname: false,
            theme: {
                spacing: {
                    sectionVertical: '35px',
                },
                typography: {
                    headings: {
                        lineHeight: "1.2em"
                    }
                }
            }
        },
        oasUrl = elRedocContainer.dataset.oasurl;

    Redoc.init(oasUrl, options, elRedocContainer);
}

if (typeof window !== 'undefined') {
    window.addEventListener("DOMContentLoaded", onDomLoaded);
}
