/* ------------------------------------------------------------------
 * core.js - everything the pages share: HTTP client, login state,
 * small helpers and the event dispatchers.
 * ------------------------------------------------------------------ */

/** Thin wrapper around fetch(). The JWT lives in an HttpOnly cookie, so JavaScript never sees it:
 *  the browser attaches the cookie automatically to same-origin requests. */
const Api = (() => {
    async function request(method, url, body) {
        const options = {method, headers: {}, credentials: 'same-origin'};
        if (body !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        if (response.status === 204) {
            return null;
        }
        const text = await response.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch (e) { /* not JSON */ }
        if (!response.ok) {
            const error = new Error(messageOf(data, response.status));
            error.status = response.status;
            throw error;
        }
        return data;
    }

    // ProblemDetail: "detail" is the message, validation errors come in an "errors" map
    function messageOf(data, status) {
        if (data) {
            if (data.errors && typeof data.errors === 'object') {
                return Object.values(data.errors).join('; ');
            }
            if (data.detail) {
                return data.detail;
            }
        }
        if (status === 401) return 'Please log in first';
        if (status === 403) return 'You do not have permission to do that';
        return 'Request failed (' + status + ')';
    }

    return {
        get: url => request('GET', url),
        post: (url, body) => request('POST', url, body),
        put: (url, body) => request('PUT', url, body),
        patch: (url, body) => request('PATCH', url, body),
        del: url => request('DELETE', url)
    };
})();

/** Who is logged in. The server is the only source of truth: GET /api/auth/me. */
const Auth = {
    user: null,
    async load() {
        try {
            this.user = await Api.get('/api/auth/me');
        } catch (e) {
            this.user = null;      // 401: nobody is logged in
        }
    },
    get isAdmin() {
        return this.user !== null && this.user.role === 'ADMIN';
    }
};

/* ----------------------------- helpers ----------------------------- */

const $ = selector => document.querySelector(selector);

/** Escapes text before it goes into innerHTML (prevents XSS from titles, names...). */
function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, c =>
        ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
}

function toast(message, kind = 'success') {
    const element = document.createElement('div');
    element.className = 'toast align-items-center text-bg-' + kind + ' border-0';
    element.innerHTML = '<div class="d-flex"><div class="toast-body">' + esc(message) +
        '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
    $('#toasts').appendChild(element);
    const instance = new bootstrap.Toast(element, {delay: 3500});
    element.addEventListener('hidden.bs.toast', () => element.remove());
    instance.show();
}

function alertBox(message, kind = 'danger') {
    return '<div class="alert alert-' + kind + '">' + esc(message) + '</div>';
}

function nice(enumValue) {
    if (!enumValue) return '';
    const text = String(enumValue).replace(/_/g, ' ').toLowerCase();
    return text.charAt(0).toUpperCase() + text.slice(1);
}

/** A cover image. Use cssClass 'thumb' for the small version in rankings and lists. */
function coverHtml(url, title, cssClass = 'cover') {
    if (url) {
        return '<img class="' + cssClass + '" loading="lazy" referrerpolicy="no-referrer" src="' + esc(url) +
            '" alt="' + esc(title) + '">';
    }
    return placeholderHtml(title, cssClass.includes('thumb'));
}

function placeholderHtml(title, small = false) {
    return '<div class="' + (small ? 'thumb-placeholder' : 'cover-placeholder') + '">' +
        esc((title || '?').charAt(0).toUpperCase()) + '</div>';
}

// a missing or broken image URL falls back to the placeholder
document.addEventListener('error', event => {
    const image = event.target;
    if (image.tagName === 'IMG' && (image.classList.contains('cover') || image.classList.contains('thumb'))) {
        image.outerHTML = placeholderHtml(image.alt, image.classList.contains('thumb'));
    }
}, true);

/** Runs an async action and shows the error as a red toast. Returns true on success. */
async function attempt(action, successMessage) {
    try {
        await action();
        if (successMessage) toast(successMessage);
        return true;
    } catch (error) {
        if (error.status === 401) {
            Auth.user = null;
            renderNav();
            location.hash = '#/login';
        }
        toast(error.message, 'danger');
        return false;
    }
}

/* ------------------- event delegation (one listener each) ------------------- */
// Pages write  data-action="name"  /  data-form="name"  /  data-change="name"  in their HTML
// and register the handler in these objects. No inline onclick attributes are needed.

const Actions = {};   // click
const Forms = {};     // submit
const Changes = {};   // change

document.addEventListener('click', event => {
    const element = event.target.closest('[data-action]');
    if (element && Actions[element.dataset.action]) {
        event.preventDefault();
        Actions[element.dataset.action](element);
    }
});

document.addEventListener('submit', event => {
    const name = event.target.dataset.form;
    if (name && Forms[name]) {
        event.preventDefault();
        Forms[name](event.target);
    }
});

document.addEventListener('change', event => {
    const element = event.target.closest('[data-change]');
    if (element && Changes[element.dataset.change]) {
        Changes[element.dataset.change](element);
    }
});
