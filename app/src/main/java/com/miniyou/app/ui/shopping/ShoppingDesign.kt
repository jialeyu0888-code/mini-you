package com.miniyou.app.ui.shopping

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.miniyou.app.R
import com.miniyou.app.data.db.ShoppingItem
import com.miniyou.app.ui.diary.DiaryDarkIllustration
import com.miniyou.app.ui.diary.DiaryPaperTexture
import com.miniyou.app.ui.diary.DiaryText
import com.miniyou.app.ui.theme.ActionRose
import com.miniyou.app.ui.theme.DarkActionRose
import com.miniyou.app.ui.theme.OnDarkActionRose
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ShopColors(
    val paper: Color,
    val ink: Color,
    val secondary: Color,
    val accent: Color,
    val card: Color,
    val pink: Color,
    val pinkSoft: Color,
    val line: Color,
    val button: Color,
    val onButton: Color,
)

internal val LightShop = ShopColors(
    paper = Color(0xFFFFF6F1),
    ink = Color(0xFF562B1E),
    secondary = Color(0xFF8A655C),
    accent = Color(0xFF9F3749),
    card = Color(0xFFFFFCF9),
    pink = Color(0xFFF7D5D6),
    pinkSoft = Color(0xFFFFF0EE),
    line = Color(0xFFF0DDD4),
    button = ActionRose,
    onButton = Color.White,
)

internal val DarkShop = ShopColors(
    paper = Color(0xFF211815),
    ink = Color(0xFFFFE5D8),
    secondary = Color(0xFFE0BDB2),
    accent = Color(0xFFFFAA9D),
    card = Color(0xFF30221E),
    pink = Color(0xFF593635),
    pinkSoft = Color(0xFF3A2824),
    line = Color(0xFF624238),
    button = DarkActionRose,
    onButton = OnDarkActionRose,
)

internal val LocalShopColors = compositionLocalOf { LightShop }

internal class ShoppingMarks(context: Context) {
    private val prefs = context.getSharedPreferences("shopping_marks", Context.MODE_PRIVATE)
    private val favoritesFlow = MutableStateFlow(read("fav"))
    private val pickedFlow = MutableStateFlow(read("pick"))
    val favorites: StateFlow<Set<Long>> = favoritesFlow.asStateFlow()
    val picked: StateFlow<Set<Long>> = pickedFlow.asStateFlow()

    private fun read(key: String): Set<Long> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    private fun write(key: String, flow: MutableStateFlow<Set<Long>>, id: Long) {
        val next = flow.value.toMutableSet().apply { if (!add(id)) remove(id) }
        prefs.edit().putStringSet(key, next.map { it.toString() }.toSet()).apply()
        flow.value = next
    }

    fun toggleFavorite(id: Long) = write("fav", favoritesFlow, id)
    fun togglePicked(id: Long) = write("pick", pickedFlow, id)
}

@Composable
internal fun rememberShoppingMarks(): ShoppingMarks {
    val context = LocalContext.current.applicationContext
    return remember { ShoppingMarks(context) }
}

fun money(v: Double) = if (v % 1.0 == 0.0) "¥%.0f".format(v) else "¥%.2f".format(v)

fun categoryEmoji(name: String) = when {
    name.contains("待产") -> "🧳"
    name.contains("服饰") || name.contains("衣") -> "👕"
    name.contains("宝宝") || name.contains("母婴") || name.contains("新生儿") -> "🍼"
    name.contains("喂养") || name.contains("营养") -> "🥛"
    name.contains("孕妇") || name.contains("妈妈") -> "🤰"
    name.contains("洗护") -> "🛁"
    name.contains("出行") -> "🚼"
    name.contains("玩具") -> "🧸"
    name.contains("其他") -> "📦"
    else -> "🏷️"
}

internal data class ShopGrid(
    val label: String,
    @DrawableRes val icon: Int,
    val match: (ShoppingItem) -> Boolean,
)

