/* ------------------------------------------------------------------
 * admin.js - the admin pages: manage titles, genres and tags.
 * The page is only a convenience: the real protection is on the server
 * (SecurityConfig lets only ROLE_ADMIN reach /api/admin/**).
 * ------------------------------------------------------------------ */

const Admin = (() => {
    // same rules as the CHECK constraints in the database and the AdminCatalogService
    const FORMATS = {
        ANIME: ['TV', 'MOVIE', 'OVA', 'ONA', 'SPECIAL'],
        MANGA: ['MANGA', 'ONE_SHOT', 'NOVEL']
    };
    const STATUSES = {
        ANIME: ['UPCOMING', 'AIRING', 'FINISHED', 'HIATUS'],
        MANGA: ['UPCOMING', 'RELEASING', 'FINISHED', 'HIATUS']
    };

    const state = {title: '', type: '', page: 0};
    const cache = {genres: [], tags: []};
    const root = () => $('#view');

    async function show(tab) {
        if (!Auth.isAdmin) {
            root().innerHTML = alertBox('This page is for administrators only.', 'warning');
            return;
        }
        if (tab === 'genres' || tab === 'tags') {
            await showNames(tab);
        } else {
            await showMedia();
        }
    }

    function tabs(active) {
        return `<h1 class="h3 mb-3">Admin</h1>
            <ul class="nav nav-tabs mb-4">
                ${[['media', 'Titles'], ['genres', 'Genres'], ['tags', 'Tags']].map(([key, label]) =>
                    `<li class="nav-item"><a class="nav-link ${key === active ? 'active' : ''}"
                        href="#/admin/${key}">${label}</a></li>`).join('')}
            </ul>`;
    }

    /* ------------------------------ titles ------------------------------ */

    async function showMedia() {
        const query = new URLSearchParams({page: state.page, size: 15});
        if (state.title) query.set('title', state.title);
        if (state.type) query.set('type', state.type);
        const [data, genres, tags] = await Promise.all([
            Api.get('/api/media?' + query), Api.get('/api/genres'), Api.get('/api/tags')]);
        cache.genres = genres;
        cache.tags = tags;

        root().innerHTML = tabs('media') + `
            <div class="d-flex flex-wrap justify-content-between gap-2 mb-3">
                <form class="d-flex gap-2" data-form="admin-filter">
                    <input name="title" class="form-control" placeholder="Search by title" value="${esc(state.title)}">
                    <select name="type" class="form-select">
                        <option value="">All</option>
                        <option value="ANIME"${selected('ANIME', state.type)}>Anime</option>
                        <option value="MANGA"${selected('MANGA', state.type)}>Manga</option>
                    </select>
                    <button class="btn btn-primary">Search</button>
                </form>
                <button class="btn btn-success" data-action="admin-new">+ New title</button>
            </div>
            <div id="mediaForm"></div>
            <div class="table-responsive">
                <table class="table table-sm align-middle">
                    <thead><tr><th>ID</th><th>Title</th><th>Type</th><th>Format</th><th>Status</th><th></th></tr></thead>
                    <tbody>
                        ${data.content.map(m => `
                            <tr>
                                <td>${m.id}</td>
                                <td><a href="#/media/${m.id}">${esc(m.title)}</a></td>
                                <td>${nice(m.mediaType)}</td><td>${nice(m.format)}</td><td>${nice(m.status)}</td>
                                <td class="text-end text-nowrap">
                                    <button class="btn btn-outline-primary btn-sm" data-action="admin-edit" data-id="${m.id}">Edit</button>
                                    <button class="btn btn-outline-danger btn-sm" data-action="admin-delete-media" data-id="${m.id}"
                                            data-title="${esc(m.title)}">Delete</button>
                                </td>
                            </tr>`).join('')}
                    </tbody>
                </table>
            </div>
            ${pager(data, 'admin-page')}`;
    }

    const options = (list, current) =>
        list.map(value => `<option value="${value}"${selected(value, current)}>${nice(value)}</option>`).join('');

    function mediaFormHtml(m) {
        const type = m.mediaType;
        const checks = (name, items, chosen) => items.map(item => `
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" name="${name}" value="${item.id}"
                       id="${name}${item.id}" ${chosen.includes(item.name) ? 'checked' : ''}>
                <label class="form-check-label" for="${name}${item.id}">${esc(item.name)}</label>
            </div>`).join('');
        return `
            <div class="card mb-4"><div class="card-body">
                <h2 class="h5">${m.id ? 'Edit title #' + m.id : 'New title'}</h2>
                <form data-form="admin-media" data-id="${m.id ?? ''}" class="row g-3">
                    <div class="col-md-8"><label class="form-label">Title</label>
                        <input name="title" class="form-control" required maxlength="255" value="${esc(m.title)}"></div>
                    <div class="col-md-4"><label class="form-label">Type</label>
                        <select name="mediaType" class="form-select" data-change="admin-type">
                            ${options(['ANIME', 'MANGA'], type)}
                        </select></div>
                    <div class="col-12"><label class="form-label">Description</label>
                        <textarea name="description" class="form-control" rows="3" maxlength="10000">${esc(m.description)}</textarea></div>
                    <div class="col-md-3"><label class="form-label">Format</label>
                        <select name="format" class="form-select">${options(FORMATS[type], m.format)}</select></div>
                    <div class="col-md-3"><label class="form-label">Status</label>
                        <select name="status" class="form-select">${options(STATUSES[type], m.status)}</select></div>
                    <div class="col-md-3"><label class="form-label">Start date</label>
                        <input name="startDate" type="date" class="form-control" value="${esc(m.startDate)}"></div>
                    <div class="col-md-3"><label class="form-label">End date</label>
                        <input name="endDate" type="date" class="form-control" value="${esc(m.endDate)}"></div>
                    <div class="col-md-3 ${type === 'ANIME' ? '' : 'd-none'}" data-for="ANIME"><label class="form-label">Episodes</label>
                        <input name="totalEpisodes" type="number" min="1" class="form-control" value="${esc(m.totalEpisodes)}"></div>
                    <div class="col-md-3 ${type === 'ANIME' ? '' : 'd-none'}" data-for="ANIME"><label class="form-label">Episode length (min)</label>
                        <input name="durationMinutes" type="number" min="1" class="form-control" value="${esc(m.durationMinutes)}"></div>
                    <div class="col-md-3 ${type === 'MANGA' ? '' : 'd-none'}" data-for="MANGA"><label class="form-label">Chapters</label>
                        <input name="totalChapters" type="number" min="1" class="form-control" value="${esc(m.totalChapters)}"></div>
                    <div class="col-md-3 ${type === 'MANGA' ? '' : 'd-none'}" data-for="MANGA"><label class="form-label">Volumes</label>
                        <input name="totalVolumes" type="number" min="1" class="form-control" value="${esc(m.totalVolumes)}"></div>
                    <div class="col-12"><label class="form-label">Cover image URL (optional)</label>
                        <input name="coverImageUrl" class="form-control" maxlength="500" value="${esc(m.coverImageUrl)}"></div>
                    <div class="col-12"><label class="form-label d-block">Genres</label>${checks('genre', cache.genres, m.genres)}</div>
                    <div class="col-12"><label class="form-label d-block">Tags</label>${checks('tag', cache.tags, m.tags)}</div>
                    <div class="col-12 d-flex gap-2">
                        <button class="btn btn-primary">Save</button>
                        <button type="button" class="btn btn-outline-secondary" data-action="admin-cancel">Cancel</button>
                    </div>
                </form>
            </div></div>`;
    }

    function openForm(m) {
        $('#mediaForm').innerHTML = mediaFormHtml(m);
        $('#mediaForm').scrollIntoView({behavior: 'smooth'});
    }

    Actions['admin-new'] = () => openForm({
        mediaType: 'ANIME', format: 'TV', status: 'AIRING', genres: [], tags: []
    });

    Actions['admin-edit'] = async element => {
        try {
            openForm(await Api.get('/api/media/' + element.dataset.id));
        } catch (error) {
            toast(error.message, 'danger');
        }
    };

    Actions['admin-cancel'] = () => {
        $('#mediaForm').innerHTML = '';
    };

    // switching anime <-> manga changes the allowed formats, statuses and number fields
    Changes['admin-type'] = select => {
        const form = select.form;
        const type = select.value;
        for (const [name, list] of [['format', FORMATS[type]], ['status', STATUSES[type]]]) {
            const current = form.elements[name].value;
            form.elements[name].innerHTML = options(list, list.includes(current) ? current : list[0]);
        }
        form.querySelectorAll('[data-for]').forEach(el => el.classList.toggle('d-none', el.dataset.for !== type));
    };

    Forms['admin-media'] = async form => {
        const e = form.elements;
        const text = name => e[name].value.trim() || null;
        const number = name => e[name].value === '' ? null : Number(e[name].value);
        const checked = name => [...form.querySelectorAll(`input[name="${name}"]:checked`)].map(i => Number(i.value));
        const anime = e.mediaType.value === 'ANIME';
        const body = {
            title: e.title.value.trim(),
            description: text('description'),
            mediaType: e.mediaType.value,
            format: e.format.value,
            status: e.status.value,
            startDate: text('startDate'),
            endDate: text('endDate'),
            // only the fields that belong to the type are sent
            totalEpisodes: anime ? number('totalEpisodes') : null,
            durationMinutes: anime ? number('durationMinutes') : null,
            totalChapters: anime ? null : number('totalChapters'),
            totalVolumes: anime ? null : number('totalVolumes'),
            coverImageUrl: text('coverImageUrl'),
            genreIds: checked('genre'),
            tagIds: checked('tag')
        };
        const id = form.dataset.id;
        const saved = await attempt(() => id
            ? Api.put('/api/admin/media/' + id, body)
            : Api.post('/api/admin/media', body), 'Saved');
        if (saved) showMedia().catch(error => toast(error.message, 'danger'));
    };

    Actions['admin-delete-media'] = async element => {
        if (!confirm('Delete "' + element.dataset.title + '"?\nIt is also removed from every user list, rating and favorite.')) {
            return;
        }
        const deleted = await attempt(() => Api.del('/api/admin/media/' + element.dataset.id), 'Deleted');
        if (deleted) showMedia().catch(error => toast(error.message, 'danger'));
    };

    Forms['admin-filter'] = form => {
        state.title = form.elements.title.value.trim();
        state.type = form.elements.type.value;
        state.page = 0;
        showMedia().catch(error => toast(error.message, 'danger'));
    };

    Actions['admin-page'] = element => {
        state.page = Number(element.dataset.page);
        showMedia().catch(error => toast(error.message, 'danger'));
    };

    /* --------------------------- genres and tags ------------------------- */

    async function showNames(kind) {
        const items = await Api.get('/api/' + kind);
        const label = kind === 'genres' ? 'genre' : 'tag';
        root().innerHTML = tabs(kind) + `
            <form class="input-group mb-4" style="max-width: 420px" data-form="admin-name-add" data-kind="${kind}">
                <input name="name" class="form-control" placeholder="New ${label}" required maxlength="50">
                <button class="btn btn-success">Add</button>
            </form>
            <div class="table-responsive"><table class="table table-sm align-middle" style="max-width: 640px">
                <tbody>
                    ${items.map(item => `
                        <tr>
                            <td>
                                <form class="d-flex gap-2" data-form="admin-name-save" data-kind="${kind}" data-id="${item.id}">
                                    <input name="name" class="form-control form-control-sm" required maxlength="50"
                                           value="${esc(item.name)}">
                                    <button class="btn btn-outline-primary btn-sm">Save</button>
                                </form>
                            </td>
                            <td class="text-secondary text-nowrap">${item.mediaCount !== undefined ? item.mediaCount + ' titles' : ''}</td>
                            <td class="text-end">
                                <button class="btn btn-outline-danger btn-sm" data-action="admin-name-delete"
                                        data-kind="${kind}" data-id="${item.id}" data-name="${esc(item.name)}">Delete</button>
                            </td>
                        </tr>`).join('')}
                </tbody>
            </table></div>`;
    }

    const reload = kind => showNames(kind).catch(error => toast(error.message, 'danger'));

    Forms['admin-name-add'] = async form => {
        const kind = form.dataset.kind;
        const ok = await attempt(() => Api.post('/api/admin/' + kind, {name: form.elements.name.value.trim()}), 'Added');
        if (ok) reload(kind);
    };

    Forms['admin-name-save'] = async form => {
        const kind = form.dataset.kind;
        const ok = await attempt(() => Api.put('/api/admin/' + kind + '/' + form.dataset.id,
            {name: form.elements.name.value.trim()}), 'Saved');
        if (ok) reload(kind);
    };

    Actions['admin-name-delete'] = async element => {
        const kind = element.dataset.kind;
        if (!confirm('Delete "' + element.dataset.name + '"? It is removed from every title that uses it.')) return;
        const ok = await attempt(() => Api.del('/api/admin/' + kind + '/' + element.dataset.id), 'Deleted');
        if (ok) reload(kind);
    };

    return {show};
})();
