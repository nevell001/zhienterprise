/* 滚动入场 + 动效基座（全部页面共用）
   无障碍：prefers-reduced-motion 下全部关闭，元素直接呈终态 */
(function () {
  var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var els = document.querySelectorAll('.rv');
  var i;

  if (!('IntersectionObserver' in window) || reduce) {
    for (i = 0; i < els.length; i++) els[i].classList.add('in');
    return;
  }
  var io = new IntersectionObserver(function (es) {
    es.forEach(function (e) {
      if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
    });
  }, { threshold: .15, rootMargin: '0px 0px -10% 0px' });
  for (i = 0; i < els.length; i++) io.observe(els[i]);
})();

/* 表单失焦校验 */
(function () {
  var forms = document.querySelectorAll('[data-validate]');
  if (!forms.length) return;
  Array.prototype.forEach.call(forms, function (form) {
    Array.prototype.forEach.call(form.querySelectorAll('[required]'), function (f) {
      f.addEventListener('blur', function () { check(f); });
      f.addEventListener('input', function () {
        if (f.closest('.field').classList.contains('err')) check(f);
      });
    });
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var bad = null;
      Array.prototype.forEach.call(form.querySelectorAll('[required]'), function (f) {
        if (!check(f) && !bad) bad = f;
      });
      if (bad) { bad.focus(); bad.scrollIntoView({ block: 'center', behavior: reduce ? 'auto' : 'smooth' }); return; }
      var ok = form.parentNode.querySelector('[data-success]');
      if (ok) { form.style.display = 'none'; ok.style.display = 'block'; }
    });
  });

  function check(f) {
    var ok = f.value.trim() !== '';
    if (ok && f.type === 'email') ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.value.trim());
    var box = f.closest('.field');
    if (box) box.classList.toggle('err', !ok);
    return ok;
  }
})();

/* 阅读进度条（新闻详情） */
(function () {
  var bar = document.querySelector('[data-progress]');
  if (!bar) return;
  var art = document.querySelector('[data-article]');
  if (!art) return;
  var update = function () {
    var h = art.offsetHeight - window.innerHeight;
    var p = h > 0 ? Math.min(window.scrollY / h, 1) : 0;
    bar.style.transform = 'scaleX(' + p + ')';
  };
  window.addEventListener('scroll', update, { passive: true });
  update();
})();

/* 目录高亮（新闻详情） */
(function () {
  var toc = document.querySelector('[data-toc]');
  if (!toc || !('IntersectionObserver' in window)) return;
  var links = toc.querySelectorAll('a');
  var map = {};
  Array.prototype.forEach.call(links, function (a) {
    var t = document.querySelector(a.getAttribute('href'));
    if (t) map[t.id] = a;
  });
  var io = new IntersectionObserver(function (es) {
    es.forEach(function (e) {
      if (!e.isIntersecting) return;
      Array.prototype.forEach.call(links, function (l) { l.style.color = ''; l.style.fontWeight = ''; });
      if (map[e.target.id]) { map[e.target.id].style.color = 'var(--p-text)'; map[e.target.id].style.fontWeight = '600'; }
    });
  }, { rootMargin: '-20% 0px -70% 0px' });
  Object.keys(map).forEach(function (id) { io.observe(document.getElementById(id)); });
})();

/* 手风琴 */
(function () {
  var ac = document.querySelectorAll('[data-acc]');
  Array.prototype.forEach.call(ac, function (btn) {
    btn.addEventListener('click', function () {
      var item = btn.closest('.acc-item');
      var body = item.querySelector('.acc-body');
      var open = item.classList.contains('open');
      var group = btn.closest('[data-acc-group]');
      if (group) {
        Array.prototype.forEach.call(group.querySelectorAll('.acc-item.open'), function (o) {
          if (o !== item) { o.classList.remove('open'); o.querySelector('.acc-body').style.maxHeight = null; }
        });
      }
      item.classList.toggle('open', !open);
      body.style.maxHeight = open ? null : body.scrollHeight + 'px';
    });
  });
})();

/* 筛选（荣誉资质 / 方案） */
(function () {
  var filters = document.querySelectorAll('[data-filter-group]');
  Array.prototype.forEach.call(filters, function (group) {
    var btns = group.querySelectorAll('[data-filter]');
    var targetSel = group.getAttribute('data-filter-group');
    var items = document.querySelectorAll(targetSel);
    var countEl = document.querySelector('[data-count]');
    Array.prototype.forEach.call(btns, function (b) {
      b.addEventListener('click', function () {
        Array.prototype.forEach.call(btns, function (x) { x.classList.remove('tag', 'on'); x.classList.add('tag'); });
        b.classList.remove('tag'); b.classList.add('tag', 'on');
        var v = b.getAttribute('data-filter');
        var n = 0;
        Array.prototype.forEach.call(items, function (it) {
          var show = v === 'all' || it.getAttribute('data-cat') === v;
          it.style.display = show ? '' : 'none';
          if (show) n++;
        });
        if (countEl) countEl.textContent = n;
        var empty = document.querySelector('[data-empty]');
        if (empty) empty.style.display = n === 0 ? 'block' : 'none';
      });
    });
  });
})();