package com.miniyou.app.data.content

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Label(val id: String, val label: String, val description: String = "")

data class Trimester(val id: String, val label: String, val weekRange: String, val startWeek: Int, val endWeek: Int)

data class RemedyMethod(
    val id: String,
    val name: String,
    val type: String,
    val evidence: String,
    val requiresDoctor: Boolean,
    val rxLabel: String?,
    val howTo: String,
    val cautions: List<String>,
) {
    val isMedication: Boolean get() = type == "medication" || requiresDoctor || rxLabel != null
}

data class Remedy(
    val id: String,
    val symptom: String,
    val aliases: List<String>,
    val category: String,
    val trimesters: List<String>,
    val trimesterNote: String,
    val overview: String,
    val methods: List<RemedyMethod>,
    val whenToSeeDoctor: List<String>,
) {
    val medicationCount: Int get() = methods.count { it.isMedication }

    fun matches(q: String): Boolean {
        if (q.isBlank()) return true
        val k = q.trim()
        return symptom.contains(k, true) || aliases.any { it.contains(k, true) } ||
            overview.contains(k, true) || methods.any { it.name.contains(k, true) || it.howTo.contains(k, true) }
    }
}

data class RemedyContent(
    val title: String,
    val disclaimer: String,
    val medicationNotice: String,
    val medicationNoticeDetail: String,
    val evidenceLevels: List<Label>,
    val methodTypes: List<Label>,
    val trimesters: List<Trimester>,
    val categories: List<Label>,
    val generalEmergencySigns: List<String>,
    val remedies: List<Remedy>,
) {
    fun evidence(id: String) = evidenceLevels.firstOrNull { it.id == id }
    fun typeLabel(id: String) = methodTypes.firstOrNull { it.id == id }?.label ?: id
    fun categoryLabel(id: String) = categories.firstOrNull { it.id == id }?.label ?: id
    fun trimesterLabel(id: String) = trimesters.firstOrNull { it.id == id }?.label ?: id
}

data class LessonStep(val order: Int, val title: String, val content: String)
data class Myth(val myth: String, val fact: String)
data class LessonTable(val title: String, val columns: List<String>, val rows: List<List<String>>, val note: String)

data class Lesson(
    val id: String,
    val chapterId: String,
    val order: Int,
    val title: String,
    val audience: String,
    val minMonths: Int,
    val maxMonths: Int,
    val ageLabel: String,
    val readMinutes: Int,
    val summary: String,
    val steps: List<LessonStep>,
    val tips: List<String>,
    val myths: List<Myth>,
    val whenToSeeDoctor: List<String>,
    val table: LessonTable?,
    val tags: List<String>,
) {
    fun matches(q: String): Boolean {
        if (q.isBlank()) return true
        val k = q.trim()
        return title.contains(k, true) || summary.contains(k, true) || tags.any { it.contains(k, true) } ||
            steps.any { it.title.contains(k, true) || it.content.contains(k, true) }
    }
}

data class Chapter(val id: String, val order: Int, val title: String, val description: String, val lessonIds: List<String>)

data class ClassContent(
    val title: String,
    val disclaimer: String,
    val emergencyNotice: String,
    val chapters: List<Chapter>,
    val lessons: List<Lesson>,
) {
    fun lesson(id: String) = lessons.firstOrNull { it.id == id }
    fun lessonsOf(chapter: Chapter) = chapter.lessonIds.mapNotNull(::lesson)
}

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }

private inline fun <T> JSONArray?.objects(map: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.let(map) }

private fun JSONObject.str(key: String) = if (isNull(key)) "" else optString(key)

/** 读取 assets/content 下的内置知识内容，结构见仓库 README。 */
class ContentRepository(private val context: Context) {
    val remedies: RemedyContent by lazy { parseRemedies(read("content/remedies.json")) }
    val classes: ClassContent by lazy { parseClasses(read("content/classes.json")) }

