(() => {
  'use strict';

  const IS_MAC = /Mac|iPhone|iPad/.test(navigator.platform);
  const MOD_KEY = IS_MAC ? '⌘' : 'Ctrl';

  const FRAMES = [
    { key: 'compact', label: 'Compact', w: 320, h: 640 },
    { key: 'pixel', label: 'Pixel 6', w: 360, h: 720 },
    { key: 'tablet', label: 'Tablet', w: 520, h: 690 },
  ];
  const DENSITY_ORDER = ['ldpi', 'mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi'];
  const PALETTE_LIMIT = 40;
  const ZOOM_MIN = 0.15;
  const ZOOM_MAX = 6;
  const FIT_MARGIN = 96;
  const TOAST_MS = 1900;

  const ICONS = {
    grid: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect></svg>',
    gridSm: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect></svg>',
    list: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h16"></path></svg>',
    copy: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="12" height="12" rx="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>',
    external: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 4h6v6"></path><path d="M20 4l-9 9"></path><path d="M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"></path></svg>',
    compare: '<svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6"><rect x="3" y="4" width="7" height="16"></rect><rect x="14" y="4" width="7" height="16"></rect></svg>',
    minus: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><path d="M5 12h14"></path></svg>',
    plus: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><path d="M12 5v14M5 12h14"></path></svg>',
    search: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#9A9A9A" stroke-width="2.2" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="M20 20l-3.5-3.5"></path></svg>',
    check: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"></path></svg>',
  };

  let DATA = null;

  const state = {
    route: { kind: 'all' },
    q: '',
    view: 'grid',
    stateKey: 'Base|Default',
    compareCat: null,
    zoom: 1,
    panX: 0,
    panY: 0,
    dims: null,
    bezel: false,
    frame: 'pixel',
    palette: false,
    pq: '',
    pi: 0,
  };

  let canvasEl = null;
  let stageEl = null;
  let toastTimer = null;

  const $ = (sel, root) => (root || document).querySelector(sel);

  function esc(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // snapshotPath is written module-relative (e.g. "images/foo.png") with the
  // generating OS's separator; the aggregated site nests it under the module dir.
  function snapUrl(moduleName, snapshotPath) {
    const base = String(snapshotPath).split(/[\\/]/).pop();
    return encodeURIComponent(moduleName) + '/images/' + encodeURIComponent(base);
  }

  function groupOf(component) {
    return component.group || 'Ungrouped';
  }

  function plural(count, singular, pluralWord) {
    return count + ' ' + (count === 1 ? singular : pluralWord);
  }

  /* ------------------------------------------------------------- search */

  // Substring match only — used for shared-prefix fields (package, group, tags, module).
  function contains(text, q) {
    const i = (text || '').toLowerCase().indexOf(q);
    if (i < 0) return -1;
    return i === 0 ? 60 : 45 - Math.min(i, 20);
  }

  // Substring first, subsequence fallback — only ever applied to the component name.
  function fuzzyName(text, q) {
    const t = (text || '').toLowerCase();
    const i = t.indexOf(q);
    if (i === 0) return 100;
    if (i > 0) return 80 - Math.min(i, 20);
    let ti = 0;
    let first = -1;
    for (const ch of q) {
      const at = t.indexOf(ch, ti);
      if (at < 0) return -1;
      if (first < 0) first = at;
      ti = at + 1;
    }
    const span = ti - first;
    if (span > q.length + 3) return -1; // reject scattered letter-soup matches
    return 30 - (span - q.length);
  }

  function sortVariants(category, list) {
    const c = category.toLowerCase();
    const out = list.slice();
    if (c === 'density' && out.every((v) => !isNaN(parseFloat(v.variant)))) {
      return out.sort((a, b) => parseFloat(a.variant) - parseFloat(b.variant));
    }
    if (c === 'density') {
      return out.sort((a, b) =>
        DENSITY_ORDER.indexOf(a.variant.toLowerCase()) - DENSITY_ORDER.indexOf(b.variant.toLowerCase()));
    }
    if (c === 'font') {
      return out.sort((a, b) => parseFloat(a.variant) - parseFloat(b.variant));
    }
    return out.sort((a, b) => a.variant.localeCompare(b.variant, undefined, { numeric: true }));
  }

  /* --------------------------------------------------------------- data */

  function allItems() {
    const out = [];
    DATA.modules.forEach((m) => m.items.forEach((it) =>
      out.push({ ...it, module: m.name, designFileKey: m.designFileKey || null })));
    return out;
  }

  function findItem(id) {
    return allItems().find((x) => x.component.id === id) || null;
  }

  function stateGroupsOf(item) {
    const cats = {};
    item.variants.forEach((v) => {
      (cats[v.category] = cats[v.category] || []).push(v);
    });
    const groups = [{
      category: 'Base',
      items: [{ variant: 'Default', snapshotPath: item.snapshotPath }],
    }];
    Object.keys(cats).sort().forEach((c) => groups.push({ category: c, items: sortVariants(c, cats[c]) }));
    return groups;
  }

  function paletteResults() {
    const q = state.pq.trim().toLowerCase();
    const items = allItems();
    if (!q) return items.slice(0, PALETTE_LIMIT);
    return items
      .map((x) => {
        const c = x.component;
        const score = Math.max(
          fuzzyName(c.name, q),
          fuzzyName(c.functionName, q) - 5,
          contains(c.group, q) - 8,
          contains(c.packageName, q) - 14,
          contains(c.tags.join(' '), q) - 10,
          contains(x.module, q) - 6,
        );
        return { item: x, score };
      })
      .filter((x) => x.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, PALETTE_LIMIT)
      .map((x) => x.item);
  }

  function chipsFor(component, moduleName) {
    const out = [];
    if (moduleName) out.push({ label: moduleName, cls: 'chip--module' });
    if (component.group) out.push({ label: component.group, cls: 'chip--group' });
    const isView = component.tags.indexOf('view') > -1;
    out.push({ label: isView ? 'view' : 'composable', cls: isView ? 'chip--view' : 'chip--composable' });
    const g = (component.group || '').toLowerCase();
    component.tags
      .filter((t) => t !== 'composable' && t !== 'view' && t.toLowerCase() !== g)
      .forEach((t) => out.push({ label: t, cls: '' }));
    return out;
  }

  function chipRow(chips, limit) {
    return (limit ? chips.slice(0, limit) : chips)
      .map((c) => '<span class="chip ' + c.cls + '">' + esc(c.label) + '</span>')
      .join('');
  }

  /* ------------------------------------------------------------ routing */

  function parseHash() {
    const parts = location.hash.replace(/^#\/?/, '').split('/').map((p) => {
      try { return decodeURIComponent(p); } catch (e) { return p; }
    });
    if (parts[0] === 'component' && parts[1]) return { kind: 'detail', id: parts[1] };
    if (parts[0] === 'module' && parts[1]) {
      if (parts[2] === 'group' && parts[3]) return { kind: 'group', module: parts[1], group: parts[3] };
      return { kind: 'module', module: parts[1] };
    }
    return { kind: 'all' };
  }

  function hashFor(route) {
    if (route.kind === 'detail') return '#/component/' + encodeURIComponent(route.id);
    if (route.kind === 'module') return '#/module/' + encodeURIComponent(route.module);
    if (route.kind === 'group') {
      return '#/module/' + encodeURIComponent(route.module) + '/group/' + encodeURIComponent(route.group);
    }
    return '#/all';
  }

  function navTo(route) {
    const target = hashFor(route);
    if (location.hash === target) {
      applyRoute();
    } else {
      location.hash = target;
    }
  }

  function applyRoute() {
    const route = parseHash();
    const prev = state.route;
    if (route.kind === 'detail' && !(prev.kind === 'detail' && prev.id === route.id)) {
      state.stateKey = 'Base|Default';
      state.compareCat = null;
      state.zoom = 1;
      state.panX = 0;
      state.panY = 0;
      state.dims = null;
    }
    state.route = route;
    state.palette = false;
    renderPalette();
    render();
  }

  /* ------------------------------------------------------------ sidebar */

  function renderTree() {
    const q = state.q.trim().toLowerCase();
    const route = state.route;
    const total = allItems().length;

    const allActive = route.kind === 'all';
    let html =
      '<button type="button" class="tree-all' + (allActive ? ' is-active' : '') + '" data-nav="all">' +
      ICONS.grid +
      '<span class="label">All components</span>' +
      '<span class="count">' + total + '</span>' +
      '</button>';

    let anyMatch = false;
    DATA.modules.forEach((m) => {
      const matched = m.items.filter((it) => {
        if (!q) return true;
        const c = it.component;
        return (c.name + ' ' + c.group + ' ' + c.packageName + ' ' + c.tags.join(' '))
          .toLowerCase().indexOf(q) > -1;
      });
      if (q && matched.length === 0) return;
      anyMatch = anyMatch || matched.length > 0;

      const byGroup = {};
      matched.forEach((it) => {
        const g = groupOf(it.component);
        (byGroup[g] = byGroup[g] || []).push(it);
      });

      const modActive = route.kind === 'module' && route.module === m.name;
      html += '<div class="tree-module">' +
        '<button type="button" class="tree-module-head' + (modActive ? ' is-active' : '') + '" data-nav="module" data-module="' + esc(m.name) + '">' +
        '<span class="label">' + esc(m.name) + '</span>' +
        '<span class="count">' + esc(plural(matched.length, 'item', 'items')) + '</span>' +
        '</button>';

      Object.keys(byGroup).sort().forEach((g) => {
        const grpActive = route.kind === 'group' && route.module === m.name && route.group === g;
        html += '<div class="tree-group">' +
          '<button type="button" class="tree-group-head' + (grpActive ? ' is-active' : '') + '" data-nav="group" data-module="' + esc(m.name) + '" data-group="' + esc(g) + '">' +
          '<span class="label">' + esc(g) + '</span>' +
          '<span class="count">' + byGroup[g].length + '</span>' +
          '</button>';
        byGroup[g].forEach((it) => {
          const active = route.kind === 'detail' && route.id === it.component.id;
          html += '<button type="button" class="tree-item' + (active ? ' is-active' : '') + '" data-pick="' + esc(it.component.id) + '">' +
            '<span class="thumb"><img src="' + esc(snapUrl(m.name, it.snapshotPath)) + '" alt="" loading="lazy"></span>' +
            '<span class="label">' + esc(it.component.name) + '</span>' +
            '<span class="badge">' + (it.variants.length ? it.variants.length : '') + '</span>' +
            '</button>';
        });
        html += '</div>';
      });
      html += '</div>';
    });

    if (q && !anyMatch) {
      html += '<div class="tree-empty">No component matches “' + esc(state.q) + '”.</div>';
    }

    $('#tree').innerHTML = html;
  }

  /* ----------------------------------------------------------- overview */

  function renderOverview() {
    const route = state.route;
    const isAll = route.kind === 'all';
    const mod = isAll ? null : DATA.modules.find((m) => m.name === route.module);
    if (!isAll && !mod) {
      $('#main').innerHTML = '<div class="load-error">Module <code>' + esc(route.module) + '</code> not found.</div>';
      return;
    }

    const base = isAll ? allItems() : mod.items.map((it) => ({ ...it, module: mod.name }));
    const group = route.kind === 'group' ? route.group : null;
    const items = group ? base.filter((it) => groupOf(it.component) === group) : base;
    const variantCount = (list) => list.reduce((a, x) => a + x.variants.length, 0);

    const kind = isAll ? 'Catalogue' : (group ? 'Group' : 'Module');
    const name = isAll ? DATA.projectName : (group || mod.name);
    let meta;
    if (isAll) {
      meta = items.length + ' components across ' + DATA.modules.length + ' modules · ' +
        variantCount(items) + ' variant snapshots';
    } else if (group) {
      meta = plural(items.length, 'component', 'components') + ' in ' + mod.name + ' · ' +
        variantCount(items) + ' variant snapshots';
    } else {
      const groupCount = new Set(mod.items.map((x) => groupOf(x.component))).size;
      const vars = variantCount(mod.items);
      meta = plural(mod.items.length, 'component', 'components') + ' · ' +
        plural(groupCount, 'group', 'groups') + ' · ' +
        plural(vars, 'variant snapshot', 'variant snapshots');
    }

    const isGrid = state.view === 'grid';
    let html =
      '<div class="overview">' +
      '<div class="ov-head">' +
      '<div class="ov-head-text">' +
      '<div class="ov-kind-row">' +
      '<span class="ov-kind">' + esc(kind) + '</span>' +
      (group
        ? '<span class="ov-in">in</span>' +
          '<button type="button" class="ov-module-link" data-nav="module" data-module="' + esc(mod.name) + '">' + esc(mod.name) + '</button>'
        : '') +
      '</div>' +
      '<h1 class="ov-title">' + esc(name) + '</h1>' +
      '<p class="ov-meta">' + esc(meta) + '</p>' +
      '</div>' +
      '<div class="viewtoggle">' +
      '<button type="button" class="' + (isGrid ? 'is-active' : '') + '" data-view="grid">' + ICONS.gridSm + 'Grid</button>' +
      '<button type="button" class="' + (isGrid ? '' : 'is-active') + '" data-view="list">' + ICONS.list + 'List</button>' +
      '</div>' +
      '</div>';

    if (isGrid) {
      html += '<div class="cards">';
      items.forEach((it) => {
        const c = it.component;
        html += '<button type="button" class="card" data-pick="' + esc(c.id) + '">' +
          '<div class="card-shot"><img src="' + esc(snapUrl(it.module, it.snapshotPath)) + '" alt="' + esc(c.name) + '" loading="lazy"></div>' +
          '<div class="card-body">' +
          '<span class="card-name">' + esc(c.name) + '</span>' +
          '<span class="card-file">' + esc(c.fileName) + '</span>' +
          '<div class="chiprow">' + chipRow(chipsFor(c, isAll ? it.module : null), 3) + '</div>' +
          '</div>' +
          '</button>';
      });
      html += '</div>';
    } else {
      html += '<div class="rows' + (isAll ? ' is-all' : '') + '">' +
        '<div class="rows-head">' +
        '<span></span><span>Component</span>' +
        '<span class="clip">' + (isAll ? 'Module · Group' : 'Group') + '</span>' +
        '<span>Package</span><span>Tags</span><span class="right">Variants</span>' +
        '</div>';
      items.forEach((it) => {
        const c = it.component;
        const groupLabel = isAll ? it.module + ' · ' + groupOf(c) : groupOf(c);
        html += '<button type="button" class="row" data-pick="' + esc(c.id) + '">' +
          '<span class="shot"><img src="' + esc(snapUrl(it.module, it.snapshotPath)) + '" alt="" loading="lazy"></span>' +
          '<span class="name">' + esc(c.name) + '</span>' +
          '<span class="group">' + esc(groupLabel) + '</span>' +
          '<span class="pkg">' + esc(c.packageName) + '</span>' +
          '<span class="tags">' + esc(c.tags.join(', ')) + '</span>' +
          '<span class="vars' + (it.variants.length ? '' : ' none') + '">' + (it.variants.length || '—') + '</span>' +
          '</button>';
      });
      html += '</div>';
    }

    html += '</div>';
    $('#main').innerHTML = html;
    canvasEl = null;
    stageEl = null;
  }

  /* ------------------------------------------------------------- detail */

  function flatStates(groups) {
    const flat = [];
    groups.forEach((g) => g.items.forEach((s) =>
      flat.push({ key: g.category + '|' + s.variant, category: g.category, variant: s.variant, snapshotPath: s.snapshotPath })));
    return flat;
  }

  function renderDetail() {
    const item = findItem(state.route.id);
    if (!item) {
      $('#main').innerHTML = '<div class="load-error">Component <code>' + esc(state.route.id) + '</code> not found.</div>';
      return;
    }
    const c = item.component;
    const groups = stateGroupsOf(item);
    const flat = flatStates(groups);
    const active = flat.find((s) => s.key === state.stateKey) || flat[0];
    const compareGroup = state.compareCat ? groups.find((g) => g.category === state.compareCat) : null;
    const hasFigma = !!(c.designNodeId && item.designFileKey);
    const figmaUrl = hasFigma
      ? 'https://www.figma.com/file/' + encodeURIComponent(item.designFileKey) + '?node-id=' + encodeURIComponent(c.designNodeId)
      : '#';
    const frameDef = FRAMES.find((f) => f.key === state.frame) || FRAMES[1];
    const shotSrc = snapUrl(item.module, active.snapshotPath);
    const shotLabel = c.name + ' · ' + active.variant;

    let canvasInner;
    if (compareGroup) {
      canvasInner = '<div class="canvas-compare">' +
        compareGroup.items.map((s) =>
          '<div class="compare-item">' +
          '<div class="compare-shot"><img src="' + esc(snapUrl(item.module, s.snapshotPath)) + '" alt="' + esc(s.variant) + '"></div>' +
          '<div class="compare-caption"><span class="label">' + esc(s.variant) + '</span><span class="sub">' + esc(compareGroup.category) + '</span></div>' +
          '</div>').join('') +
        '</div>';
    } else if (state.bezel) {
      canvasInner = '<div class="canvas-solo"><div class="stage" id="stage">' +
        '<div class="phone">' +
        '<div class="phone-screen" style="width:' + frameDef.w + 'px;height:' + frameDef.h + 'px">' +
        '<div class="phone-status"><span>9:41</span><span>▮▮▮</span></div>' +
        '<div class="phone-body"><img src="' + esc(shotSrc) + '" alt="' + esc(shotLabel) + '"></div>' +
        '</div>' +
        '</div>' +
        '</div></div>';
    } else {
      canvasInner = '<div class="canvas-solo"><div class="stage" id="stage">' +
        '<div class="sheet"><img id="shot" src="' + esc(shotSrc) + '" alt="' + esc(shotLabel) + '"></div>' +
        '</div></div>';
    }

    const hint = compareGroup
      ? 'compare · ' + compareGroup.category
      : 'drag to pan · ' + MOD_KEY + ' + scroll to zoom';

    const stateSection =
      '<div class="panel-section">' +
      '<div class="panel-title-row">' +
      '<span class="panel-title">State</span>' +
      '<span class="panel-count">' + esc(plural(flat.length, 'snapshot', 'snapshots')) + '</span>' +
      '</div>' +
      groups.map((g) => {
        const on = state.compareCat === g.category;
        return '<div class="stategroup">' +
          '<div class="stategroup-head">' +
          '<span class="stategroup-label">' + esc(g.category === 'Base' ? 'base' : g.category) + '</span>' +
          (g.items.length > 1
            ? '<button type="button" class="btn-compare' + (on ? ' is-active' : '') + '" data-compare="' + esc(g.category) + '">' + ICONS.compare + 'Compare</button>'
            : '') +
          '</div>' +
          '<div class="statechips">' +
          g.items.map((s) => {
            const key = g.category + '|' + s.variant;
            const sel = key === active.key && !state.compareCat;
            return '<button type="button" class="statechip' + (sel ? ' is-active' : '') + '" data-state="' + esc(key) + '">' + esc(s.variant) + '</button>';
          }).join('') +
          '</div>' +
          '</div>';
      }).join('') +
      (flat.length === 1
        ? '<div class="hintbox">Only the base snapshot was generated. Turn on <code>enableVariants</code> in the <code>arkive { }</code> block to add font-scale, density and layout-direction states.</div>'
        : '') +
      '</div>';

    const canvasSection =
      '<div class="panel-section">' +
      '<div class="panel-title-row"><span class="panel-title">Canvas</span></div>' +
      '<div class="switchrow">' +
      '<span class="label">Device frame</span>' +
      '<button type="button" class="switch' + (state.bezel ? ' is-on' : '') + '" data-action="toggle-bezel"><span class="knob"></span></button>' +
      '</div>' +
      '<div class="framerow' + (state.bezel ? '' : ' is-off') + '">' +
      FRAMES.map((f) =>
        '<button type="button" class="framebtn' + (state.bezel && state.frame === f.key ? ' is-active' : '') + '" data-frame="' + esc(f.key) + '">' + esc(f.label) + '</button>').join('') +
      '</div>' +
      '<p class="panel-note">Snapshots are wrap-content crops, so the frame is only a size reference — it does not re-render the component.</p>' +
      '</div>';

    const metaRows = [
      { label: 'File', value: c.fileName },
      { label: 'Package', value: c.packageName },
      { label: 'Preview function', value: c.functionName },
      { label: 'Id', value: c.id },
      { label: 'Tags', value: c.tags.join(', ') || '—' },
      { label: 'Extra metadata', value: (c.extraMetadata || []).join(', ') || '—' },
    ];
    const sourceSection =
      '<div class="panel-section">' +
      '<div class="panel-title-row"><span class="panel-title">Source</span></div>' +
      metaRows.map((m) =>
        '<div class="metarow"><span class="label">' + esc(m.label) + '</span><span class="value">' + esc(m.value) + '</span></div>').join('') +
      '</div>';

    const designSection =
      '<div class="panel-section">' +
      '<div class="panel-title-row"><span class="panel-title">Design</span></div>' +
      (hasFigma
        ? '<div class="figma-card">' +
          '<div class="blurb">Linked to a Figma node — designers can diff the build against the source frame.</div>' +
          '<div class="node">' + esc(item.designFileKey) + ' · ' + esc(c.designNodeId) + '</div>' +
          '<a href="' + esc(figmaUrl) + '" target="_blank" rel="noopener">Open in Figma</a>' +
          '</div>'
        : '<div class="figma-none">No <code>designNodeId</code> on this component. Add one to <code>@ArkiveComposable</code> to link the Figma frame.</div>') +
      '</div>';

    const html =
      '<div class="detail">' +
      '<div class="detail-head">' +
      '<div class="crumbs">' +
      '<button type="button" data-nav="module" data-module="' + esc(item.module) + '">' + esc(item.module) + '</button>' +
      '<span>/</span>' +
      '<button type="button" data-nav="group" data-module="' + esc(item.module) + '" data-group="' + esc(groupOf(c)) + '">' + esc(groupOf(c)) + '</button>' +
      '<span>/</span><span class="here">' + esc(c.name) + '</span>' +
      '</div>' +
      '<div class="detail-head-row">' +
      '<div class="detail-head-text">' +
      '<h1 class="detail-title">' + esc(c.name) + '</h1>' +
      '<div class="detail-chips">' + chipRow(chipsFor(c, null)) +
      '<span class="detail-file">' + esc(c.fileName) + '</span>' +
      '</div>' +
      '</div>' +
      '<div class="detail-actions">' +
      '<button type="button" class="btn-copy" data-action="copy">' + ICONS.copy + 'Copy preview fn</button>' +
      (hasFigma
        ? '<a class="btn-figma" href="' + esc(figmaUrl) + '" target="_blank" rel="noopener">' + ICONS.external + 'Open in Figma</a>'
        : '') +
      '</div>' +
      '</div>' +
      '</div>' +
      '<div class="detail-body">' +
      '<div class="canvas" id="canvas">' +
      canvasInner +
      '<div class="zoombar">' +
      '<button type="button" class="icon" data-action="zoom-out">' + ICONS.minus + '</button>' +
      '<span class="pct" id="zoom-label">' + Math.round(state.zoom * 100) + '%</span>' +
      '<button type="button" class="icon" data-action="zoom-in">' + ICONS.plus + '</button>' +
      '<span class="divider"></span>' +
      '<button type="button" class="text" data-action="zoom-fit">Fit</button>' +
      '<button type="button" class="text" data-action="zoom-actual">1:1</button>' +
      '</div>' +
      '<div class="dimbadge" id="dim-label">' + (state.dims ? state.dims.w + ' × ' + state.dims.h + ' px' : 'measuring…') + '</div>' +
      '<div class="canvas-hint">' + esc(hint) + '</div>' +
      '</div>' +
      '<aside class="panel" id="panel">' + stateSection + canvasSection + sourceSection + designSection + '</aside>' +
      '</div>' +
      '</div>';

    const panelBefore = $('#panel');
    const panelScroll = panelBefore ? panelBefore.scrollTop : 0;
    $('#main').innerHTML = html;
    const panelAfter = $('#panel');
    if (panelAfter) panelAfter.scrollTop = panelScroll;

    canvasEl = $('#canvas');
    stageEl = $('#stage');
    updateStage();
    bindCanvas(item);
  }

  function updateStage() {
    if (stageEl) {
      stageEl.style.transform =
        'translate(' + state.panX + 'px,' + state.panY + 'px) scale(' + state.zoom + ')';
    }
    const label = $('#zoom-label');
    if (label) label.textContent = Math.round(state.zoom * 100) + '%';
  }

  function measure(img) {
    if (!img || !img.naturalWidth) return;
    const w = img.naturalWidth;
    const h = img.naturalHeight;
    if (state.dims && state.dims.w === w && state.dims.h === h) return;
    state.dims = { w: w, h: h };
    fitZoom();
    const dim = $('#dim-label');
    if (dim) dim.textContent = w + ' × ' + h + ' px';
  }

  function fitZoom() {
    const box = canvasEl ? canvasEl.getBoundingClientRect() : null;
    const d = state.dims;
    const fit = box && d ? Math.min(1, (box.width - FIT_MARGIN) / d.w, (box.height - FIT_MARGIN) / d.h) : 1;
    state.zoom = Math.max(ZOOM_MIN, fit);
    state.panX = 0;
    state.panY = 0;
    updateStage();
  }

  function bindCanvas(item) {
    if (!canvasEl) return;

    const shot = $('#shot');
    if (shot) {
      if (shot.complete && shot.naturalWidth) {
        requestAnimationFrame(() => measure(shot));
      } else {
        shot.addEventListener('load', () => measure(shot));
      }
    }

    canvasEl.addEventListener('mousedown', (e) => {
      if (state.compareCat || e.target.closest('button')) return;
      e.preventDefault();
      const sx = e.clientX;
      const sy = e.clientY;
      const ox = state.panX;
      const oy = state.panY;
      const move = (ev) => {
        state.panX = ox + (ev.clientX - sx);
        state.panY = oy + (ev.clientY - sy);
        updateStage();
      };
      const up = () => {
        window.removeEventListener('mousemove', move);
        window.removeEventListener('mouseup', up);
      };
      window.addEventListener('mousemove', move);
      window.addEventListener('mouseup', up);
    });

    canvasEl.addEventListener('wheel', (e) => {
      if (state.compareCat) return;
      e.preventDefault();
      if (e.ctrlKey || e.metaKey) {
        state.zoom = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, state.zoom * (e.deltaY > 0 ? 0.92 : 1.08)));
      } else {
        state.panY -= e.deltaY;
      }
      updateStage();
    }, { passive: false });
  }

  /* ------------------------------------------------------------ palette */

  function renderPalette() {
    const root = $('#palette-root');
    if (!state.palette) {
      root.innerHTML = '';
      return;
    }
    root.innerHTML =
      '<div class="palette-overlay" data-action="close-palette">' +
      '<div class="palette">' +
      '<div class="palette-head">' +
      ICONS.search +
      '<input type="text" id="palette-input" placeholder="Search components, groups, packages, tags…" autocomplete="off">' +
      '<kbd>esc</kbd>' +
      '</div>' +
      '<div class="palette-list" id="palette-list"></div>' +
      '<div class="palette-foot"><span>↑↓ navigate</span><span>↵ open</span><span>esc close</span></div>' +
      '</div>' +
      '</div>';

    $('.palette', root).addEventListener('click', (e) => e.stopPropagation());
    const input = $('#palette-input');
    input.value = state.pq;
    input.addEventListener('input', () => {
      state.pq = input.value;
      state.pi = 0;
      renderPaletteList();
    });
    input.focus();
    renderPaletteList();
  }

  function renderPaletteList() {
    const list = $('#palette-list');
    if (!list) return;
    const results = paletteResults();
    if (results.length === 0) {
      list.innerHTML = '<div class="palette-empty">Nothing matches “' + esc(state.pq) + '”</div>';
      return;
    }
    list.innerHTML = results.map((r, i) =>
      '<button type="button" class="pal-item' + (i === state.pi ? ' is-active' : '') + '" data-pick="' + esc(r.component.id) + '" data-pi="' + i + '">' +
      '<span class="thumb"><img src="' + esc(snapUrl(r.module, r.snapshotPath)) + '" alt="" loading="lazy"></span>' +
      '<span class="text">' +
      '<span class="name">' + esc(r.component.name) + '</span>' +
      '<span class="sub">' + esc(r.component.packageName + '.' + r.component.functionName) + '</span>' +
      '</span>' +
      '<span class="module">' + esc(r.module) + '</span>' +
      '</button>').join('');

    list.querySelectorAll('.pal-item').forEach((el) => {
      el.addEventListener('mouseenter', () => {
        const i = Number(el.dataset.pi);
        if (i !== state.pi) {
          state.pi = i;
          list.querySelectorAll('.pal-item').forEach((n) =>
            n.classList.toggle('is-active', Number(n.dataset.pi) === i));
        }
      });
    });
  }

  function openPalette() {
    state.palette = true;
    state.pq = '';
    state.pi = 0;
    renderPalette();
  }

  function closePalette() {
    state.palette = false;
    renderPalette();
  }

  /* -------------------------------------------------------------- toast */

  function showToast(text) {
    const root = $('#toast-root');
    root.innerHTML = '<div class="toast">' + ICONS.check + '<span>' + esc(text) + '</span></div>';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { root.innerHTML = ''; }, TOAST_MS);
  }

  function copyPreviewFn() {
    const item = findItem(state.route.id);
    if (!item) return;
    const c = item.component;
    const text = c.packageName + '.' + c.functionName;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).catch(() => {});
    } else {
      const ta = document.createElement('textarea');
      ta.value = text;
      document.body.appendChild(ta);
      ta.select();
      try { document.execCommand('copy'); } catch (e) { /* best effort */ }
      ta.remove();
    }
    showToast('Copied ' + c.functionName);
  }

  /* ------------------------------------------------------------- render */

  function render() {
    if (!DATA) return;
    renderTree();
    if (state.route.kind === 'detail') {
      renderDetail();
    } else {
      renderOverview();
    }
  }

  /* -------------------------------------------------------------- wiring */

  document.addEventListener('click', (e) => {
    const target = e.target.closest('[data-action], [data-nav], [data-pick], [data-view], [data-state], [data-compare], [data-frame]');
    if (!target) return;

    if (target.dataset.pick) {
      navTo({ kind: 'detail', id: target.dataset.pick });
      return;
    }
    if (target.dataset.nav === 'all') { navTo({ kind: 'all' }); return; }
    if (target.dataset.nav === 'module') { navTo({ kind: 'module', module: target.dataset.module }); return; }
    if (target.dataset.nav === 'group') {
      navTo({ kind: 'group', module: target.dataset.module, group: target.dataset.group });
      return;
    }
    if (target.dataset.view) {
      state.view = target.dataset.view;
      renderOverview();
      return;
    }
    if (target.dataset.state) {
      state.stateKey = target.dataset.state;
      state.compareCat = null;
      state.dims = null;
      renderDetail();
      return;
    }
    if (target.dataset.compare) {
      state.compareCat = state.compareCat === target.dataset.compare ? null : target.dataset.compare;
      renderDetail();
      return;
    }
    if (target.dataset.frame) {
      if (!state.bezel) return;
      state.frame = target.dataset.frame;
      renderDetail();
      return;
    }

    switch (target.dataset.action) {
      case 'open-palette': openPalette(); break;
      case 'close-palette': closePalette(); break;
      case 'copy': copyPreviewFn(); break;
      case 'toggle-bezel':
        state.bezel = !state.bezel;
        renderDetail();
        break;
      case 'zoom-in':
        state.zoom = Math.min(ZOOM_MAX, state.zoom * 1.25);
        updateStage();
        break;
      case 'zoom-out':
        state.zoom = Math.max(ZOOM_MIN, state.zoom / 1.25);
        updateStage();
        break;
      case 'zoom-fit': fitZoom(); break;
      case 'zoom-actual':
        state.zoom = 1;
        state.panX = 0;
        state.panY = 0;
        updateStage();
        break;
      default: break;
    }
  });

  document.addEventListener('keydown', (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      openPalette();
      return;
    }
    if (!state.palette) return;
    if (e.key === 'Escape') {
      e.preventDefault();
      closePalette();
      return;
    }
    const results = paletteResults();
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      state.pi = Math.min(state.pi + 1, results.length - 1);
      renderPaletteList();
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault();
      state.pi = Math.max(state.pi - 1, 0);
      renderPaletteList();
    }
    if (e.key === 'Enter' && results[state.pi]) {
      e.preventDefault();
      navTo({ kind: 'detail', id: results[state.pi].component.id });
    }
  });

  $('#tree-filter').addEventListener('input', (e) => {
    state.q = e.target.value;
    renderTree();
  });

  window.addEventListener('hashchange', applyRoute);

  /* --------------------------------------------------------------- boot */

  async function boot() {
    $('#search-kbd').textContent = MOD_KEY + 'K';
    try {
      const response = await fetch('arkive-showcase.json');
      if (!response.ok) throw new Error('HTTP ' + response.status);
      DATA = await response.json();
    } catch (error) {
      $('#main').innerHTML =
        '<div class="load-error">Could not load <code>arkive-showcase.json</code> (' + esc(error.message) + ').<br><br>' +
        'If you opened this file directly, serve the showcase directory instead, e.g. ' +
        '<code>python3 -m http.server</code> — or publish it to GitHub Pages.</div>';
      return;
    }

    const total = allItems().length;
    $('#brand-project').textContent = DATA.projectName || '';
    $('#search-label').textContent = 'Search ' + total + ' components…';
    $('#module-count').textContent = plural(DATA.modules.length, 'module', 'modules');
    document.title = 'Arkive Showcase — ' + (DATA.projectName || '');

    state.route = parseHash();
    render();
  }

  boot();
})();
