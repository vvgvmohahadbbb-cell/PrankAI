# PrankAI — رسائل مقالب (ثقيلة but مضحكة)

تطبيق Android (Kotlin) يولد رسائل مقالب "ثقيلة" (أسلوب جدي) لكن مضحكة وآمنة (لا تعطّل أجهزة).

## الميزات
- توليد رسائل مقالب عبر OpenAI API (gpt-4o-mini) — يتطلب مفتاح API
- 5 أنواع: تحذير أمني، غامضة، من 'مجهول'، 'خطيرة'، 'self-destruct'
- Fallback: قوالب جاهزة بدون مفتاح
- نسخ / إرسال مباشرة عبر WhatsApp

## بناء APK
1. افتح Android Studio → Open → اختر مجلد `PrankAI`
2. انتظر Gradle sync
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. APK في: `app/build/outputs/apk/debug/app-debug.apk`

## ملاحظة أخلاقية
- الرسائل آمنة (نص فقط، لا تعطّل أجهزة)
- لا تستخدمها لإخافة/إيذاء (كبار السن، قلقين، إلخ)
- أرسل "مقلب 😂" بعد رد الضحية
