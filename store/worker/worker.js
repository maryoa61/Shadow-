/**
 * SHADOW_STORE — ورکر کلودفلر
 * -----------------------------------------------
 * مسئول:
 *   ۱) وبهوک ربات تلگرام: دستور /start، پاسخ به pre_checkout_query و پرداخت موفق استارز (XTR)
 *   ۲) API سفارش‌ها: دریافت سفارش از صفحه‌ی فرود، ذخیره در KV و اطلاع به ادمین
 *   ۳) API محصولات: سرو کاتالوگ از KV یا پروکسی فایل store/products.json از گیت‌هاب
 *
 * متغیرهای محیطی (در wrangler.toml یا داشبورد کلودفلر):
 *   BOT_TOKEN      → توکن ربات (بهتر است با wrangler secret put تنظیم شود)
 *   ADMIN_CHAT_ID  → چت‌آیدی ادمین برای دریافت اعلان سفارش
 *   STORE_URL      → آدرس صفحه‌ی فرود (برای دکمه‌ی Web App در /start)
 *   GH_REPO        → مثل: maryoa61/Shadow-
 *   GH_BRANCH      → پیش‌فرض: main
 *   GH_PATH        → پیش‌فرض: store/products.json
 *   ORDERS         → (اختیاری) Binding سپیس‌نیم KV برای ذخیره‌ی سفارش‌ها
 */

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...CORS_HEADERS },
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    try {
      switch (true) {
        case url.pathname === "/health":
          return json({
            ok: true,
            service: "shadow-store-worker",
            bot: Boolean(env.BOT_TOKEN),
            kv: Boolean(env.ORDERS),
            time: new Date().toISOString(),
          });

        case url.pathname === "/webhook" && request.method === "POST":
          return await handleTelegramWebhook(request, env);

        case url.pathname === "/api/order" && request.method === "POST":
          return await handleOrder(request, env);

        case url.pathname === "/api/products" && request.method === "GET":
          return await handleProducts(env, ctx);

        case url.pathname === "/api/orders" && request.method === "GET":
          return await handleListOrders(request, env);

        default:
          return json({ ok: false, error: "مسیر یافت نشد", path: url.pathname }, 404);
      }
    } catch (err) {
      return json({ ok: false, error: String(err && err.message || err) }, 500);
    }
  },
};

/* ================= تلگرام ================= */

async function tg(env, method, payload) {
  const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/${method}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const data = await res.json();
  if (!data.ok) throw new Error(`Telegram ${method}: ${data.description}`);
  return data.result;
}

async function handleTelegramWebhook(request, env) {
  if (!env.BOT_TOKEN) return json({ ok: false, error: "BOT_TOKEN تنظیم نشده" }, 500);
  const update = await request.json();

  // ۱) تأیید فاکتور استارز پیش از پرداخت (الزامی: حداکثر در ۱۰ ثانیه)
  if (update.pre_checkout_query) {
    await tg(env, "answerPreCheckoutQuery", {
      pre_checkout_query_id: update.pre_checkout_query.id,
      ok: true,
    });
    return json({ ok: true });
  }

  const msg = update.message;
  if (!msg) return json({ ok: true });

  // ۲) پرداخت موفق استارز
  if (msg.successful_payment) {
    const sp = msg.successful_payment;
    const orderId = sp.invoice_payload || "نامشخص";
    await tg(env, "sendMessage", {
      chat_id: msg.chat.id,
      text:
        `✅ پرداخت شما با موفقیت انجام شد!\n\n` +
        `🧾 سفارش: ${orderId}\n` +
        `⭐ مبلغ: ${sp.total_amount} استارز\n\n` +
        `کانفیگ اشتراک شما در چند لحظه‌ی آینده همین‌جا ارسال می‌شود. 🛡️`,
    });
    if (env.ADMIN_CHAT_ID) {
      await tg(env, "sendMessage", {
        chat_id: env.ADMIN_CHAT_ID,
        text:
          `💰 پرداخت استارز موفق\n` +
          `🧾 سفارش: ${orderId}\n` +
          `⭐ مبلغ: ${sp.total_amount} XTR\n` +
          `👤 کاربر: @${msg.from?.username || msg.from?.id}`,
      });
    }
    if (env.ORDERS) {
      const key = `order:${orderId}`;
      const prev = (await env.ORDERS.get(key, "json")) || {};
      await env.ORDERS.put(key, JSON.stringify({ ...prev, id: orderId, status: "paid", paidVia: "stars", paidAt: new Date().toISOString() }));
    }
    return json({ ok: true });
  }

  // ۳) دستورها
  const text = (msg.text || "").trim();
  if (text.startsWith("/start")) {
    const storeUrl = env.STORE_URL || "https://maryoa61.github.io/Shadow-/store/";
    await tg(env, "sendMessage", {
      chat_id: msg.chat.id,
      text:
        `سلام ${msg.from?.first_name || ""}! به فروشگاه SHADOW_NET خوش آمدی 🛡️\n\n` +
        `از دکمه‌ی زیر وارد فروشگاه سه‌بعدی شو، پلن را انتخاب کن و با استارز پرداخت کن تا کانفیگت آنی برسد.`,
      reply_markup: {
        inline_keyboard: [
          [{ text: "🛒 ورود به فروشگاه", web_app: { url: storeUrl } }],
          [{ text: "💬 پشتیبانی", url: `https://t.me/${(env.SUPPORT_USER || "shadow_support").replace("@", "")}` }],
        ],
      },
    });
    return json({ ok: true });
  }

  if (text.startsWith("/order")) {
    await tg(env, "sendMessage", {
      chat_id: msg.chat.id,
      text: "برای پیگیری سفارش، شماره‌ی سفارشت (مثل SHW-XXXX) را همین‌جا بفرست تا پشتیبانی بررسی کند. 🧾",
    });
    return json({ ok: true });
  }

  return json({ ok: true });
}

