package ir.dadban.glass.model

import org.json.JSONArray
import org.json.JSONObject

// -------------------------------------------------------------
// 1. چت یکپارچه (Unified Chat)
// -------------------------------------------------------------
data class ChatRequest(
    val conversationId: Int? = null,
    val message: String,
    val imageBase64: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("conversation_id", conversationId ?: JSONObject.NULL)
        put("message", message)
        if (!imageBase64.isNullOrBlank()) {
            put("image_base64", imageBase64)
        }
    }
}

data class CitedArticle(
    val id: String = "",
    val title: String = "",
    val lawName: String = "",
    val summary: String = ""
) {
    companion object {
        fun fromJson(obj: JSONObject): CitedArticle = CitedArticle(
            id = obj.optString("id", obj.optString("article_id", "")),
            title = obj.optString("title", obj.optString("article_number", "")),
            lawName = obj.optString("law_name", obj.optString("collection", "قانون")),
            summary = obj.optString("summary", obj.optString("text", ""))
        )
    }
}

data class PrintableDocument(
    val title: String = "",
    val content: String = "",
    val docType: String = ""
) {
    companion object {
        fun fromJson(obj: JSONObject?): PrintableDocument? {
            if (obj == null) return null
            val content = obj.optString("content", obj.optString("body", ""))
            if (content.isBlank()) return null
            return PrintableDocument(
                title = obj.optString("title", "سند حقوقی دادبان"),
                content = content,
                docType = obj.optString("doc_type", "تنظیم اوراق قضایی")
            )
        }
    }
}

data class VerificationData(
    val mode: String = "rag", // rag | calculator_and_rag | document | scanner
    val printableDocument: PrintableDocument? = null
) {
    companion object {
        fun fromJson(obj: JSONObject?): VerificationData {
            if (obj == null) return VerificationData()
            val printDocObj = obj.optJSONObject("printable_document")
            return VerificationData(
                mode = obj.optString("mode", "rag"),
                printableDocument = PrintableDocument.fromJson(printDocObj)
            )
        }
    }
}

data class ChatResponseData(
    val conversationId: Int?,
    val messageId: Int?,
    val answer: String,
    val citedArticles: List<CitedArticle> = emptyList(),
    val verification: VerificationData = VerificationData(),
    val subscription: JSONObject? = null
) {
    companion object {
        fun fromJson(obj: JSONObject): ChatResponseData {
            val convId = if (obj.has("conversation_id") && !obj.isNull("conversation_id")) obj.optInt("conversation_id") else null
            val msgId = if (obj.has("message_id") && !obj.isNull("message_id")) obj.optInt("message_id") else null
            val ans = obj.optString("answer", obj.optString("response", ""))
            
            val articles = mutableListOf<CitedArticle>()
            val articlesArray = obj.optJSONArray("cited_articles")
            if (articlesArray != null) {
                for (i in 0 until articlesArray.length()) {
                    val item = articlesArray.optJSONObject(i)
                    if (item != null) articles.add(CitedArticle.fromJson(item))
                }
            }
            
            val ver = VerificationData.fromJson(obj.optJSONObject("verification"))
            val sub = obj.optJSONObject("subscription")
            return ChatResponseData(convId, msgId, ans, articles, ver, sub)
        }
    }
}

data class ChatLine(
    val text: String,
    val fromUser: Boolean,
    val imageBase64: String? = null,
    val citedArticles: List<CitedArticle> = emptyList(),
    val verification: VerificationData = VerificationData(),
    val timestamp: Long = System.currentTimeMillis()
)

// -------------------------------------------------------------
// 2. احراز هویت و کاربر (Auth & Profile)
// -------------------------------------------------------------
data class UserDto(
    val id: Int = 0,
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val nationalCode: String = "",
    val hasActiveSubscription: Boolean = false,
    val subscriptionPlan: String = "پایه (رایگان)"
) {
    companion object {
        fun fromJson(obj: JSONObject): UserDto = UserDto(
            id = obj.optInt("id", 0),
            name = obj.optString("name", "کاربر دادبان"),
            mobile = obj.optString("mobile", ""),
            email = obj.optString("email", ""),
            nationalCode = obj.optString("national_code", ""),
            hasActiveSubscription = obj.optBoolean("has_active_subscription", false),
            subscriptionPlan = obj.optString("subscription_plan", "پایه (رایگان)")
        )
    }
}

data class AuthResponse(
    val token: String,
    val user: UserDto? = null
)

// -------------------------------------------------------------
// 3. محاسبه‌گرهای حقوقی (7 Legal Calculators)
// -------------------------------------------------------------
enum class CalculatorType(val key: String, val titleFa: String, val desc: String) {
    MEHRIEH("mehrieh", "محاسبه مهریه", "محاسبه ارزش روز مهریه بر اساس شاخص بانک مرکزی"),
    ERS("ers", "محاسبه سهم‌الارث", "تقسیم ارث مطابق قانون مدنی بین طبقات و همسر"),
    DIEH("dieh", "محاسبه دیه", "محاسبه درصدهای قانونی دیه در ماه‌های عادی و حرام"),
    DADRESI("dadresi", "هزینه دادرسی", "محاسبه تمبر و هزینه دادرسی مراحل بدوی و تجدیدنظر"),
    TAKHIRE_TADIEH("takhire_tadieh", "خسارت تأخیر تأدیه", "محاسبه کاهش ارزش پول بر پایه شاخص بهای کالا"),
    VEKALAT("vekalat", "تعرفه حق‌الوکاله", "تعرفه قانونی حق‌الوکاله دعاوی مالی و غیرمالی"),
    SANAVAT("sanavat", "سنوات و پایان‌کار", "مطالبات کارگری، سنوات و عیدی مطابق قانون کار")
}

data class CalculatorComputeResult(
    val title: String,
    val totalAmount: String,
    val breakdown: List<Pair<String, String>> = emptyList(),
    val lawNote: String = ""
)

// -------------------------------------------------------------
// 4. قوانین و آرای قضایی (Laws & Precedents)
// -------------------------------------------------------------
data class LawCollection(
    val id: String,
    val title: String,
    val category: String,
    val articleCount: Int
)

data class LawArticle(
    val id: String,
    val number: String,
    val collectionName: String,
    val text: String,
    val notes: String = ""
)

// -------------------------------------------------------------
// 5. اوراق قضایی و قراردادها (Documents)
// -------------------------------------------------------------
enum class DocumentCategory(val key: String, val titleFa: String) {
    PETITION("petition", "دادخواست"),
    COMPLAINT("complaint", "شکواییه"),
    DECLARATION("declaration", "اظهارنامه رسمی"),
    DEFENSE_BRIEF("defense_brief", "لایحه دفاعیه"),
    CONTRACT("contract", "قرارداد")
}

data class JudicialDocument(
    val id: String,
    val category: DocumentCategory,
    val title: String,
    val fullText: String,
    val createdAt: String
)

// -------------------------------------------------------------
// 6. مهلت‌ها و مواعد قضایی (Deadlines)
// -------------------------------------------------------------
data class JudicialDeadline(
    val id: String,
    val caseNumber: String,
    val title: String,
    val daysRemaining: Int,
    val dueDate: String,
    val isCompleted: Boolean = false
)

// -------------------------------------------------------------
// 7. پلن‌های اشتراک (Billing)
// -------------------------------------------------------------
data class BillingPlan(
    val id: String,
    val title: String,
    val priceToman: Long,
    val durationDays: Int,
    val features: List<String>
)
