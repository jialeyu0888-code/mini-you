package com.miniyou.app.ai

import android.content.Context
import android.net.Uri
import com.miniyou.app.data.DateFmt
import com.miniyou.app.data.ImageStore
import com.miniyou.app.data.PregnancyInfo
import com.miniyou.app.data.SettingsStore
import com.miniyou.app.data.db.Indicator
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.data.lmpFromDueDate
import com.miniyou.app.data.lmpFromGestation
import com.miniyou.app.data.parseDateOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class CheckupDraft(
    val date: LocalDate?,
    val hospital: String,
    val gestationalWeek: String,
    val checkupType: String,
    val weightKg: String,
    val bpSystolic: String,
    val bpDiastolic: String,
    val fetalHeartRate: String,
    val indicators: List<Indicator>,
    val doctorAdvice: String,
    val nextCheckupDate: LocalDate?,
    val nextCheckupNote: String,
    val summary: String,
)

data class ShoppingDraft(
    val name: String,
    val category: String,
    val status: Int,
    val price: Double?,
    val quantity: Int,
    val channel: String,
    val note: String,
)

data class DiaryDraft(val mood: String, val tags: List<String>, val content: String)

data class CheckinDraft(val name: String, val emoji: String, val note: String, val reminderMinutes: Int?)

data class PregnancyDraft(val lmp: LocalDate?, val source: String)

private const val BASE_SYSTEM = "你是一名细心的中文孕期健康助理，擅长从照片和截图中准确提取信息。" +
    "只根据图片中能看到的内容作答，看不清或没有的信息填空字符串或 null，绝不编造。" +
    "必须只输出一个合法的 JSON 对象，不要输出任何解释或 Markdown。"

class AiService(private val context: Context, private val settings: SettingsStore) {

    private val key get() = settings.current.effectiveApiKey

    private suspend fun images(uris: List<Uri>, maxSide: Int = 2048): List<ByteArray> {
        val list = uris.take(5).mapNotNull { ImageStore.loadJpeg(context, it, maxSide) }
        if (list.isEmpty()) throw AiException("图片读取失败，请重新选择")
        return list
    }

    private fun today() = LocalDate.now().format(DateFmt)

    suspend fun parseCheckup(uris: List<Uri>, info: PregnancyInfo?): CheckupDraft {
        val context = buildString {
            append("今天是 ${today()}。")
            if (info != null) append("用户末次月经为 ${info.lmp.format(DateFmt)}，预产期 ${info.dueDate.format(DateFmt)}，可用于推算检查当天孕周。")
        }
        val prompt = """
            $context
            这是孕妇的产检单/化验单/B超报告照片（可能有多张，属于同一次产检）。请提取信息，按以下 JSON 结构返回：
            {
              "date": "检查日期 YYYY-MM-DD，没有则 null",
              "hospital": "医院名称",
              "gestationalWeek": "检查时孕周，格式如 20+3；单上没有但能由日期和末次月经推算时给出推算值",
              "checkupType": "本次检查项目概括，如 NT检查、大排畸、糖耐量、常规产检",
              "weightKg": "体重数字(kg)",
              "bpSystolic": "收缩压数字",
              "bpDiastolic": "舒张压数字",
              "fetalHeartRate": "胎心率数字(次/分)",
              "indicators": [
                {"category": "血常规|尿常规|B超|生化|血糖|甲功|其他", "name": "指标中文名(可附英文缩写)", "value": "结果", "unit": "单位", "reference": "参考范围", "flag": "偏高|偏低|异常|空字符串"}
              ],
              "doctorAdvice": "医生建议/诊断意见/处理意见，原文概括",
              "nextCheckupDate": "下次产检/复诊日期 YYYY-MM-DD；若写的是“2周后复查”等相对时间，请基于检查日期换算；没有则 null",
              "nextCheckupNote": "下次产检需要做的项目或注意事项",
              "summary": "用一两句温和、通俗的话总结本次结果，有异常项时提醒遵医嘱复查"
            }
            B超数据（双顶径BPD、头围HC、腹围AC、股骨长FL、羊水指数AFI、胎盘位置/成熟度、胎方位等）请放入 indicators，category 为 B超。
            化验单上带 ↑/↓/H/L 标记或超出参考范围的，flag 填 偏高/偏低。只输出 JSON。
        """.trimIndent()
        val o = ZhipuClient.visionJson(key, BASE_SYSTEM, prompt, images(uris, 2560), reasoningEffort = "high")
        return CheckupDraft(
            date = parseDateOrNull(o.s("date")),
            hospital = o.s("hospital"),
            gestationalWeek = o.s("gestationalWeek").replace("周", "").replace("天", "").replace(" ", ""),
            checkupType = o.s("checkupType"),
            weightKg = o.num("weightKg"),
            bpSystolic = o.num("bpSystolic"),
            bpDiastolic = o.num("bpDiastolic"),
            fetalHeartRate = o.num("fetalHeartRate"),
            indicators = o.optJSONArray("indicators").objects {
                Indicator(
                    category = it.s("category").ifBlank { "其他" },
                    name = it.s("name"),
                    value = it.s("value"),
                    unit = it.s("unit"),
                    reference = it.s("reference"),
                    flag = it.s("flag").takeIf { f -> f in setOf("偏高", "偏低", "异常") } ?: "",
                )
            }.filter { it.name.isNotBlank() },
            doctorAdvice = o.s("doctorAdvice"),
            nextCheckupDate = parseDateOrNull(o.s("nextCheckupDate")),
            nextCheckupNote = o.s("nextCheckupNote"),
            summary = o.s("summary"),
        )
    }

