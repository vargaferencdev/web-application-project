/* ------------------------------------------------------------------
 * app.js - navigation, router and the user-facing pages
 * (catalog, detail, my list, favorites, statistics, login, register).
 * The admin pages are in admin.js.
 * ------------------------------------------------------------------ */

const LIST_STATUSES = ['PLANNING', 'CURRENT', 'COMPLETED', 'PAUSED', 'DROPPED'];
const REACTIONS = {POSITIVE: '👍 Liked', NEUTRAL: '😐 Meh', NEGATIVE: '👎 Disliked'};
const MAX_FAVORITES = 6;

const view = $('#view');

/** Navigates to a hash; if we are already there, just re-renders. */
function go(hash) {
    if (location.hash === hash) {
        route();
    } else {
        location.hash = hash;
    }
}

const selected = (value, current) => String(value) === String(current ?? '') ? ' selected' : '';

/* ------------------------------ navbar ------------------------------ */

function renderNav() {
    const user = Auth.user;
    const links = [['#/', 'Catalog']];
    if (user) {
        links.push(['#/list', 'My list'], ['#/favorites', 'Favorites'], ['#/stats', 'Statistics']);
    }
    if (Auth.isAdmin) {
        links.push(['#/admin/media', 'Admin']);
    }
    $('#nav').innerHTML = `
        <div class="container">
            <a class="navbar-brand fw-bold" href="#/">AniTrack</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navMenu">
                <ul class="navbar-nav me-auto">
                    ${links.map(([href, label]) =>
                        `<li class="nav-item"><a class="nav-link" href="${href}">${label}</a></li>`).join('')}
                </ul>
                ${user
                    ? `<span class="navbar-text me-3">${esc(user.username)}
                           ${Auth.isAdmin ? '<span class="badge text-bg-warning">ADMIN</span>' : ''}</span>
                       <button class="btn btn-outline-secondary btn-sm" data-action="logout">Log out</button>`
                    : `<a class="btn btn-outline-light btn-sm me-2" href="#/login">Log in</a>
                       <a class="btn btn-primary btn-sm" href="#/register">Register</a>`}
            </div>
        </div>`;
}

/* ------------------------------- router ------------------------------ */

const PROTECTED = new Set(['list', 'favorites', 'stats', 'admin']);

