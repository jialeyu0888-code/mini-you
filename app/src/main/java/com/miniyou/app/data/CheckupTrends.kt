package com.miniyou.app.data

import com.miniyou.app.data.db.Checkup
import com.miniyou.app.data.db.Indicator
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

internal enum class FetalMeasure(val label: String, private val aliases: Set<String>) {
    AC("腹围", setOf("腹围")),
    HC("头围", setOf("头围")),
    FL("股骨长", setOf("股骨长", "股骨长度", "股骨长径", "股骨径")),
    BPD("双顶径", setOf("双顶径", "头双顶径"));

    fun centimeters(indicator: Indicator): Double? {
        val name = normalized(indicator.name)
        val lengthUnit = nameLengthUnit.find(name)
        val key = name.removeSuffix(lengthUnit?.value.orEmpty())
            .replace(nameSeparators, "")
            .removePrefix("胎儿")
            .removePrefix("胎")
        val abbreviation = this.name.lowercase(Locale.ROOT)
        if (key != abbreviation && aliases.none { key == it || key == it + abbreviation || key == abbreviation + it }) {
            return null
        }
        val value = measurement.matchEntire(normalized(indicator.value)) ?: return null
        val enteredUnit = lengthScale(normalized(indicator.unit))
        if (indicator.unit.isNotBlank() && enteredUnit == null) return null
        val embeddedUnit = lengthScale(value.groupValues[2])
        if (value.groupValues[2].isNotBlank() && embeddedUnit == null) return null
        val units = listOfNotNull(enteredUnit, embeddedUnit, lengthScale(lengthUnit?.groupValues?.get(1).orEmpty()))
        val scale = units.firstOrNull() ?: return null
        if (units.any { it != scale }) return null
        return positive(value.groupValues[1])?.times(scale)
    }
}

private val nameLengthUnit = Regex("""\s*\((mm|cm|毫米|厘米)\)\s*$""")
private val nameSeparators = Regex("""[\s():_-]""")
private val measurement = Regex("""([0-9]+(?:\.[0-9]+)?|\.[0-9]+)\s*([^0-9\s]*)""")
private val gestation = Regex("""(?:孕\s*)?([0-9]{1,2})\s*(?:周|w)?\s*(?:\+\s*([0-9])\s*(?:天|d)?|([0-9])\s*(?:天|d))?""")

private fun normalized(text: String) =
    Normalizer.normalize(text, Normalizer.Form.NFKC).trim().lowercase(Locale.ROOT)

private fun lengthScale(unit: String): Double? = when (unit) {
    "cm", "厘米" -> 1.0
    "mm", "毫米" -> 0.1
    else -> null
}

private fun positive(number: String): Double? =
    number.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it < 10_000 }

private fun valueIn(text: String, units: Set<String>): Double? {
    val match = measurement.matchEntire(normalized(text)) ?: return null
    return positive(match.groupValues[1]).takeIf { match.groupValues[2] in units }
}

internal fun gestationDays(text: String): Int? {
    val match = gestation.matchEntire(normalized(text)) ?: return null
    val weeks = match.groupValues[1].toInt()
    val days = match.groupValues[2].ifEmpty { match.groupValues[3] }.toIntOrNull() ?: 0
    return (weeks * 7 + days).takeIf { weeks <= 45 && days <= 6 }
}

internal data class CheckupReading(
    val record: Checkup,
    val gestationDays: Int?,
    val weight: Double?,
    val systolic: Double?,
    val diastolic: Double?,
    val heartRate: Double?,
    val fetal: Map<FetalMeasure, Double>,
) {
    val week: String? get() = gestationDays?.let { "${it / 7}+${it % 7} 周" }
}

internal fun checkupReadings(records: List<Checkup>, lmp: Long?): List<CheckupReading> = records
    .filter { lmp == null || it.date >= lmp }
    .sortedWith(compareBy(Checkup::date, Checkup::id))
    .map { record ->
        val days = gestationDays(record.gestationalWeek)
            ?: lmp?.let { (record.date - it).takeIf { elapsed -> elapsed in 0..321 }?.toInt() }
        CheckupReading(
            record = record,
            gestationDays = days,
            weight = valueIn(record.weightKg, setOf("", "kg", "千克", "公斤")),
            systolic = valueIn(record.bpSystolic, setOf("", "mmhg")),
            diastolic = valueIn(record.bpDiastolic, setOf("", "mmhg")),
            heartRate = valueIn(record.fetalHeartRate, setOf("", "bpm", "次/分", "次/分钟")),
            fetal = FetalMeasure.entries.mapNotNull { measure ->
                val values = record.indicators.mapNotNull(measure::centimeters)
                val first = values.firstOrNull()
                if (first != null && values.all { abs(it - first) < 0.0001 }) measure to first else null
            }.toMap(),
        )
    }

internal data class FetalReference(val median: Double, val deviation: Double) {
    fun zScore(centimeters: Double): Double = (centimeters - median) / deviation
}

internal val fetalReferenceDays = 14 * 7..40 * 7

/** Papageorghiou et al., Lancet 2014, Table 2, doi:10.1016/S0140-6736(14)61490-2. */
internal fun fetalReference(measure: FetalMeasure, days: Int): FetalReference? {
    if (days !in fetalReferenceDays) return null
    val ga = days / 7.0
    val square = ga * ga
    val cube = square * ga
    val log = ln(ga)
    val (meanMm, sdMm) = when (measure) {
        FetalMeasure.AC -> Pair(
            -81.3243 + 11.6772 * ga - 0.000561865 * cube,
            -4.36302 + 0.121445 * square - 0.0130256 * cube + 0.00282143 * cube * log,
        )
        FetalMeasure.HC -> Pair(
            -28.2849 + 1.69267 * square - 0.397485 * square * log,
            1.98735 + 0.0136772 * cube - 0.00726264 * cube * log + 0.000976253 * cube * log * log,
        )
        FetalMeasure.FL -> Pair(
            -39.9616 + 4.32298 * ga - 0.0380156 * square,
            exp(0.605843 - 42.0014 / square + 0.00000917972 * cube),
        )
        FetalMeasure.BPD -> Pair(
            5.60878 + 0.158369 * square - 0.00256379 * cube,
            exp(0.101242 + 0.00150557 * cube - 0.000771535 * cube * log + 0.0000999638 * cube * log * log),
        )
    }
    return FetalReference(meanMm / 10, sdMm / 10)
}

/** Normal CDF approximation for the fitted Z-score, expressed as a percentage. */
internal fun fetalPercentile(z: Double): Double {
    val t = 1 / (1 + 0.2316419 * abs(z))
    val polynomial = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
    val tail = 0.3989422804014327 * exp(-z * z / 2) * polynomial
    return 100 * if (z < 0) tail else 1 - tail
}