    suspend fun parseShopping(uris: List<Uri>, categories: List<String>): List<ShoppingDraft> {
        val prompt = """
            今天是 ${today()}。这是一张商品照片，或网购订单/购物小票截图，用于孕期与宝宝用品购物记录。请识别其中的商品，返回：
            {
              "items": [
                {
                  "name": "简洁的商品名（去掉营销词，保留品牌+品类+关键规格，如“贝亲 宽口径玻璃奶瓶 240ml”）",
                  "category": "从以下分类中选最合适的一个：${categories.joinToString("、")}",
                  "status": "bought（订单/小票/已付款截图）| decided（购物车、待付款）| planned（单纯的商品照片或种草）",
                  "price": 单价数字或 null（实付价优先）,
                  "quantity": 数量整数（默认 1）,
                  "channel": "购买渠道，如 淘宝、天猫、京东、拼多多、抖音、母婴店、超市；看不出填空字符串",
                  "note": "规格、颜色、尺码等补充信息"
                }
              ]
            }
            订单里有多件商品时逐一列出；忽略运费、优惠券等非商品行。只输出 JSON。
        """.trimIndent()
        val o = ZhipuClient.visionJson(key, BASE_SYSTEM, prompt, images(uris))
        val items = o.optJSONArray("items").objects { it }.ifEmpty { listOf(o) }
        return items.map {
            val cat = it.s("category")
            ShoppingDraft(
                name = it.s("name"),
                category = categories.firstOrNull { c -> c == cat } ?: categories.firstOrNull { c -> cat.contains(c) || c.contains(cat) && cat.isNotBlank() } ?: categories.lastOrNull().orEmpty(),
                status = when (it.s("status")) {
                    "bought" -> ShoppingStatus.BOUGHT
                    "decided" -> ShoppingStatus.DECIDED
                    else -> ShoppingStatus.PLANNED
                },
                price = it.num("price").toDoubleOrNull(),
                quantity = it.num("quantity").toDoubleOrNull()?.toInt()?.coerceAtLeast(1) ?: 1,
                channel = it.s("channel"),
                note = it.s("note"),
            )
        }.filter { it.name.isNotBlank() }
    }