/* ================= سفارش‌ها ================= */

async function handleOrder(request, env) {
  const order = await request.json();
  if (!order || !Array.isArray(order.items) || !order.items.length) {
    return json({ ok: false, error: "سفارش نامعتبر است" }, 400);
  }
  order.receivedAt = new Date().toISOString();

  // ذخیره در KV (در صورت اتصال)
  if (env.ORDERS) {
    await env.ORDERS.put(`order:${order.id}`, JSON.stringify(order));
  }

  // اطلاع به ادمین در تلگرام
  let notified = false;
  if (env.BOT_TOKEN && env.ADMIN_CHAT_ID) {
    const methodFa = { stars: "⭐ استارز", crypto: "🪙 کریپتو", card: "💳 کارت‌به‌کارت" }[order.payment?.method] || "نامشخص";
    const lines = order.items.map((it) => `▫️ ${it.name} × ${it.qty}`);
    await tg(env, "sendMessage", {
      chat_id: env.ADMIN_CHAT_ID,
      text:
        `🧾 سفارش جدید از ورکر — ${order.id}\n\n` +
        lines.join("\n") +
        `\n\n💰 ${Number(order.totalToman || 0).toLocaleString("fa-IR")} تومان · ${methodFa}` +
        (order.payment?.reference ? `\n🔖 ${order.payment.reference}` : "") +
        `\n👤 ${order.customer?.name || "—"} · ${order.customer?.telegram || "—"}`,
    });
    notified = true;
  }

  return json({ ok: true, id: order.id, stored: Boolean(env.ORDERS), notified });
}

async function handleListOrders(request, env) {
  const url = new URL(request.url);
  if (!env.ADMIN_KEY || url.searchParams.get("key") !== env.ADMIN_KEY) {
    return json({ ok: false, error: "دسترسی غیرمجاز" }, 401);
  }
  if (!env.ORDERS) return json({ ok: true, orders: [], note: "KV متصل نیست" });
  const list = await env.ORDERS.list({ prefix: "order:", limit: 100 });
  const orders = [];
  for (const k of list.keys) {
    const o = await env.ORDERS.get(k.name, "json");
    if (o) orders.push(o);
  }
  return json({ ok: true, orders });
}

/* ================= محصولات ================= */

async function handleProducts(env, ctx) {
  // اول KV → اگر کاتالوگ دستی در KV گذاشته شده
  if (env.ORDERS) {
    const cached = await env.ORDERS.get("catalog", "json");
    if (cached && Array.isArray(cached.products)) return json(cached);
  }

  // پروکسی فایل محصولات از مخزن گیت‌هاب
  const repo = env.GH_REPO || "maryoa61/Shadow-";
  const branch = env.GH_BRANCH || "main";
  const path = env.GH_PATH || "store/products.json";
  const raw = `https://raw.githubusercontent.com/${repo}/${branch}/${path}`;
  const res = await fetch(raw, {
    headers: { "User-Agent": "shadow-store-worker" },
    cf: { cacheTtl: 300, cacheEverything: true },
  });
  if (!res.ok) return json({ ok: false, error: `GitHub: HTTP ${res.status}` }, 502);
  const data = await res.json();
  return json(data);
}
