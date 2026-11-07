package com.miniyou.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val PREGNANCY_DAYS = 280

data class PregnancyInfo(val lmp: LocalDate, val today: LocalDate = LocalDate.now()) {
    val dueDate: LocalDate get() = lmp.plusDays(PREGNANCY_DAYS.toLong())
    val totalDays: Int get() = ChronoUnit.DAYS.between(lmp, today).toInt().coerceAtLeast(0)
    val weeks: Int get() = totalDays / 7
    val days: Int get() = totalDays % 7
    val daysToDue: Int get() = ChronoUnit.DAYS.between(today, dueDate).toInt()
    val progress: Float get() = (totalDays / PREGNANCY_DAYS.toFloat()).coerceIn(0f, 1f)
    val trimesterId: String get() = trimesterOf(weeks)
    val trimesterLabel: String get() = when (trimesterId) {
        "early" -> "孕早期"
        "mid" -> "孕中期"
        else -> "孕晚期"
    }
    val weekLabel: String get() = "孕 $weeks 周 + $days 天"

    fun gestationAt(date: LocalDate): String {
        val d = ChronoUnit.DAYS.between(lmp, date).toInt()
        if (d < 0) return ""
        return "${d / 7}+${d % 7}"
    }
}

fun trimesterOf(weeks: Int) = when {
    weeks < 14 -> "early"
    weeks < 28 -> "mid"
    else -> "late"
}

fun lmpFromDueDate(due: LocalDate): LocalDate = due.minusDays(PREGNANCY_DAYS.toLong())

/** 根据检查日期与当时孕周（如 "20+3"）反推末次月经。 */
fun lmpFromGestation(examDate: LocalDate, gestation: String): LocalDate? {
    val m = Regex("""(\d{1,2})\s*(?:周|w|W)?\s*(?:\+\s*(\d))?""").find(gestation) ?: return null
    val w = m.groupValues[1].toIntOrNull() ?: return null
    val d = m.groupValues[2].toIntOrNull() ?: 0
    if (w > 45) return null
    return examDate.minusDays((w * 7 + d).toLong())
}

val DateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val CnDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
val ShortCnFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")

fun LocalDate.cn(): String = format(CnDateFmt)
fun LocalDate.shortCn(): String = format(ShortCnFmt)
fun Long.toDate(): LocalDate = LocalDate.ofEpochDay(this)

private val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
fun LocalDate.weekdayCn(): String = weekDays[dayOfWeek.value - 1]

fun parseDateOrNull(s: String?): LocalDate? {
    if (s.isNullOrBlank()) return null
    val norm = s.trim().replace(Regex("[年/.]"), "-").replace("月", "-").replace("日", "")
    return runCatching { LocalDate.parse(norm, DateFmt) }.getOrNull()
        ?: runCatching {
            val p = norm.split("-").map { it.trim().toInt() }
            LocalDate.of(p[0], p[1], p[2])
        }.getOrNull()
}

data class BabySize(val week: Int, val fruit: String, val emoji: String, val length: String, val weight: String, val tip: String)

