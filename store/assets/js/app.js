/* =========================================================
   SHADOW_STORE — منطق فروشگاه
   اتصال به ربات تلگرام (Bot API) · ورکر کلودفلر · مخزن گیت‌هاب
   پرداخت: استارز تلگرام (XTR) · کریپتو · کارت به کارت
   ========================================================= */
"use strict";

/* ---------- ابزارها ---------- */
const $ = (s, el = document) => el.querySelector(s);
const $$ = (s, el = document) => [...el.querySelectorAll(s)];
const faNum = (n) => Number(n).toLocaleString("fa-IR");
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

function toast(msg, type = "") {
  const box = $("#toasts");
  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.textContent = msg;
  box.appendChild(el);
  setTimeout(() => { el.style.opacity = "0"; el.style.transition = "opacity .4s"; }, 3200);
  setTimeout(() => el.remove(), 3700);
}

async function copyText(text, okMsg = "کپی شد") {
  try {
    await navigator.clipboard.writeText(text);
    toast(okMsg, "ok");
  } catch {
    const ta = document.createElement("textarea");
    ta.value = text; document.body.appendChild(ta); ta.select();
    document.execCommand("copy"); ta.remove();
    toast(okMsg, "ok");
  }
}

/* ---------- تنظیمات ---------- */
const SETTINGS_KEY = "shadow_store_settings_v1";
const DEFAULT_SETTINGS = {
  botToken: "",
  adminChatId: "",
  workerUrl: "",
  ghOwner: "maryoa61",
  ghRepo: "Shadow-",
  ghBranch: "main",
  ghPath: "store/products.json",
  cardNumber: "6037-9971-2345-6789",
  cardHolder: "فروشگاه شادو‌استور",
  usdtTrc20: "TDqShadowStoreUSDTexampleAddress9xYz",
  trxAddr: "TShadowStoreTRXexampleAddressA1b2C3",
  tonAddr: "UQCshadow-store-ton-example-address",
  usdtRate: 115000,
};

let settings = { ...DEFAULT_SETTINGS };
try {
  const saved = JSON.parse(localStorage.getItem(SETTINGS_KEY) || "{}");
  settings = { ...DEFAULT_SETTINGS, ...saved };
} catch { /* نادیده بگیر */ }

function saveSettings() {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
}

/* ---------- آیکون‌های محصول ---------- */
const ICONS = {
  shield: '<svg viewBox="0 0 24 24"><path d="M12 22s8-3.5 8-10V5l-8-3-8 3v7c0 6.5 8 10 8 10z"/></svg>',
  bolt: '<svg viewBox="0 0 24 24"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>',
  crown: '<svg viewBox="0 0 24 24"><path d="M2 8l4 4 6-7 6 7 4-4-2 11H4L2 8z"/></svg>',
  infinity: '<svg viewBox="0 0 24 24"><path d="M18.6 6.6c-2-2-5.2-2-7.2 0L12 6l-0.6-.6c-2-2-5.2-2-7.2 0s-2 5.2 0 7.2 5.2 2 7.2 0L12 12l.6.6c2 2 5.2 2 7.2 0s2-5.2 0-7.2z" transform="rotate(90 12 12) scale(0.9) translate(1.3 1.3)"/></svg>',
  pin: '<svg viewBox="0 0 24 24"><path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>',
  users: '<svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
  box: '<svg viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>',
};
const icon = (name) => ICONS[name] || ICONS.box;

/* ---------- کاتالوگ محصولات ---------- */
let products = [];
let catalogSource = "محلی";