internal val ShopGrids = listOf(
    ShopGrid("孕妈服饰", R.drawable.shop_grid_clothes) {
        it.category.contains("孕妇") || it.category.contains("服饰") || it.name.contains("衣") || it.name.contains("裤")
    },
    ShopGrid("营养补充", R.drawable.shop_grid_nutrition) {
        it.category.contains("喂养") || it.name.contains("叶酸") || it.name.contains("DHA") || it.name.contains("钙") || it.name.contains("维生素")
    },
    ShopGrid("睡眠护腰", R.drawable.shop_grid_sleep) { it.name.contains("枕") || it.name.contains("腰") },
    ShopGrid("洗护护理", R.drawable.shop_grid_wash) {
        it.category.contains("洗护") || it.name.contains("护肤") || it.name.contains("乳液") || it.name.contains("巾")
    },
    ShopGrid("孕期饮食", R.drawable.shop_grid_food) { it.name.contains("食") || it.name.contains("餐") || it.category.contains("饮食") },
    ShopGrid("宝宝用品", R.drawable.shop_grid_baby) {
        it.category.contains("宝宝") || it.name.contains("奶瓶") || it.name.contains("巾")
    },
    ShopGrid("玩具早教", R.drawable.shop_grid_toy) { it.category.contains("玩具") || it.name.contains("玩具") },
    ShopGrid("纸尿裤", R.drawable.shop_grid_diaper) { it.name.contains("纸尿") || it.name.contains("尿不湿") },
    ShopGrid("待产用品", R.drawable.shop_grid_bag) { it.category.contains("待产") || it.name.contains("待产") },
    ShopGrid("全部分类", R.drawable.shop_grid_all) { true },
)

internal data class ShopSuggest(
    val name: String,
    val subtitle: String,
    val price: Int,
    @DrawableRes val art: Int,
    val category: String,
)

internal val ShopSuggests = listOf(
    ShopSuggest("孕期 DHA 藻油", "助力宝宝大脑发育", 129, R.drawable.shop_featured_dha, "喂养用品"),
    ShopSuggest("多功能孕妇枕", "托腹护腰 · 安睡整晚", 199, R.drawable.shop_featured_pillow, "孕妇用品"),
    ShopSuggest("孕期专用护肤套装", "温和安全 · 滋润保湿", 169, R.drawable.shop_featured_skincare, "洗护用品"),
)

@DrawableRes
internal fun shopArt(item: ShoppingItem): Int {
    val n = item.name
    return when {
        n.contains("枕") -> R.drawable.shop_item_pillow
        n.contains("叶酸") -> R.drawable.shop_item_folic
        n.contains("DHA") || n.contains("藻油") -> R.drawable.shop_featured_dha
        n.contains("内裤") || n.contains("裤") -> R.drawable.shop_rec_panties
        n.contains("巾") || n.contains("毛巾") -> R.drawable.shop_item_towel
        n.contains("护肤") || n.contains("乳液") || n.contains("按摩油") -> R.drawable.shop_featured_skincare
        n.contains("纸尿") -> R.drawable.shop_grid_diaper
        n.contains("奶瓶") -> R.drawable.shop_grid_baby
        n.contains("玩具") -> R.drawable.shop_grid_toy
        n.contains("衣") || n.contains("服") -> R.drawable.shop_grid_clothes
        item.category.contains("洗护") -> R.drawable.shop_grid_wash
        item.category.contains("喂养") || item.category.contains("营养") -> R.drawable.shop_grid_nutrition
        item.category.contains("待产") -> R.drawable.shop_grid_bag
        item.category.contains("玩具") -> R.drawable.shop_grid_toy
        else -> R.drawable.shop_cat_other
    }
}

@DrawableRes
internal fun categoryArt(name: String): Int = when {
    name.contains("母婴") || name.contains("新生儿") || name.contains("宝宝") -> R.drawable.shop_cat_baby
    name.contains("营养") || name.contains("喂养") -> R.drawable.shop_cat_nutrition
    name.contains("妈妈") || name.contains("孕妇") || name.contains("洗护") -> R.drawable.shop_cat_mom
    else -> R.drawable.shop_cat_other
}

internal fun itemTags(item: ShoppingItem): List<String> {
    val pieces = item.note.split('、', '，', ',', '·')
        .map { it.trim() }
        .filter { it.length in 2..8 }
    if (pieces.isNotEmpty()) return pieces.take(2)
    return listOf(item.category).filter { it.isNotBlank() }
}

@Composable
internal fun ShopTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkShop else LightShop
    CompositionLocalProvider(LocalShopColors provides colors, content = content)
}

@Composable
internal fun ShopPaper(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    if (!dark) DiaryPaperTexture(R.drawable.home_paper, modifier, 0.22f)
}