/** 各孕周宝宝大小的通俗比喻（数据为常见参考值）。 */
val BabySizes = listOf(
    BabySize(4, "罂粟籽", "🌱", "约 0.1 厘米", "<1 克", "受精卵刚刚着床，记得坚持补充叶酸。"),
    BabySize(5, "芝麻", "🌱", "约 0.2 厘米", "<1 克", "心脏开始形成，可能出现早孕反应。"),
    BabySize(6, "小扁豆", "🫘", "约 0.5 厘米", "<1 克", "B 超可能看到胎芽和原始心管搏动。"),
    BabySize(7, "蓝莓", "🫐", "约 1 厘米", "约 1 克", "孕吐较常见，少量多餐会舒服些。"),
    BabySize(8, "树莓", "🍓", "约 1.6 厘米", "约 1 克", "手指脚趾开始分化。"),
    BabySize(9, "樱桃", "🍒", "约 2.3 厘米", "约 2 克", "胚胎期即将结束，正式成为胎儿。"),
    BabySize(10, "金桔", "🍊", "约 3 厘米", "约 4 克", "重要器官基本形成，注意避免接触有害物质。"),
    BabySize(11, "无花果", "🟣", "约 4 厘米", "约 7 克", "可以预约 NT 检查啦。"),
    BabySize(12, "青柠", "🍋", "约 5.4 厘米", "约 14 克", "NT 检查窗口期（11–13+6 周）。"),
    BabySize(13, "柠檬", "🍋", "约 7 厘米", "约 23 克", "即将进入舒适的孕中期。"),
    BabySize(14, "桃子", "🍑", "约 8.7 厘米", "约 43 克", "孕吐通常逐渐缓解，胃口会变好。"),
    BabySize(15, "苹果", "🍎", "约 10 厘米", "约 70 克", "注意唐筛/无创 DNA 的时间。"),
    BabySize(16, "牛油果", "🥑", "约 11.6 厘米", "约 100 克", "宝宝开始会做小动作。"),
    BabySize(17, "石榴", "🍎", "约 13 厘米", "约 140 克", "脂肪开始积累，妈妈体重稳步增长。"),
    BabySize(18, "甜椒", "🫑", "约 14 厘米", "约 190 克", "有的妈妈开始感到胎动。"),
    BabySize(19, "芒果", "🥭", "约 15 厘米", "约 240 克", "宝宝的感官快速发育。"),
    BabySize(20, "香蕉", "🍌", "约 16.5 厘米（头臀）", "约 300 克", "孕期过半！准备大排畸检查。"),
    BabySize(21, "胡萝卜", "🥕", "约 26.7 厘米", "约 360 克", "胎动越来越明显。"),
    BabySize(22, "木瓜", "🍈", "约 27.8 厘米", "约 430 克", "宝宝能听到妈妈的声音了，多和 TA 说说话。"),
    BabySize(23, "葡萄柚", "🍊", "约 28.9 厘米", "约 500 克", "注意控制体重增长速度。"),
    BabySize(24, "玉米", "🌽", "约 30 厘米", "约 600 克", "糖耐量检查（24–28 周）别错过。"),
    BabySize(25, "花椰菜", "🥦", "约 34.6 厘米", "约 660 克", "宝宝开始有规律的作息。"),
    BabySize(26, "生菜", "🥬", "约 35.6 厘米", "约 760 克", "宝宝的眼睛开始睁开。"),
    BabySize(27, "卷心菜", "🥬", "约 36.6 厘米", "约 875 克", "孕中期最后一周，开始准备待产包吧。"),
    BabySize(28, "茄子", "🍆", "约 37.6 厘米", "约 1 千克", "进入孕晚期，开始数胎动。"),
    BabySize(29, "南瓜", "🎃", "约 38.6 厘米", "约 1.2 千克", "产检频率变为两周一次。"),
    BabySize(30, "椰子", "🥥", "约 39.9 厘米", "约 1.3 千克", "注意休息，左侧卧睡更舒服。"),
    BabySize(31, "菠萝", "🍍", "约 41.1 厘米", "约 1.5 千克", "宝宝的大脑快速发育。"),
    BabySize(32, "哈密瓜", "🍈", "约 42.4 厘米", "约 1.7 千克", "可能出现假性宫缩，注意区分。"),
    BabySize(33, "菠萝蜜", "🍈", "约 43.7 厘米", "约 1.9 千克", "宝宝的骨骼逐渐变硬。"),
    BabySize(34, "香瓜", "🍈", "约 45 厘米", "约 2.1 千克", "胎心监护开始常规化。"),
    BabySize(35, "蜜瓜", "🍈", "约 46.2 厘米", "约 2.4 千克", "GBS 筛查（35–37 周）。"),
    BabySize(36, "长白菜", "🥬", "约 47.4 厘米", "约 2.6 千克", "产检改为每周一次，待产包准备好了吗？"),
    BabySize(37, "冬瓜", "🍈", "约 48.6 厘米", "约 2.9 千克", "宝宝已足月，随时可能发动。"),
    BabySize(38, "韭葱", "🥬", "约 49.8 厘米", "约 3.1 千克", "留意见红、破水、规律宫缩。"),
    BabySize(39, "小西瓜", "🍉", "约 50.7 厘米", "约 3.3 千克", "保持放松，随时准备迎接宝宝。"),
    BabySize(40, "西瓜", "🍉", "约 51.2 厘米", "约 3.5 千克", "预产期到啦！超过 41 周请听从医生安排。"),
)