async function loadCatalog() {
  // ۱) اگر ورکر تنظیم شده، اول از ورکر بگیر
  if (settings.workerUrl) {
    try {
      const r = await fetch(`${settings.workerUrl.replace(/\/+$/, "")}/api/products`, { cache: "no-store" });
      if (r.ok) {
        const data = await r.json();
        if (Array.isArray(data.products) && data.products.length) {
          products = data.products; catalogSource = "ورکر کلودفلر";
          renderProducts(); return;
        }
      }
    } catch { /* ادامه */ }
  }
  // ۲) فایل محلی products.json (کنار صفحه / دیپلوی گیت‌هاب‌پیجز)
  try {
    const r = await fetch(`products.json?v=${Date.now()}`, { cache: "no-store" });
    if (r.ok) {
      const data = await r.json();
      if (Array.isArray(data.products) && data.products.length) {
        products = data.products; catalogSource = "مخزن گیت‌هاب (صفحه)";
        renderProducts(); return;
      }
    }
  } catch { /* ادامه */ }
  // ۳) فالبک: خام گیت‌هاب
  try {
    await syncFromGithub(true);
    return;
  } catch { /* ادامه */ }
  catalogSource = "پیش‌فرض";
  renderProducts();
}

async function syncFromGithub(silent = false) {
  const { ghOwner, ghRepo, ghBranch, ghPath } = settings;
  if (!ghOwner || !ghRepo) {
    if (!silent) toast("مالک و نام مخزن گیت‌هاب را در تنظیمات وارد کن", "err");
    return;
  }
  const url = `https://raw.githubusercontent.com/${ghOwner}/${ghRepo}/${ghBranch || "main"}/${ghPath || "store/products.json"}`;
  const r = await fetch(url, { cache: "no-store" });
  if (!r.ok) throw new Error(`GitHub ${r.status}`);
  const data = await r.json();
  if (!Array.isArray(data.products) || !data.products.length) throw new Error("فرمت نامعتبر");
  products = data.products;
  catalogSource = `github.com/${ghOwner}/${ghRepo}`;
  renderProducts();
  if (!silent) toast("کاتالوگ از گیت‌هاب به‌روزرسانی شد", "ok");
  return data;
}

/* ---------- رندر محصولات ---------- */
function renderProducts() {
  $("#catalog-source").textContent = catalogSource;
  const grid = $("#product-grid");
  grid.innerHTML = "";
  if (!products.length) {
    grid.innerHTML = `<div class="glass" style="grid-column:1/-1;border-radius:18px;padding:34px;text-align:center;color:var(--txt-dim)">
      محصولی یافت نشد. فایل <b>products.json</b> را بررسی کن یا از بخش «اتصال‌ها» مخزن گیت‌هاب را همگام‌سازی کن.</div>`;
    return;
  }
  products.forEach((p) => {
    const [g1, g2] = p.gradient || ["#7c3aed", "#22d3ee"];
    const card = document.createElement("article");
    card.className = "p-card tilt";
    card.style.setProperty("--g1", g1);
    card.style.setProperty("--g2", g2);
    card.innerHTML = `
      <div class="glow"></div>
      <div class="p-top">
        <span class="p-badge">${esc(p.badge || "جدید")}</span>
        <span class="p-icon">${icon(p.icon)}</span>
      </div>
      <h3 class="p-name">${esc(p.name)}</h3>
      <span class="p-duration">${esc(p.duration || "")}</span>
      <p class="p-desc">${esc(p.desc || "")}</p>
      <ul class="p-features">${(p.features || []).map((f) => `<li>${esc(f)}</li>`).join("")}</ul>
      <div class="p-price">
        <b>${faNum(p.priceToman)}</b><small>تومان</small>
        ${p.oldPriceToman ? `<del>${faNum(p.oldPriceToman)}</del>` : ""}
      </div>
      <div class="p-stars">${faNum(p.priceStars)} ⭐ استارز</div>
      <div class="p-actions">
        <button class="btn btn-primary" data-add="${esc(p.id)}">افزودن به سبد</button>
        <button class="btn btn-ghost" data-buy="${esc(p.id)}">خرید سریع</button>
      </div>`;
    grid.appendChild(card);
  });
  bindTilt(grid);
}

