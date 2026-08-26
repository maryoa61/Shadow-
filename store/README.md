# 🛒 SHADOW_STORE — فروشگاه سه‌بعدی با پرداخت تلگرامی

صفحه‌ی فرود فروشگاهی سه‌بعدی (Three.js) برای فروش اشتراک‌های SHADOW_NET با:

- ⭐ **پرداخت با استارز تلگرام** (Bot API → `createInvoiceLink` با ارز `XTR`)
- 🪙 **پرداخت کریپتو** (USDT-TRC20 / TRX / TON با QR و ثبت TXID)
- 💳 **کارت به کارت** (نمایش شماره کارت + ثبت کد پیگیری)
- 🤖 **اتصال به ربات تلگرام** با توکن — ارسال خودکار سفارش به ادمین
- ☁️ **اتصال به ورکر کلودفلر** — وبهوک ربات + API سفارش‌ها
- 🐙 **اتصال به مخزن گیت‌هاب** — مدیریت کاتالوگ محصولات از روی `products.json`

---

## ۱. ساخت ربات تلگرام و فعال‌سازی استارز

1. در تلگرام به [@BotFather](https://t.me/BotFather) برو و با `/newbot` ربات بساز و **توکن** را بگیر.
2. پرداخت با **Telegram Stars** به‌صورت پیش‌فرض برای ربات‌ها فعال است و به درگاه بانکی نیاز ندارد (ارز `XTR`).
3. دکمه‌ی **«اتصال‌ها»** را در صفحه‌ی فروشگاه باز کن، توکن و چت‌آیدی ادمین را وارد کن و **«تست اتصال»** را بزن.
4. برای گرفتن چت‌آیدی خودت، به [@userinfobot](https://t.me/userinfobot) پیام بده.

> نکته: چون Bot API تلگرام CORS را باز گذاشته، فروشگاه می‌تواند مستقیم از مرورگر فاکتور استارز بسازد و سفارش‌ها را برای ادمین بفرستد — حتی بدون ورکر.

## ۲. دیپلوی ورکر کلودفلر

```bash
cd store/worker
npm install -g wrangler
npx wrangler login
npx wrangler secret put BOT_TOKEN        # توکن ربات
npx wrangler secret put ADMIN_CHAT_ID    # چت‌آیدی ادمین
npx wrangler secret put ADMIN_KEY        # یک رمز دلخواه برای دیدن لیست سفارش‌ها
npx wrangler kv namespace create ORDERS  # آیدی را در wrangler.toml بگذار
npx wrangler deploy
```

بعد از دیپلوی، آدرس ورکر (مثل `https://shadow-store-worker.YOU.workers.dev`) را در بخش «اتصال‌ها» وارد کن و:

- **«تست سلامت ورکر»** → باید `ورکر سالم است ✅` ببینی.
- **«ثبت وبهوک روی ورکر»** → `setWebhook` روی `<worker>/webhook` انجام می‌شود.

ورکر این کارها را می‌کند:

| مسیر | کاربرد |
|---|---|
| `GET /health` | تست سلامت و وضعیت اتصال KV/ربات |
| `POST /webhook` | وبهوک تلگرام: `/start` با دکمه‌ی Web App، پاسخ `pre_checkout_query`، تأیید پرداخت موفق استارز و اطلاع به ادمین |
| `POST /api/order` | دریافت سفارش از صفحه، ذخیره در KV، ارسال اعلان به ادمین |
| `GET /api/products` | سرو کاتالوگ محصولات (از KV یا پروکسی گیت‌هاب با کش ۵ دقیقه‌ای) |
| `GET /api/orders?key=ADMIN_KEY` | مشاهده‌ی سفارش‌های اخیر (فقط با رمز ادمین) |

## ۳. اتصال مخزن گیت‌هاب (مدیریت محصولات)

کاتالوگ محصولات فایل [`products.json`](products.json) همین مخزن است. با ویرایش و پوش آن، همه می‌توانند از داخل فروشگاه دکمه‌ی **«همگام‌سازی»** را بزنند تا لیست به‌روز شود. در بخش «اتصال‌ها» می‌توانی مالک/مخزن/برنچ/مسیر دیگری هم تنظیم کنی.

فرمت هر محصول:

```json
{
  "id": "gold-6m",
  "name": "اشتراک طلایی",
  "badge": "محبوب‌ترین",
  "duration": "۶ ماهه",
  "desc": "توضیح کوتاه…",
  "features": ["حجم ۵۰۰ گیگابایت"],
  "priceToman": 449000,
  "oldPriceToman": 588000,
  "priceStars": 340,
  "gradient": ["#f59e0b", "#f43f5e"],
  "icon": "crown"
}
```

آیکون‌های موجود: `shield`, `bolt`, `crown`, `infinity`, `pin`, `users`, `box`

## ۴. دیپلوی صفحه روی GitHub Pages

ورک‌فلوی آماده در [`deploy/store-pages.yml.example`](deploy/store-pages.yml.example) قرار دارد:

1. آن را به مسیر `.github/workflows/store-pages.yml` کپی کن.
2. در گیت‌هاب: **Settings → Pages → Source = GitHub Actions**
3. روی `main` پوش کن — صفحه در آدرس زیر بالا می‌آید:
   `https://<user>.github.io/<repo>/store/`
3. همین آدرس را در `STORE_URL` داخل `wrangler.toml` بگذار تا دکمه‌ی `/start` ربات به فروشگاه وصل شود.

## ۵. اجرای محلی

```bash
cd store
python3 -m http.server 8000
# باز کن: http://localhost:8000
```

## جریان پرداخت استارز (فنی)

```
صفحه → createInvoiceLink (XTR) → باز شدن فاکتور در تلگرام
      → کاربر پرداخت می‌کند
تلگرام → POST /webhook (pre_checkout_query) → ورکر answer می‌دهد
تلگرام → POST /webhook (successful_payment) → تأیید خودکار + اطلاع به ادمین
```

اگر فروشگاه به‌عنوان **Telegram Web App** باز شود (دکمه‌ی بات)، پرداخت با `Telegram.WebApp.openInvoice` درون همان وب‌اپ انجام می‌شود و نام کاربری خریدار خودکار در فرم پر می‌شود.

## امنیت

- توکن ربات فقط در `localStorage` مرورگر خودت نگه‌داری می‌شود؛ برای محیط واقعی، منطق پرداخت را سمت ورکر/سرور نگه‌دارید تا توکن در کلاینت نباشد.
- هیچ‌وقت `ADMIN_KEY` و سکرت‌ها را داخل کد فرانت نگذارید.