async function route() {
    const parts = location.hash.replace(/^#\/?/, '').split('/');
    const page = parts[0] || 'catalog';
    if (PROTECTED.has(page) && !Auth.user) {
        go('#/login');
        return;
    }
    view.innerHTML = '<div class="text-center py-5"><div class="spinner-border"></div></div>';
    try {
        switch (page) {
            case 'catalog': await viewCatalog(); break;
            case 'media': await viewDetail(parts[1]); break;
            case 'list': await viewList(); break;
            case 'favorites': await viewFavorites(); break;
            case 'stats': await viewStats(); break;
            case 'login': viewLogin(); break;
            case 'register': viewRegister(); break;
            case 'admin': await Admin.show(parts[1] || 'media'); break;
            default: view.innerHTML = alertBox('Page not found', 'warning');
        }
    } catch (error) {
        view.innerHTML = alertBox(error.message);
    }
}

window.addEventListener('hashchange', route);

/* ------------------------------ catalog ------------------------------ */

const filters = {title: '', type: '', genreId: '', year: '', page: 0};

async function viewCatalog() {
    const [genres, popular, topRated] = await Promise.all([
        Api.get('/api/genres'),
        Api.get('/api/media/popular?limit=5'),
        Api.get('/api/media/top-rated?minVotes=1&limit=5')
    ]);
    view.innerHTML = `
        <h1 class="h3 mb-3">Catalog</h1>
        <form class="row g-2 mb-4" data-form="filters">
            <div class="col-md-4">
                <input name="title" class="form-control" placeholder="Search by title" value="${esc(filters.title)}">
            </div>
            <div class="col-md-2">
                <select name="type" class="form-select">
                    <option value="">Anime &amp; manga</option>
                    <option value="ANIME"${selected('ANIME', filters.type)}>Anime</option>
                    <option value="MANGA"${selected('MANGA', filters.type)}>Manga</option>
                </select>
            </div>
            <div class="col-md-3">
                <select name="genreId" class="form-select">
                    <option value="">All genres</option>
                    ${genres.map(g => `<option value="${g.id}"${selected(g.id, filters.genreId)}>${esc(g.name)}</option>`).join('')}
                </select>
            </div>
            <div class="col-md-2">
                <input name="year" type="number" class="form-control" placeholder="Year" value="${esc(filters.year)}">
            </div>
            <div class="col-md-1"><button class="btn btn-primary w-100">Go</button></div>
        </form>
        <div id="results"></div>
        <div class="row mt-5 g-4">
            <div class="col-md-6">
                <h2 class="h5">Most popular</h2>
                <ul class="list-group">
                    ${popular.map(p => `<li class="list-group-item d-flex justify-content-between">
                        <a href="#/media/${p.mediaId}">${esc(p.title)}</a>
                        <span class="text-secondary">${p.listCount} lists</span></li>`).join('')
                        || '<li class="list-group-item text-secondary">Nothing yet</li>'}
                </ul>
            </div>
            <div class="col-md-6">
                <h2 class="h5">Top rated</h2>
                <ul class="list-group">
                    ${topRated.map(t => `<li class="list-group-item d-flex justify-content-between">
                        <a href="#/media/${t.mediaId}">${esc(t.title)}</a>
                        <span class="text-secondary">${t.positivePercent}% positive · ${t.votes} votes</span></li>`).join('')
                        || '<li class="list-group-item text-secondary">No ratings yet</li>'}
                </ul>
            </div>
        </div>`;
    await loadResults();
}

async function loadResults() {
    const query = new URLSearchParams({page: filters.page, size: 12});
    for (const key of ['title', 'type', 'genreId', 'year']) {
        if (filters[key]) query.set(key, filters[key]);
    }
    const data = await Api.get('/api/media?' + query);
    const box = $('#results');
    if (!box) return;
    box.innerHTML = data.content.length === 0
        ? '<p class="text-secondary">No titles found.</p>'
        : `<div class="row row-cols-2 row-cols-md-4 row-cols-lg-6 g-3">${data.content.map(mediaCard).join('')}</div>`
          + pager(data, 'page');
}

function mediaCard(m) {
    const size = m.mediaType === 'ANIME'
        ? (m.totalEpisodes ? m.totalEpisodes + ' ep' : '')
        : (m.totalChapters ? m.totalChapters + ' ch' : '');
    return `
        <div class="col">
            <a href="#/media/${m.id}" class="card h-100 text-decoration-none media-card">
                ${coverHtml(m.coverImageUrl, m.title)}
                <div class="card-body p-2">
                    <div class="small text-secondary">${nice(m.mediaType)} · ${nice(m.format)}</div>
                    <div class="fw-semibold">${esc(m.title)}</div>
                    <div class="small text-secondary">${nice(m.status)}${size ? ' · ' + size : ''}</div>
                </div>
            </a>
        </div>`;
}

function pager(data, action) {
    if (data.totalPages <= 1) return '';
    return `
        <nav class="mt-4 d-flex justify-content-center align-items-center gap-3">
            <button class="btn btn-outline-secondary btn-sm" data-action="${action}" data-page="${data.page - 1}"
                    ${data.page === 0 ? 'disabled' : ''}>← Previous</button>
            <span>Page ${data.page + 1} / ${data.totalPages}</span>
            <button class="btn btn-outline-secondary btn-sm" data-action="${action}" data-page="${data.page + 1}"
                    ${data.page + 1 >= data.totalPages ? 'disabled' : ''}>Next →</button>
        </nav>`;
}

Forms.filters = form => {
    for (const key of ['title', 'type', 'genreId', 'year']) {
        filters[key] = form.elements[key].value.trim();
    }
    filters.page = 0;
    loadResults().catch(error => toast(error.message, 'danger'));
};

Actions.page = element => {
    filters.page = Number(element.dataset.page);
    loadResults().catch(error => toast(error.message, 'danger'));
};

/* ------------------------------- detail ------------------------------ */

async function viewDetail(id) {
    const m = await Api.get('/api/media/' + id);
    let myReaction = null, entry = null, isFavorite = false;
    if (Auth.user) {
        const [rating, list, favorites] = await Promise.all([
            Api.get('/api/me/ratings/' + id).catch(() => null),   // 404 = not rated yet
            Api.get('/api/me/list'),
            Api.get('/api/me/favorites')
        ]);
        myReaction = rating ? rating.reaction : null;
        entry = list.find(e => e.mediaId === m.id) || null;
        isFavorite = favorites.some(f => f.mediaId === m.id);
    }

    const facts = [
        ['Type', nice(m.mediaType)], ['Format', nice(m.format)], ['Status', nice(m.status)],
        ['Start', m.startDate], ['End', m.endDate],
        ['Episodes', m.totalEpisodes], ['Chapters', m.totalChapters], ['Volumes', m.totalVolumes],
        ['Episode length', m.durationMinutes ? m.durationMinutes + ' min' : null]
    ].filter(([, value]) => value);

    const r = m.ratings;
    const ratingHtml = r.total === 0
        ? '<p class="text-secondary mb-0">No ratings yet.</p>'
        : `<div class="progress rating-bar mb-1">
               <div class="progress-bar bg-success" style="width:${r.positivePercent}%">${r.positivePercent}%</div>
               <div class="progress-bar bg-secondary" style="width:${r.neutralPercent}%">${r.neutralPercent}%</div>
               <div class="progress-bar bg-danger" style="width:${r.negativePercent}%">${r.negativePercent}%</div>
           </div>
           <div class="small text-secondary">${r.total} votes: ${r.positive} liked · ${r.neutral} meh · ${r.negative} disliked</div>`;

    const userPanel = !Auth.user
        ? '<p class="text-secondary"><a href="#/login">Log in</a> to rate and track this title.</p>'
        : `<div class="mb-3">
               <div class="small text-secondary mb-1">Your rating (click again to remove)</div>
               ${Object.entries(REACTIONS).map(([key, label]) => `
                   <button class="btn btn-sm ${key === myReaction ? 'btn-primary' : 'btn-outline-secondary'}"
                           data-action="rate" data-id="${m.id}" data-reaction="${key}"
                           data-active="${key === myReaction}">${label}</button>`).join(' ')}
           </div>
           <div class="mb-3">
               <div class="small text-secondary mb-1">Your list</div>
               ${entry
                   ? `<span class="badge text-bg-info">${nice(entry.status)}</span>
                      ${entry.progress} / ${entry.total ?? '?'}
                      <a class="ms-2" href="#/list">Edit in My list</a>`
                   : `<div class="input-group" style="max-width: 320px">
                          <select id="addStatus" class="form-select">
                              ${LIST_STATUSES.map(s => `<option value="${s}">${nice(s)}</option>`).join('')}
                          </select>
                          <button class="btn btn-primary" data-action="add-to-list" data-id="${m.id}">Add to my list</button>
                      </div>`}
           </div>
           <button class="btn btn-sm ${isFavorite ? 'btn-warning' : 'btn-outline-warning'}"
                   data-action="toggle-favorite" data-id="${m.id}" data-favorite="${isFavorite}">
               ${isFavorite ? '★ In your favorites' : '☆ Add to favorites'}
           </button>`;

    view.innerHTML = `
        <a href="#/" class="text-decoration-none">← Back to the catalog</a>
        <div class="row g-4 mt-1">
            <div class="col-md-3">${coverHtml(m.coverImageUrl, m.title, 'cover detail-cover')}</div>
            <div class="col-md-9">
                <h1 class="h3">${esc(m.title)}</h1>
                <div class="mb-2">
                    ${m.genres.map(g => `<span class="badge text-bg-primary me-1">${esc(g)}</span>`).join('')}
                    ${m.tags.map(t => `<span class="badge text-bg-secondary me-1">${esc(t)}</span>`).join('')}
                </div>
                <p>${esc(m.description) || '<span class="text-secondary">No description.</span>'}</p>
                <dl class="row mb-4">
                    ${facts.map(([label, value]) =>
                        `<dt class="col-sm-3 col-md-2">${label}</dt><dd class="col-sm-9 col-md-10 mb-1">${esc(value)}</dd>`).join('')}
                </dl>
                <h2 class="h6">Community rating</h2>
                <div class="mb-4">${ratingHtml}</div>
                ${userPanel}
            </div>
        </div>`;
}

Actions.rate = async element => {
    const id = element.dataset.id;
    await attempt(async () => {
        if (element.dataset.active === 'true') {
            await Api.del('/api/me/ratings/' + id);
        } else {
            await Api.put('/api/me/ratings/' + id, {reaction: element.dataset.reaction});
        }
    });
    viewDetail(id).catch(error => toast(error.message, 'danger'));
};

Actions['add-to-list'] = async element => {
    const id = element.dataset.id;
    await attempt(() => Api.post('/api/me/list', {mediaId: Number(id), status: $('#addStatus').value, progress: 0}),
        'Added to your list');
    viewDetail(id).catch(error => toast(error.message, 'danger'));
};

Actions['toggle-favorite'] = async element => {
    const id = element.dataset.id;
    const isFavorite = element.dataset.favorite === 'true';
    await attempt(() => isFavorite
        ? Api.del('/api/me/favorites/' + id)
        : Api.post('/api/me/favorites', {mediaId: Number(id)}));
    viewDetail(id).catch(error => toast(error.message, 'danger'));
};

/* ------------------------------ my list ------------------------------ */

let listTab = 'ALL';

async function viewList() {
    const entries = await Api.get('/api/me/list');
    const shown = listTab === 'ALL' ? entries : entries.filter(e => e.status === listTab);
    const count = status => status === 'ALL' ? entries.length : entries.filter(e => e.status === status).length;

    view.innerHTML = `
        <h1 class="h3 mb-3">My list</h1>
        <ul class="nav nav-pills mb-3">
            ${['ALL', ...LIST_STATUSES].map(s => `
                <li class="nav-item">
                    <button class="nav-link ${s === listTab ? 'active' : ''}" data-action="list-tab" data-tab="${s}">
                        ${s === 'ALL' ? 'All' : nice(s)} <span class="badge text-bg-secondary">${count(s)}</span>
                    </button>
                </li>`).join('')}
        </ul>
        ${shown.length === 0 ? '<p class="text-secondary">Nothing here yet. Find something in the <a href="#/">catalog</a>.</p>' : `
        <div class="table-responsive">
            <table class="table align-middle">
                <thead><tr><th>Title</th><th>Type</th><th>Status</th><th>Progress</th><th></th></tr></thead>
                <tbody>
                    ${shown.map(e => `
                        <tr>
                            <td><a href="#/media/${e.mediaId}">${esc(e.title)}</a></td>
                            <td>${nice(e.mediaType)}</td>
                            <td>
                                <select class="form-select form-select-sm" data-change="entry-status" data-id="${e.id}">
                                    ${LIST_STATUSES.map(s => `<option value="${s}"${selected(s, e.status)}>${nice(s)}</option>`).join('')}
                                </select>
                            </td>
                            <td>
                                <div class="d-flex align-items-center gap-2">
                                    <input type="number" min="0" ${e.total ? `max="${e.total}"` : ''} value="${e.progress}"
                                           class="form-control form-control-sm progress-input"
                                           data-change="entry-progress" data-id="${e.id}">
                                    <span class="text-secondary">/ ${e.total ?? '?'}</span>
                                    <button class="btn btn-outline-secondary btn-sm" data-action="plus-one"
                                            data-id="${e.id}" data-progress="${e.progress}">+1</button>
                                </div>
                            </td>
                            <td class="text-end">
                                <button class="btn btn-outline-danger btn-sm" data-action="delete-entry" data-id="${e.id}">Remove</button>
                            </td>
                        </tr>`).join('')}
                </tbody>
            </table>
        </div>`}`;
}

async function updateEntry(id, body) {
    await attempt(() => Api.patch('/api/me/list/' + id, body), 'Saved');
    viewList().catch(error => toast(error.message, 'danger'));   // show what the server really stored
}

Actions['list-tab'] = element => {
    listTab = element.dataset.tab;
    viewList().catch(error => toast(error.message, 'danger'));
};
Changes['entry-status'] = element => updateEntry(element.dataset.id, {status: element.value});
Changes['entry-progress'] = element => updateEntry(element.dataset.id, {progress: Number(element.value)});
Actions['plus-one'] = element => updateEntry(element.dataset.id, {progress: Number(element.dataset.progress) + 1});
Actions['delete-entry'] = async element => {
    if (!confirm('Remove this title from your list?')) return;
    await attempt(() => Api.del('/api/me/list/' + element.dataset.id), 'Removed');
    viewList().catch(error => toast(error.message, 'danger'));
};

/* ----------------------------- favorites ----------------------------- */

async function viewFavorites() {
    const favorites = await Api.get('/api/me/favorites');
    view.innerHTML = `
        <h1 class="h3 mb-1">Favorites</h1>
        <p class="text-secondary">${favorites.length} / ${MAX_FAVORITES} slots used</p>
        ${favorites.length === 0 ? '<p class="text-secondary">No favorites yet. Open a title and click the star.</p>' : `
        <div class="row row-cols-2 row-cols-md-3 row-cols-lg-6 g-3">
            ${favorites.map(f => `
                <div class="col">
                    <div class="card h-100">
                        <a href="#/media/${f.mediaId}" class="text-decoration-none">${coverHtml(f.coverImageUrl, f.title)}</a>
                        <div class="card-body p-2">
                            <span class="badge text-bg-warning">#${f.position}</span>
                            <div class="fw-semibold">${esc(f.title)}</div>
                            <button class="btn btn-outline-danger btn-sm mt-2" data-action="remove-favorite"
                                    data-id="${f.mediaId}">Remove</button>
                        </div>
                    </div>
                </div>`).join('')}
        </div>`}`;
}

Actions['remove-favorite'] = async element => {
    await attempt(() => Api.del('/api/me/favorites/' + element.dataset.id), 'Removed');
    viewFavorites().catch(error => toast(error.message, 'danger'));
};

/* ----------------------------- statistics ---------------------------- */

async function viewStats() {
    const s = await Api.get('/api/me/statistics');
    const tiles = [
        ['Titles in your list', s.totalTitles],
        ['Anime', s.animeCount],
        ['Manga', s.mangaCount],
        ['Episodes watched', s.episodesWatched],
        ['Chapters read', s.chaptersRead],
        ['Time watched', (s.minutesWatched / 60).toFixed(1) + ' h'],
        ['Estimated days watched', s.estimatedDaysWatched]
    ];
    view.innerHTML = `
        <h1 class="h3 mb-3">Statistics</h1>
        <div class="row row-cols-2 row-cols-md-4 g-3">
            ${tiles.map(([label, value]) => `
                <div class="col"><div class="card text-center h-100"><div class="card-body">
                    <div class="display-6">${esc(value)}</div>
                    <div class="text-secondary">${label}</div>
                </div></div></div>`).join('')}
        </div>`;
}

/* ------------------------- login and register ------------------------ */

function authForm(title, formName, fields, buttonLabel, footer) {
    view.innerHTML = `
        <div class="row justify-content-center"><div class="col-md-5 col-lg-4">
            <h1 class="h3 mb-3">${title}</h1>
            <div id="formError"></div>
            <form data-form="${formName}">
                ${fields.map(([name, label, type, extra = '']) => `
                    <div class="mb-3">
                        <label class="form-label">${label}</label>
                        <input name="${name}" type="${type}" class="form-control" required ${extra}>
                    </div>`).join('')}
                <button class="btn btn-primary w-100">${buttonLabel}</button>
            </form>
            <p class="mt-3 text-secondary">${footer}</p>
        </div></div>`;
}

function viewLogin() {
    authForm('Log in', 'login',
        [['username', 'Username', 'text', 'autocomplete="username"'],
         ['password', 'Password', 'password', 'autocomplete="current-password"']],
        'Log in', 'No account yet? <a href="#/register">Register</a>');
}

function viewRegister() {
    authForm('Create an account', 'register',
        [['username', 'Username', 'text', 'minlength="3" maxlength="30" pattern="[A-Za-z0-9_]+"'],
         ['email', 'Email', 'email'],
         ['password', 'Password (at least 8 characters)', 'password', 'minlength="8" maxlength="72"']],
        'Register', 'Already registered? <a href="#/login">Log in</a>');
}

Forms.login = async form => {
    try {
        // on success the server answers with the user and sets the HttpOnly JWT cookie
        Auth.user = await Api.post('/api/auth/login', {
            username: form.elements.username.value.trim(),
            password: form.elements.password.value
        });
        renderNav();
        go('#/');
    } catch (error) {
        $('#formError').innerHTML = alertBox(error.message);
    }
};

Forms.register = async form => {
    try {
        await Api.post('/api/auth/register', {
            username: form.elements.username.value.trim(),
            email: form.elements.email.value.trim(),
            password: form.elements.password.value
        });
        toast('Account created, you can log in now');
        go('#/login');
    } catch (error) {
        $('#formError').innerHTML = alertBox(error.message);
    }
};

Actions.logout = async () => {
    await attempt(() => Api.post('/api/auth/logout'));
    Auth.user = null;
    renderNav();
    go('#/');
};

/* -------------------------------- start ------------------------------ */

(async () => {
    await Auth.load();
    renderNav();
    route();
})();