/* ---------- افکت تیلت سه‌بعدی ---------- */
function bindTilt(scope = document) {
  $$(".tilt", scope).forEach((el) => {
    if (el.dataset.tiltBound) return;
    el.dataset.tiltBound = "1";
    el.addEventListener("pointermove", (e) => {
      const r = el.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width - 0.5;
      const y = (e.clientY - r.top) / r.height - 0.5;
      el.style.transform = `perspective(900px) rotateY(${x * -10}deg) rotateX(${y * 8}deg) translateY(-4px)`;
    });
    el.addEventListener("pointerleave", () => { el.style.transform = ""; });
  });
}

/* ---------- سبد خرید ---------- */
let cart = [];
try { cart = JSON.parse(localStorage.getItem("shadow_store_cart_v1") || "[]"); } catch { cart = []; }

const cartTotalToman = () => cart.reduce((s, it) => s + it.priceToman * it.qty, 0);
const cartTotalStars = () => cart.reduce((s, it) => s + it.priceStars * it.qty, 0);

function persistCart() { localStorage.setItem("shadow_store_cart_v1", JSON.stringify(cart)); }

function addToCart(id, openDrawer = false) {
  const p = products.find((x) => x.id === id);
  if (!p) return;
  const found = cart.find((x) => x.id === id);
  if (found) found.qty += 1;
  else cart.push({ id: p.id, name: p.name, duration: p.duration, priceToman: p.priceToman, priceStars: p.priceStars, icon: p.icon, gradient: p.gradient, qty: 1 });
  persistCart(); renderCart();
  toast(`«${p.name}» به سبد اضافه شد`, "ok");
  if (openDrawer) openCart();
}

function changeQty(id, delta) {
  const it = cart.find((x) => x.id === id);
  if (!it) return;
  it.qty += delta;
  if (it.qty <= 0) cart = cart.filter((x) => x.id !== id);
  persistCart(); renderCart();
}

function removeItem(id) { cart = cart.filter((x) => x.id !== id); persistCart(); renderCart(); }

function renderCart() {
  const box = $("#cart-items");
  const count = cart.reduce((s, x) => s + x.qty, 0);
  const badge = $("#cart-count");
  badge.textContent = faNum(count);
  badge.classList.toggle("hidden", count === 0);

  if (!cart.length) {
    box.innerHTML = `<div class="cart-empty">سبد خریدت خالی است 🛒<br>از بخش محصولات یک پلن انتخاب کن.</div>`;
  } else {
    box.innerHTML = "";
    cart.forEach((it) => {
      const [g1, g2] = it.gradient || ["#7c3aed", "#22d3ee"];
      const row = document.createElement("div");
      row.className = "cart-item";
      row.style.setProperty("--g1", g1); row.style.setProperty("--g2", g2);
      row.innerHTML = `
        <span class="ci-icon">${icon(it.icon)}</span>
        <div class="ci-info">
          <b>${esc(it.name)}</b>
          <span>${esc(it.duration || "")}</span>
          <div class="ci-qty" style="margin-top:6px">
            <button data-qty="${esc(it.id)}|-1">−</button><span>${faNum(it.qty)}</span><button data-qty="${esc(it.id)}|1">+</button>
          </div>
        </div>
        <div>
          <div class="ci-price">${faNum(it.priceToman * it.qty)} تومان</div>
          <button class="ci-remove" data-rm="${esc(it.id)}" title="حذف">✕</button>
        </div>`;
      box.appendChild(row);
    });
  }
  $("#cart-total").textContent = `${faNum(cartTotalToman())} تومان`;
  $("#stars-total").textContent = `${faNum(cartTotalStars())} ⭐`;
  $("#card-total").textContent = `${faNum(cartTotalToman())} تومان`;
}