    private fun read(path: String) = context.assets.open(path).bufferedReader().use { JSONObject(it.readText()) }

    private fun labels(arr: JSONArray?) = arr.objects { Label(it.str("id"), it.str("label"), it.str("description")) }

    private fun parseRemedies(o: JSONObject) = RemedyContent(
        title = o.str("title"),
        disclaimer = o.str("disclaimer"),
        medicationNotice = o.str("medicationNotice").ifBlank { "需遵医嘱" },
        medicationNoticeDetail = o.str("medicationNoticeDetail"),
        evidenceLevels = labels(o.optJSONArray("evidenceLevels")),
        methodTypes = labels(o.optJSONArray("methodTypes")),
        trimesters = o.optJSONArray("trimesters").objects {
            Trimester(it.str("id"), it.str("label"), it.str("weekRange"), it.optInt("startWeek"), it.optInt("endWeek"))
        },
        categories = labels(o.optJSONArray("categories")),
        generalEmergencySigns = o.optJSONArray("generalEmergencySigns").strings(),
        remedies = o.optJSONArray("remedies").objects { r ->
            Remedy(
                id = r.str("id"),
                symptom = r.str("symptom"),
                aliases = r.optJSONArray("aliases").strings(),
                category = r.str("category"),
                trimesters = r.optJSONArray("trimesters").strings(),
                trimesterNote = r.str("trimesterNote"),
                overview = r.str("overview"),
                methods = r.optJSONArray("methods").objects { m ->
                    RemedyMethod(
                        id = m.str("id"),
                        name = m.str("name"),
                        type = m.str("type"),
                        evidence = m.str("evidence"),
                        requiresDoctor = m.optBoolean("requiresDoctor"),
                        rxLabel = m.str("rxLabel").ifBlank { null },
                        howTo = m.str("howTo"),
                        cautions = m.optJSONArray("cautions").strings(),
                    )
                },
                whenToSeeDoctor = r.optJSONArray("whenToSeeDoctor").strings(),
            )
        },
    )

    private fun parseClasses(o: JSONObject) = ClassContent(
        title = o.str("title"),
        disclaimer = o.str("disclaimer"),
        emergencyNotice = o.str("emergencyNotice"),
        chapters = o.optJSONArray("chapters").objects {
            Chapter(it.str("id"), it.optInt("order"), it.str("title"), it.str("description"), it.optJSONArray("lessonIds").strings())
        }.sortedBy { it.order },
        lessons = o.optJSONArray("lessons").objects { l ->
            val age = l.optJSONObject("ageRange")
            Lesson(
                id = l.str("id"),
                chapterId = l.str("chapterId"),
                order = l.optInt("order"),
                title = l.str("title"),
                audience = l.str("audience"),
                minMonths = age?.optInt("minMonths") ?: 0,
                maxMonths = age?.optInt("maxMonths") ?: 6,
                ageLabel = age?.str("label").orEmpty(),
                readMinutes = l.optInt("readMinutes"),
                summary = l.str("summary"),
                steps = l.optJSONArray("steps").objects { LessonStep(it.optInt("order"), it.str("title"), it.str("content")) }
                    .sortedBy { it.order },
                tips = l.optJSONArray("tips").strings(),
                myths = l.optJSONArray("myths").objects { Myth(it.str("myth"), it.str("fact")) },
                whenToSeeDoctor = l.optJSONArray("whenToSeeDoctor").strings(),
                table = l.optJSONObject("table")?.let { t ->
                    val rows = t.optJSONArray("rows")
                    LessonTable(
                        title = t.str("title"),
                        columns = t.optJSONArray("columns").strings(),
                        rows = if (rows == null) emptyList() else (0 until rows.length()).map { rows.optJSONArray(it).strings() },
                        note = t.str("note"),
                    )
                },
                tags = l.optJSONArray("tags").strings(),
            )
        },
    )
}
