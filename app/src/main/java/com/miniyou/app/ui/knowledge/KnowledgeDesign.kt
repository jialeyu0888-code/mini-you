package com.miniyou.app.ui.knowledge

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniyou.app.R
import com.miniyou.app.ui.theme.ActionRose

internal data class KnowledgeColors(
    val paper: Color,
    val ink: Color,
    val secondaryInk: Color,
    val card: Color,
    val cardSoft: Color,
    val outline: Color,
    val accentPink: Color,
    val badgePink: Color,
    val chipSelectedBg: Color,
    val chipSelectedInk: Color,
    val tagBlueBg: Color,
    val tagBlueInk: Color,
    val tagPinkBg: Color,
    val tagPinkInk: Color,
    val tagPeachBg: Color,
    val tagPeachInk: Color,
    val tagGrayBg: Color,
    val tagGrayInk: Color,
    val rxOrange: Color,
    val dangerBg: Color,
    val dangerBorder: Color,
    val dangerInk: Color,
    val fabPinkBg: Color,
    val cautionBg: Color,
)

internal val LightKnowledgeColors = KnowledgeColors(
    paper = Color(0xFFFEF4F4),
    ink = Color(0xFF3E241E),
    secondaryInk = Color(0xFF6B4F49),
    card = Color(0xFFFFFCFB),
    cardSoft = Color(0xFFFEF2F4),
    outline = Color(0xFFE7CFCB),
    accentPink = ActionRose,
    badgePink = Color(0xFFE9728C),
    chipSelectedBg = Color(0xFFFCE2D4),
    chipSelectedInk = Color(0xFF47281F),
    tagBlueBg = Color(0xFFEAE8FC),
    tagBlueInk = Color(0xFF352C6B),
    tagPinkBg = Color(0xFFFDE4E9),
    tagPinkInk = Color(0xFF5A2531),
    tagPeachBg = Color(0xFFFEEAD7),
    tagPeachInk = Color(0xFF59311D),
    tagGrayBg = Color(0xFFF5EBEF),
    tagGrayInk = Color(0xFF5E474C),
    rxOrange = Color(0xFFE1725D),
    dangerBg = Color(0xFFFCE9EC),
    dangerBorder = Color(0xFFE8BBC2),
    dangerInk = Color(0xFF8B2E29),
    fabPinkBg = Color(0xFFFBE2EB),
    cautionBg = Color(0xFFFCEBEF),
)

internal val DarkKnowledgeColors = KnowledgeColors(
    paper = Color(0xFF211816),
    ink = Color(0xFFFFE6DC),
    secondaryInk = Color(0xFFDCC0B6),
    card = Color(0xFF2F221F),
    cardSoft = Color(0xFF392826),
    outline = Color(0xFF5D423B),
    accentPink = Color(0xFFFF98AE),
    badgePink = Color(0xFFD9647E),
    chipSelectedBg = Color(0xFF573B30),
    chipSelectedInk = Color(0xFFFFE6DC),
    tagBlueBg = Color(0xFF383457),
    tagBlueInk = Color(0xFFE2DFFF),
    tagPinkBg = Color(0xFF57323A),
    tagPinkInk = Color(0xFFFFD9E1),
    tagPeachBg = Color(0xFF573C2B),
    tagPeachInk = Color(0xFFFFE2CE),
    tagGrayBg = Color(0xFF443336),
    tagGrayInk = Color(0xFFEBD6DA),
    rxOrange = Color(0xFFD66853),
    dangerBg = Color(0xFF47262A),
    dangerBorder = Color(0xFF7A4148),
    dangerInk = Color(0xFFFFC4C8),
    fabPinkBg = Color(0xFF593542),
    cautionBg = Color(0xFF3D282C),
)

internal val LocalKnowledgeColors = compositionLocalOf { LightKnowledgeColors }
private val KnowledgeShadow = Color(0xFFC5968E).copy(alpha = 0.28f)
internal val KnowledgeDarkFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.82f, 0.76f, 0.74f, 1f) })

@Composable
internal fun KnowledgeTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkKnowledgeColors else LightKnowledgeColors
    CompositionLocalProvider(LocalKnowledgeColors provides colors, content = content)
}

internal fun Modifier.knowledgeCardShadow(shape: RoundedCornerShape): Modifier =
    fillMaxWidth().shadow(3.dp, shape, clip = false, ambientColor = KnowledgeShadow, spotColor = KnowledgeShadow)

@Composable
internal fun KnowledgeArt(
    @DrawableRes res: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val dark = isSystemInDarkTheme()
    Image(
        painter = painterResource(res),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = if (dark) 0.88f else 1f,
        colorFilter = if (dark) KnowledgeDarkFilter else null,
    )
}

@Composable
internal fun KnowledgeHeroHeader(
    title: String,
    subtitle: String? = null,
    statusInset: Dp = 0.dp,
    onBack: (() -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val colors = LocalKnowledgeColors.current
    val dark = isSystemInDarkTheme()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showArt = maxWidth >= 370.dp && LocalDensity.current.fontScale < 1.2f && trailingAction == null
        val artWidth = (maxWidth * if (onBack == null) 0.35f else 0.27f).coerceAtMost(164.dp)
        if (showArt) Box(Modifier.matchParentSize()) {
            Image(
                painter = painterResource(R.drawable.kb_hero_mom),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp)
                    .wrapContentSize(Alignment.BottomEnd, unbounded = true)
                    .size(artWidth, artWidth * (220f / 370f)),
                contentScale = ContentScale.Fit,
                alpha = if (dark) 0.76f else 1f,
                colorFilter = if (dark) KnowledgeDarkFilter else null,
            )
        }
        if (onBack == null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp + statusInset)
                    .padding(start = 20.dp, end = if (showArt) artWidth + 12.dp else 16.dp, top = statusInset + 5.dp, bottom = 6.dp),
            ) {
                Text(
                    text = title,
                    color = colors.ink,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 31.sp,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = colors.secondaryInk,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp + statusInset)
                    .padding(start = 8.dp, end = if (showArt) artWidth + 12.dp else 14.dp, top = statusInset + 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = colors.ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = title,
                    color = colors.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (trailingAction != null) {
                    trailingAction()
                }
            }
        }
    }
}

@Composable
internal fun KnowledgeTagPill(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = bg, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun KnowledgeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKnowledgeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colors.chipSelectedBg else colors.card,
        border = BorderStroke(1.dp, if (selected) colors.chipSelectedBg else colors.outline),
        modifier = modifier.height(32.dp),
    ) {
        Box(
            Modifier.padding(horizontal = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) colors.chipSelectedInk else colors.secondaryInk,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun KnowledgeAiBanner(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFFB39EF4), Color(0xFFD59AE2), Color(0xFFF09DB8)),
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier.knowledgeCardShadow(shape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(gradient, shape)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.26f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✦", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(Color.White.copy(alpha = 0.45f)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@DrawableRes
internal fun symptomArtRes(remedyId: String, category: String): Int = when (remedyId) {
    "nausea-vomiting" -> R.drawable.kb_symptom_nausea
    "bloating" -> R.drawable.kb_symptom_bloat
    "heartburn" -> R.drawable.kb_symptom_heartburn
    else -> when (category) {
        "digestive" -> R.drawable.kb_symptom_bloat
        "sleep" -> R.drawable.kb_symptom_nausea
        else -> R.drawable.kb_symptom_heartburn
    }
}