/* ---------- کشو و مودال ---------- */
const backdrop = $("#backdrop");
function showBackdrop() { backdrop.classList.remove("hidden"); }
function hideBackdropIfFree() {
  if (!$(".modal.open") && !$(".drawer.open")) backdrop.classList.add("hidden");
}
function openCart() { $("#cart-drawer").classList.add("open"); showBackdrop(); }
function closeCart() { $("#cart-drawer").classList.remove("open"); hideBackdropIfFree(); }
function openModal(id) { $("#" + id).classList.add("open"); showBackdrop(); }
function closeModal(id) { $("#" + id).classList.remove("open"); hideBackdropIfFree(); }

/* ---------- تلگرام Bot API ---------- */
const TG_API = () => `https://api.telegram.org/bot${settings.botToken}`;

async function tgCall(method, payload) {
  if (!settings.botToken) throw new Error("توکن ربات تنظیم نشده است");
  const r = await fetch(`${TG_API()}/${method}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload || {}),
  });
  const data = await r.json();
  if (!data.ok) throw new Error(data.description || `خطای تلگرام (${r.status})`);
  return data.result;
}

async function testTelegram() {
  const line = $("#tg-status");
  line.className = "status-line"; line.textContent = "در حال تست اتصال…";
  try {
    const me = await tgCall("getMe");
    line.className = "status-line ok";
    line.textContent = `متصل شد ✅ ربات: @${me.username} (${me.first_name})`;
    $("#link-bot").href = `https://t.me/${me.username}`;
    toast(`ربات @${me.username} متصل است`, "ok");
  } catch (e) {
    line.className = "status-line err";
    line.textContent = `خطا: ${e.message}`;
    toast(`اتصال تلگرام ناموفق: ${e.message}`, "err");
  }
}

async function setWebhookToWorker() {
  const line = $("#tg-status");
  const base = settings.workerUrl?.replace(/\/+$/, "");
  if (!base) { toast("اول آدرس ورکر کلودفلر را وارد کن", "err"); return; }
  line.className = "status-line"; line.textContent = "در حال ثبت وبهوک…";
  try {
    await tgCall("setWebhook", { url: `${base}/webhook`, allowed_updates: ["message", "pre_checkout_query"] });
    line.className = "status-line ok";
    line.textContent = `وبهوک ثبت شد: ${base}/webhook`;
    toast("وبهوک تلگرام روی ورکر ثبت شد", "ok");
  } catch (e) {
    line.className = "status-line err";
    line.textContent = `خطا: ${e.message}`;
  }
}

/* ---------- ورکر کلودفلر ---------- */
async function testWorker() {
  const line = $("#worker-status");
  const base = settings.workerUrl?.replace(/\/+$/, "");
  if (!base) { line.className = "status-line err"; line.textContent = "آدرس ورکر خالی است"; return; }
  line.className = "status-line"; line.textContent = "در حال پینگ ورکر…";
  try {
    const r = await fetch(`${base}/health`, { cache: "no-store" });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    const data = await r.json().catch(() => ({}));
    line.className = "status-line ok";
    line.textContent = `ورکر سالم است ✅ ${data.service ? `(سرویس: ${data.service})` : ""}`;
    toast("ورکر کلودفلر پاسخ‌گوست", "ok");
  } catch (e) {
    line.className = "status-line err";
    line.textContent = `خطا: ${e.message}`;
  }
}

async function pushOrderToWorker(order) {
  const base = settings.workerUrl?.replace(/\/+$/, "");
  if (!base) return { skipped: true };
  const r = await fetch(`${base}/api/order`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(order),
  });
  if (!r.ok) throw new Error(`ورکر: HTTP ${r.status}`);
  return r.json().catch(() => ({}));
}

/* ---------- سفارش ---------- */
const tgWebApp = window.Telegram?.WebApp || null;
if (tgWebApp) { try { tgWebApp.ready(); tgWebApp.expand(); } catch { /* ignore */ } }