    suspend fun parseDiary(uris: List<Uri>, moods: List<String>, info: PregnancyInfo?): DiaryDraft {
        val prompt = """
            ${info?.let { "我现在${it.weekLabel}。" } ?: ""}这是我今天拍的照片，请以准妈妈的第一人称，帮我写一段温柔、真实、不浮夸的孕期日记草稿，返回：
            {
              "mood": "从以下心情中选一个最贴切的：${moods.joinToString("、")}",
              "tags": ["2-4 个简短标签，如 散步、美食、胎动、产检、家人、宝宝衣物"],
              "content": "80-160 字的日记正文，描述照片中的场景和感受"
            }
            只输出 JSON。
        """.trimIndent()
        val o = ZhipuClient.visionJson(key, BASE_SYSTEM, prompt, images(uris, 1280))
        return DiaryDraft(
            mood = o.s("mood").let { m -> moods.firstOrNull { it == m } ?: moods.first() },
            tags = o.optJSONArray("tags").strings().map { it.trim() }.filter { it.isNotBlank() }.take(5),
            content = o.s("content"),
        )
    }

    suspend fun parseCheckinItem(uris: List<Uri>): CheckinDraft {
        val prompt = """
            这是孕期营养补充剂/药品的包装或说明书照片，用于创建每日打卡项。返回：
            {
              "name": "简短名称，如 叶酸、钙片、DHA、维生素D3、铁剂、复合维生素（可带品牌）",
              "emoji": "一个合适的 emoji",
              "note": "包装上写的用法用量，如 每日1次，每次1粒，随餐服用；注明以医嘱为准",
              "time": "建议服用时间 HH:mm，看不出则 null"
            }
            只输出 JSON。
        """.trimIndent()
        val o = ZhipuClient.visionJson(key, BASE_SYSTEM, prompt, images(uris, 1600))
        val minutes = Regex("""(\d{1,2}):(\d{2})""").find(o.s("time"))?.let {
            it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()
        }
        return CheckinDraft(o.s("name"), o.s("emoji").ifBlank { "💊" }, o.s("note"), minutes)
    }

    suspend fun parsePregnancyDates(uris: List<Uri>): PregnancyDraft {
        val prompt = """
            今天是 ${today()}。这是孕妇的 B超单/产检单/建档资料照片。请找出可用于计算孕周的信息，返回：
            {
              "lmp": "单据上写明的末次月经日期 YYYY-MM-DD 或 null",
              "dueDate": "单据上写明的预产期 YYYY-MM-DD 或 null",
              "examDate": "检查日期 YYYY-MM-DD 或 null",
              "gestationalWeek": "检查时孕周，如 12+3，或 null"
            }
            每个字段只填写单据上明确印出的内容，不要自己推算或换算日期，没有就填 null。只输出 JSON。
        """.trimIndent()
        val o = ZhipuClient.visionJson(key, BASE_SYSTEM, prompt, images(uris))
        parseDateOrNull(o.s("lmp"))?.let { return PregnancyDraft(it, "末次月经 ${it.format(DateFmt)}") }
        parseDateOrNull(o.s("dueDate"))?.let { return PregnancyDraft(lmpFromDueDate(it), "预产期 ${it.format(DateFmt)}") }
        val exam = parseDateOrNull(o.s("examDate"))
        val ga = o.s("gestationalWeek")
        if (exam != null && ga.isNotBlank()) {
            lmpFromGestation(exam, ga)?.let { return PregnancyDraft(it, "$exam 检查时孕 $ga 周") }
        }
        throw AiException("没有在图片中找到末次月经、预产期或孕周信息")
    }
}

private fun JSONObject.s(key: String): String =
    if (!has(key) || isNull(key)) "" else optString(key).trim().let { if (it == "null") "" else it }

private fun JSONObject.num(key: String): String =
    s(key).let { Regex("""-?\d+(\.\d+)?""").find(it)?.value ?: "" }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }

private inline fun <T> JSONArray?.objects(map: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.let(map) }