fun babySizeFor(week: Int): BabySize? =
    if (week < 4) null else BabySizes.lastOrNull { it.week <= week.coerceAtMost(40) }

data class ScheduleEntry(
    val startWeek: Int,
    val endWeek: Int,
    val range: String,
    val title: String,
    val items: List<String>,
    val note: String = "",
)

/** 产检时间表参考（依据《孕前和孕期保健指南》常见安排整理，以医院实际安排为准）。 */
val CheckupSchedule = listOf(
    ScheduleEntry(6, 13, "6–13+6 周", "第 1 次产检 · 建档",
        listOf("确认宫内妊娠、核对孕周（早孕 B 超）", "血常规、尿常规、血型（ABO+Rh）", "肝肾功能、空腹血糖、甲功", "乙肝、梅毒、HIV 筛查", "建立《母子健康手册》"),
        "建档一般需要带身份证、医保卡等，具体以医院要求为准。"),
    ScheduleEntry(11, 13, "11–13+6 周", "NT 检查",
        listOf("超声测量胎儿颈项透明层厚度（NT）", "早孕期唐氏综合征风险评估"),
        "错过窗口期就无法补做，记得提前预约。"),
    ScheduleEntry(14, 19, "14–19+6 周", "第 2 次产检 · 唐筛",
        listOf("中孕期血清学筛查（唐筛）", "无创 DNA（NIPT，12–22+6 周可做）", "血压、体重、宫高、胎心"),
        "高龄或唐筛高风险时，医生可能建议羊水穿刺。"),
    ScheduleEntry(20, 24, "20–24 周", "第 3 次产检 · 大排畸",
        listOf("系统超声（大排畸）筛查胎儿结构", "血常规、尿常规", "血压、体重、宫高、腹围、胎心")),
    ScheduleEntry(24, 28, "24–28 周", "第 4 次产检 · 糖耐",
        listOf("75g 口服葡萄糖耐量试验（OGTT）", "血常规、尿常规", "血压、体重、宫高、胎心"),
        "检查前一晚 22 点后禁食，当天早上空腹抽血。"),
    ScheduleEntry(28, 31, "28–31+6 周", "第 5 次产检",
        listOf("产科超声（小排畸）", "血常规、尿常规", "Rh 阴性者复查抗体", "开始自数胎动"),
        "孕 28 周后产检通常每 2 周一次。"),
    ScheduleEntry(32, 36, "32–36+6 周", "第 6–7 次产检",
        listOf("胎心监护（NST）", "B 超评估胎位、羊水、胎盘", "血常规、尿常规", "35–37 周 GBS（B 族链球菌）筛查")),
    ScheduleEntry(37, 41, "37–41 周", "第 8–11 次产检（每周 1 次）",
        listOf("每周胎心监护", "B 超评估羊水、胎儿大小", "评估宫颈条件、确定分娩方式", "超过 41 周需住院引产评估"),
        "出现规律宫缩、破水、见红、胎动异常请立即就医。"),
)

/** 当前孕周所处（或即将到来）的产检节点；重叠时取开始更晚、更具体的一项。 */
fun scheduleFor(week: Int): ScheduleEntry? =
    CheckupSchedule.filter { week in it.startWeek..it.endWeek }.maxByOrNull { it.startWeek }
        ?: CheckupSchedule.firstOrNull { it.startWeek > week }