@Composable
internal fun ShopThumb(item: ShoppingItem, modifier: Modifier = Modifier.size(52.dp)) {
    val dark = isSystemInDarkTheme()
    val photo = item.photo
    if (!photo.isNullOrBlank()) {
        AsyncImage(File(photo), item.name, modifier.clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
    } else {
        Image(
            painterResource(shopArt(item)),
            item.name,
            modifier,
            contentScale = ContentScale.Fit,
            colorFilter = if (dark) DiaryDarkIllustration else null,
        )
    }
}

@Composable
internal fun ShopArt(@DrawableRes res: Int, description: String?, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    Image(
        painterResource(res),
        description,
        modifier,
        contentScale = ContentScale.Fit,
        colorFilter = if (dark) DiaryDarkIllustration else null,
    )
}

@Composable
internal fun ShopText(
    text: String,
    size: Int,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    center: Boolean = false,
) = DiaryText(text, size, color, modifier, weight, maxLines, center)

@Composable
internal fun PinkButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalShopColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(colors.button)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.onButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
internal fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalShopColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .border(1.5.dp, colors.button, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.button, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
internal fun ShopDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalShopColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.card, shadowElevation = 8.dp) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
internal fun CategoryPickerDialog(
    categories: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val colors = LocalShopColors.current
    ShopDialog(onDismiss) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ShopText("选择分类", 18, colors.ink, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ShopText("×", 22, colors.secondary, modifier = Modifier.clickable(onClick = onDismiss))
        }
        Spacer(Modifier.height(8.dp))
        categories.forEach { name ->
            val on = name == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) colors.pinkSoft else Color.Transparent)
                    .clickable { onSelect(name) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShopArt(categoryArt(name), null, Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    ShopText(name, 15, colors.ink, weight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .size(22.dp)
                        .border(1.5.dp, if (on) colors.button else colors.line, CircleShape)
                        .background(if (on) colors.button else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) Text("✓", color = colors.onButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun QuantityDialog(
    quantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = LocalShopColors.current
    var value = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(quantity.coerceAtLeast(1)) }
    ShopDialog(onDismiss) {
        ShopArt(R.drawable.shop_qty_bunny, null, Modifier.size(92.dp))
        ShopText("选择数量", 18, colors.ink, weight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StepperButton("−") { if (value.intValue > 1) value.intValue-- }
            Text("${value.intValue}", color = colors.button, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            StepperButton("+") { if (value.intValue < 99) value.intValue++ }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3, 5, 10).forEach { n ->
                val on = value.intValue == n
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (on) colors.button else colors.pinkSoft)
                        .clickable { value.intValue = n },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$n", color = if (on) colors.onButton else colors.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton("取消", Modifier.weight(1f), onDismiss)
            PinkButton("确定", Modifier.weight(1f)) { onConfirm(value.intValue) }
        }
    }
}

@Composable
internal fun BoughtDialog(name: String, onDismiss: () -> Unit) {
    val colors = LocalShopColors.current
    ShopDialog(onDismiss) {
        ShopArt(R.drawable.shop_bought_modal_bunny, null, Modifier.size(120.dp))
        Spacer(Modifier.height(6.dp))
        ShopText("已标记为已购买！", 18, colors.ink, weight = FontWeight.Bold, center = true)
        Spacer(Modifier.height(4.dp))
        ShopText("「$name」将移到已购买列表", 13, colors.secondary, center = true)
        Spacer(Modifier.height(14.dp))
        PinkButton("知道了", Modifier.fillMaxWidth(), onDismiss)
    }
}

@Composable
internal fun StepperButton(label: String, onClick: () -> Unit) {
    val colors = LocalShopColors.current
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.pinkSoft)
            .border(1.dp, colors.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ShopCheck(checked: Boolean, onClick: () -> Unit) {
    val colors = LocalShopColors.current
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) colors.button else Color.Transparent)
            .border(1.5.dp, if (checked) colors.button else colors.line, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Text("✓", color = colors.onButton, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun TagChip(text: String) {
    val colors = LocalShopColors.current
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.pinkSoft)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = colors.accent,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun rememberMarksState(): Pair<Set<Long>, Set<Long>> {
    val marks = rememberShoppingMarks()
    val fav by marks.favorites.collectAsState()
    val pick by marks.picked.collectAsState()
    return fav to pick
}
