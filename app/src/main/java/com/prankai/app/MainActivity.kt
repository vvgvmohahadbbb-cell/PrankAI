package com.prankai.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.prankai.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val client = OkHttpClient()

    private val categories = listOf(
        "تحذير أمني (system alert)",
        "رسالة غامضة (mysterious)",
        "رسالة من 'مجهول'",
        "رسالة 'خطيرة' (serious tone)",
        "رسالة 'self-destruct' (فكاهية)"
    )

    private val fallbackTemplates = mapOf(
        "تحذير أمني (system alert)" to listOf(
            "⚠️ تنبيه أمني — WhatsApp\n\nتم اكتشاف نشاط غير عادي في حسابك.\nإذا لم تقم بالتأكيد خلال 24 ساعة، سيتم إيقاف الحساب نهائياً.\n\nللتأكيد، اكتب: \"أنا لست روبوت\"\n\n— فريق أمن WhatsApp",
            "⚠️ تنبيه — WhatsApp\n\nحسابك سجل دخول من جهاز جديد في: بغداد، العراق\nإذا لم تكن أنت، اكتب \"أنا لست روبوت\" خلال 10 دقائق.\n\n— أمن WhatsApp"
        ),
        "رسالة غامضة (mysterious)" to listOf(
            "لا أريد أن أخبرك بهذا... لكنني اكتشفت شيئاً.\nأنت تفكر أنك تعرف من يتحدث إليك، لكن الحقيقة أعمق.\n\nلا ترد على هذه الرسالة. فقط... انتبه.\n\n(لقد أرسلت هذا بالخطأ. تجاهل. 😂)",
            "أنت تقرأ هذه الرسالة...\nوهذا يعني أنك ما زلت هنا.\n\nلكن... هل أنت متأكد؟\n\n(مقلب! 😂😂😂)"
        ),
        "رسالة من 'مجهول'" to listOf(
            "لا أعرف من أنت، لكن أعرف من أنت.\nلقد رأيت ما فعلت.\nلا ترد. فقط... انتبه.\n\n— صديق",
            "نحن نراقب. نحن نسمع. نحن نعرف.\n(مقلب! 😂)"
        ),
        "رسالة 'خطيرة' (serious tone)" to listOf(
            "يجب أن أخبرك بشيء قبل فوات الأوان.\nأنت لا تعرف من أنت حقاً.\n(مقلب! 😂)",
            "هذا ليس مقلب. هذا تحذير.\n... أم أنه مقلب؟ 😂"
        ),
        "رسالة 'self-destruct' (فكاهية)" to listOf(
            "هذه الرسالة ستختفي خلال 10 ثواني...\n3... 2... 1...\n(مقلب! 😂 لا تختفي أبداً)",
            "⚠️ self-destruct activated\n(just kidding 😂)"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spinnerCategory.adapter = adapter

        val prefs = getSharedPreferences("prankai", Context.MODE_PRIVATE)
        b.editApiKey.setText(prefs.getString("api_key", "") ?: "")

        b.btnGenerate.setOnClickListener { generate() }
        b.btnCopy.setOnClickListener { copyResult() }
        b.btnShare.setOnClickListener { shareToWhatsApp() }
    }

    private fun generate() {
        val category = b.spinnerCategory.selectedItem.toString()
        val apiKey = b.editApiKey.text.toString().trim()
        prefsSave(apiKey)

        b.progressBar.visibility = View.VISIBLE
        b.btnGenerate.isEnabled = false
        b.textResult.text = ""

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (apiKey.isNotEmpty()) {
                    try { callAi(apiKey, category) } catch (e: Exception) { fallback(category) + "\n\n[AI error: ${e.message}]" }
                } else {
                    fallback(category)
                }
            }
            b.textResult.text = result
            b.progressBar.visibility = View.GONE
            b.btnGenerate.isEnabled = true
        }
    }

    private fun callAi(apiKey: String, category: String): String {
        val systemPrompt = "أنت مساعد يولد رسائل مقالب (prank messages) 'ثقيلة' (جدية في الأسلوب) لكن مضحكة وآمنة (لا تعطّل أجهزة، لا تخدع حقيقة، لا تسبب ضرراً). " +
            "النوع المطلوب: $category. " +
            "أنتج رسالة واحدة فقط، جاهزة للإرسال في WhatsApp، بأسلوب جدي/مثير، ثم أضف في النهاية '(مقلب! 😂)' أو '(just kidding 😂)'. " +
            "استخدم العربية (ويمكن إضافة إنجليزية). لا تستخدم أكواد أو روابط. اجعلها قصيرة (3-6 أسطر)."
        val userPrompt = "توليد رسالة مقلب من نوع: $category"

        val body = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userPrompt) })
            })
            put("temperature", 0.9)
            put("max_tokens", 200)
        }

        val req = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            val json = JSONObject(resp.body!!.string())
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
        }
    }

    private fun fallback(category: String): String {
        val list = fallbackTemplates[category] ?: fallbackTemplates.values.flatten()
        return list.random()
    }

    private fun prefsSave(key: String) {
        getSharedPreferences("prankai", Context.MODE_PRIVATE).edit().putString("api_key", key).apply()
    }

    private fun copyResult() {
        val text = b.textResult.text.toString()
        if (text.isBlank()) { Toast.makeText(this, "لا يوجد نص", Toast.LENGTH_SHORT).show(); return }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("prank", text))
        Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
    }

    private fun shareToWhatsApp() {
        val text = b.textResult.text.toString()
        if (text.isBlank()) { Toast.makeText(this, "لا يوجد نص", Toast.LENGTH_SHORT).show(); return }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val generic = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(generic, "إرسال عبر"))
        }
    }
}