function buildOrder(method, ref) {
  return {
    id: `SHW-${Date.now().toString(36).toUpperCase()}`,
    createdAt: new Date().toISOString(),
    customer: {
      name: $("#co-name").value.trim(),
      telegram: $("#co-tg").value.trim(),
      tgUserId: tgWebApp?.initDataUnsafe?.user?.id || null,
    },
    items: cart.map((it) => ({ id: it.id, name: it.name, qty: it.qty, priceToman: it.priceToman, priceStars: it.priceStars })),
    totalToman: cartTotalToman(),
    totalStars: cartTotalStars(),
    payment: { method, reference: ref || null },
    status: method === "stars" ? "awaiting_payment" : "pending_review",
  };
}

function orderTextFa(o) {
  const methodFa = { stars: "⭐ استارز تلگرام", crypto: "🪙 کریپتو", card: "💳 کارت به کارت" }[o.payment.method] || o.payment.method;
  const lines = o.items.map((it) => `▫️ ${it.name} × ${it.qty} — ${faNum(it.priceToman * it.qty)} تومان`);
  return [
    `🧾 سفارش جدید ${o.id}`,
    `👤 مشتری: ${o.customer.name || "—"}`,
    `✈️ تلگرام: ${o.customer.telegram || "—"}`,
    "",
    ...lines,
    "",
    `💰 مبلغ: ${faNum(o.totalToman)} تومان (${faNum(o.totalStars)} استارز)`,
    `🏦 روش: ${methodFa}`,
    o.payment.reference ? `🔖 مرجع/پیگیری: ${o.payment.reference}` : "",
    `🕐 ${new Date(o.createdAt).toLocaleString("fa-IR")}`,
  ].filter(Boolean).join("\n");
}

async function notifyAdmin(order) {
  if (!settings.botToken || !settings.adminChatId) return { skipped: true };
  return tgCall("sendMessage", { chat_id: settings.adminChatId, text: orderTextFa(order) });
}

async function finalizeOrder(order, note) {
  let workerOk = false, tgOk = false;
  try { await pushOrderToWorker(order); workerOk = true; } catch (e) { console.warn("worker:", e); }
  try { await notifyAdmin(order); tgOk = true; } catch (e) { console.warn("telegram:", e); }

  $("#order-id").textContent = order.id;
  $("#order-note").textContent = note || (tgOk || workerOk
    ? "جزئیات سفارش برای ادمین ارسال شد. کانفیگ به‌زودی در تلگرام برایت ارسال می‌شود."
    : "سفارش ثبت شد، اما هیچ اتصالی (ربات/ورکر) فعال نیست؛ لطفاً رسید را برای پشتیبانی بفرست.");

  $("#co-step-2").classList.add("hidden");
  $("#co-step-3").classList.remove("hidden");
  cart = []; persistCart(); renderCart();
}

/* ---------- پرداخت با استارز ---------- */
async function payWithStars() {
  if (!settings.botToken) { $("#stars-warning").classList.remove("hidden"); toast("توکن ربات را در «اتصال‌ها» وارد کن", "err"); return; }
  if (!cart.length) { toast("سبد خرید خالی است", "err"); return; }

  const btn = $("#pay-stars-btn");
  btn.disabled = true; btn.textContent = "در حال ساخت فاکتور…";
  const order = buildOrder("stars");

  try {
    const prices = cart.map((it, i) => ({
      label: `${it.name} × ${it.qty}`,
      amount: Math.max(1, Math.round(it.priceStars * it.qty)),
    }));
    const invoiceLink = await tgCall("createInvoiceLink", {
      title: `سفارش ${order.id} — شادو‌استور`,
      description: cart.map((it) => `${it.name} × ${it.qty}`).join("، ").slice(0, 250),
      payload: order.id,
      currency: "XTR",
      prices,
    });

    await finalizeOrder(order, "فاکتور استارز ساخته شد؛ پس از پرداخت در تلگرام، ربات سفارش را خودکار تأیید می‌کند.");

    if (tgWebApp?.openInvoice) {
      tgWebApp.openInvoice(invoiceLink, (status) => {
        if (status === "paid") toast("پرداخت با استارز موفق بود", "ok");
        else if (status === "cancelled") toast("پرداخت لغو شد");
      });
    } else {
      window.open(invoiceLink, "_blank", "noopener");
      toast("فاکتور در تلگرام باز شد", "ok");
    }
  } catch (e) {
    toast(`خطا در پرداخت استارز: ${e.message}`, "err");
  } finally {
    btn.disabled = false; btn.textContent = "پرداخت با استارز ⭐";
  }
}

/* ---------- کریپتو ---------- */
const CRYPTO_ADDRS = () => ({
  usdt: settings.usdtTrc20,
  trx: settings.trxAddr,
  ton: settings.tonAddr,
});
function refreshCryptoPane() {
  const net = $("#crypto-net").value;
  const addr = CRYPTO_ADDRS()[net] || "—";
  $("#crypto-addr").textContent = addr;
  $("#crypto-qr").src = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=6&data=${encodeURIComponent(addr)}`;
  const toman = cartTotalToman();
  const usdt = settings.usdtRate > 0 ? (toman / settings.usdtRate).toFixed(2) : "—";
  $("#pay-crypto-btn").textContent = net === "usdt"
    ? `ثبت سفارش — معادل ≈ ${usdt} USDT`
    : `ثبت سفارش کریپتو (${faNum(toman)} تومان)`;
}

/* ---------- رویدادها ---------- */
function bindEvents() {
  // نوبار
  $("#btn-cart").addEventListener("click", () => { renderCart(); openCart(); });
  $("#btn-settings").addEventListener("click", () => { fillSettingsForm(); openModal("settings-modal"); });
  $("#btn-sync").addEventListener("click", async () => {
    const btn = $("#btn-sync"); btn.disabled = true;
    try { await syncFromGithub(); } catch (e) { toast(`همگام‌سازی ناموفق: ${e.message}`, "err"); }
    btn.disabled = false;
  });

  // بستن‌ها
  document.addEventListener("click", (e) => {
    const closer = e.target.closest("[data-close]");
    if (closer) {
      const id = closer.dataset.close;
      if (id === "cart-drawer") closeCart(); else closeModal(id);
    }
    if (e.target === backdrop || e.target.classList?.contains("modal")) {
      closeCart(); $$(".modal.open").forEach((m) => m.classList.remove("open")); hideBackdropIfFree();
    }
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") { closeCart(); $$(".modal.open").forEach((m) => m.classList.remove("open")); hideBackdropIfFree(); }
  });

  // محصولات (دلیگیشن)
  $("#product-grid").addEventListener("click", (e) => {
    const add = e.target.closest("[data-add]");
    const buy = e.target.closest("[data-buy]");
    if (add) addToCart(add.dataset.add);
    if (buy) { addToCart(buy.dataset.buy, true); }
  });

  // سبد
  $("#cart-items").addEventListener("click", (e) => {
    const qty = e.target.closest("[data-qty]");
    const rm = e.target.closest("[data-rm]");
    if (qty) { const [id, d] = qty.dataset.qty.split("|"); changeQty(id, Number(d)); }
    if (rm) removeItem(rm.dataset.rm);
  });

  // تسویه
  $("#btn-checkout").addEventListener("click", () => {
    if (!cart.length) { toast("سبد خرید خالی است", "err"); return; }
    closeCart();
    $("#co-step-1").classList.remove("hidden");
    $("#co-step-2").classList.add("hidden");
    $("#co-step-3").classList.add("hidden");
    if (tgWebApp?.initDataUnsafe?.user?.username && !$("#co-tg").value) {
      $("#co-tg").value = "@" + tgWebApp.initDataUnsafe.user.username;
    }
    openModal("checkout-modal");
  });

  $("#co-next").addEventListener("click", () => {
    if (!$("#co-name").value.trim()) { toast("نام را وارد کن", "err"); $("#co-name").focus(); return; }
    $("#co-step-1").classList.add("hidden");
    $("#co-step-2").classList.remove("hidden");
    $("#stars-total").textContent = `${faNum(cartTotalStars())} ⭐`;
    $("#card-total").textContent = `${faNum(cartTotalToman())} تومان`;
    $("#stars-warning").classList.toggle("hidden", Boolean(settings.botToken));
    $("#card-number").textContent = settings.cardNumber || "—";
    $("#card-holder").textContent = `به نام: ${settings.cardHolder || "—"}`;
    refreshCryptoPane();
  });
  $("#co-back").addEventListener("click", () => {
    $("#co-step-2").classList.add("hidden");
    $("#co-step-1").classList.remove("hidden");
  });

  // تب‌های پرداخت
  $$(".pay-tab").forEach((tab) => tab.addEventListener("click", () => {
    $$(".pay-tab").forEach((t) => t.classList.toggle("active", t === tab));
    const m = tab.dataset.method;
    $("#pay-stars").classList.toggle("hidden", m !== "stars");
    $("#pay-crypto").classList.toggle("hidden", m !== "crypto");
    $("#pay-card").classList.toggle("hidden", m !== "card");
  }));

  $("#pay-stars-btn").addEventListener("click", payWithStars);

  $("#crypto-net").addEventListener("change", refreshCryptoPane);
  $("#copy-crypto").addEventListener("click", () => copyText($("#crypto-addr").textContent, "آدرس کیف پول کپی شد"));
  $("#copy-card").addEventListener("click", () => copyText((settings.cardNumber || "").replace(/[-\s]/g, ""), "شماره کارت کپی شد"));

  $("#pay-crypto-btn").addEventListener("click", async () => {
    const tx = $("#crypto-tx").value.trim();
    if (!tx) { toast("هش تراکنش (TXID) را وارد کن", "err"); return; }
    const btn = $("#pay-crypto-btn"); btn.disabled = true;
    const order = buildOrder("crypto", `TXID: ${tx} | شبکه: ${$("#crypto-net").selectedOptions[0].textContent}`);
    await finalizeOrder(order);
    $("#crypto-tx").value = "";
    btn.disabled = false;
  });

  $("#pay-card-btn").addEventListener("click", async () => {
    const ref = $("#card-ref").value.trim();
    if (!ref) { toast("کد پیگیری واریز را وارد کن", "err"); return; }
    const btn = $("#pay-card-btn"); btn.disabled = true;
    const order = buildOrder("card", `پیگیری: ${ref}`);
    await finalizeOrder(order);
    $("#card-ref").value = "";
    btn.disabled = false;
  });

  /* ----- تنظیمات ----- */
  $("#btn-save-settings").addEventListener("click", () => {
    settings.botToken = $("#set-token").value.trim();
    settings.adminChatId = $("#set-admin").value.trim();
    settings.workerUrl = $("#set-worker").value.trim();
    settings.ghOwner = $("#set-gh-owner").value.trim();
    settings.ghRepo = $("#set-gh-repo").value.trim();
    settings.ghBranch = $("#set-gh-branch").value.trim() || "main";
    settings.ghPath = $("#set-gh-path").value.trim() || "store/products.json";
    settings.cardNumber = $("#set-card").value.trim();
    settings.cardHolder = $("#set-card-holder").value.trim();
    settings.usdtTrc20 = $("#set-usdt").value.trim();
    settings.trxAddr = $("#set-trx").value.trim();
    settings.tonAddr = $("#set-ton").value.trim();
    settings.usdtRate = Number($("#set-rate").value) || 0;
    saveSettings();
    toast("اتصال‌ها ذخیره شد", "ok");
  });

  $("#btn-reset-settings").addEventListener("click", () => {
    settings = { ...DEFAULT_SETTINGS };
    saveSettings(); fillSettingsForm();
    toast("به تنظیمات پیش‌فرض برگشت", "ok");
  });

  $("#btn-test-tg").addEventListener("click", () => {
    settings.botToken = $("#set-token").value.trim();
    settings.adminChatId = $("#set-admin").value.trim();
    saveSettings(); testTelegram();
  });
  $("#btn-webhook").addEventListener("click", () => {
    settings.botToken = $("#set-token").value.trim();
    settings.workerUrl = $("#set-worker").value.trim();
    saveSettings(); setWebhookToWorker();
  });
  $("#btn-test-worker").addEventListener("click", () => {
    settings.workerUrl = $("#set-worker").value.trim();
    saveSettings(); testWorker();
  });
  $("#btn-test-gh").addEventListener("click", async () => {
    settings.ghOwner = $("#set-gh-owner").value.trim();
    settings.ghRepo = $("#set-gh-repo").value.trim();
    settings.ghBranch = $("#set-gh-branch").value.trim() || "main";
    settings.ghPath = $("#set-gh-path").value.trim() || "store/products.json";
    saveSettings();
    const line = $("#gh-status");
    line.className = "status-line"; line.textContent = "در حال دریافت از گیت‌هاب…";
    try {
      const data = await syncFromGithub();
      line.className = "status-line ok";
      line.textContent = `${data.products.length} محصول بارگذاری شد ✅`;
    } catch (e) {
      line.className = "status-line err";
      line.textContent = `خطا: ${e.message}`;
    }
  });
}

function fillSettingsForm() {
  $("#set-token").value = settings.botToken || "";
  $("#set-admin").value = settings.adminChatId || "";
  $("#set-worker").value = settings.workerUrl || "";
  $("#set-gh-owner").value = settings.ghOwner || "";
  $("#set-gh-repo").value = settings.ghRepo || "";
  $("#set-gh-branch").value = settings.ghBranch || "";
  $("#set-gh-path").value = settings.ghPath || "";
  $("#set-card").value = settings.cardNumber || "";
  $("#set-card-holder").value = settings.cardHolder || "";
  $("#set-usdt").value = settings.usdtTrc20 || "";
  $("#set-trx").value = settings.trxAddr || "";
  $("#set-ton").value = settings.tonAddr || "";
  $("#set-rate").value = settings.usdtRate || "";
  $("#tg-status").textContent = "";
  $("#worker-status").textContent = "";
  $("#gh-status").textContent = "";
}

/* ---------- مارکی ویژگی‌ها ---------- */
function buildMarquee() {
  const items = [
    "<b>تحویل آنی</b> پس از تأیید پرداخت", "<b>هسته Xray</b> و sing-box", "<b>آنتی‌فیلتر DPI</b> فعال",
    "<b>مولتی‌هاپ ماتریکس</b> برای امنیت بیشتر", "پرداخت با <b>استارز تلگرام</b>", "پرداخت با <b>کریپتو</b> و کارت به کارت",
    "بدون <b>لاگ</b> کاربران", "سرورهای <b>۳۸ لوکیشن</b>", "پشتیبانی <b>۲۴/۷</b> در تلگرام",
  ];
  const track = $("#marquee-track");
  track.innerHTML = [...items, ...items].map((i) => `<span>◆ ${i}</span>`).join("");
}

/* ---------- شمارنده‌ها ---------- */
function animateCounters() {
  const el = $("#stat-users");
  let n = 12000;
  const target = 12400;
  const step = () => {
    n += Math.ceil((target - n) / 18);
    el.textContent = `+${faNum(n)}`;
    if (n < target) requestAnimationFrame(step);
  };
  step();
}

/* ---------- راه‌اندازی ---------- */
document.addEventListener("DOMContentLoaded", () => {
  buildMarquee();
  animateCounters();
  bindEvents();
  bindTilt(document);
  renderCart();
  loadCatalog().then(() => toast("کاتالوگ محصولات بارگذاری شد", "ok")).catch(() => toast("خطا در بارگذاری کاتالوگ", "err"));
});
