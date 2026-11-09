package com.miniyou.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.Routes
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.app
import com.miniyou.app.ui.theme.ContentWidth
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ShoppingDetailScreen(id: Long, nav: NavController) {
    ShopTheme {
        val colors = LocalShopColors.current
        val app = app()
        val scope = rememberCoroutineScope()
        val item by app.db.shopping().observe(id).collectAsState(initial = null)
        val marks = rememberShoppingMarks()
        val favorites by marks.favorites.collectAsState()
        val current = item
        var qty by remember(current?.id, current?.quantity) { mutableIntStateOf(current?.quantity ?: 1) }
        AppScaffold(containerColor = colors.paper, topBar = {
            BackTopBar("商品详情", { nav.popBackStack() }, containerColor = colors.paper) {
                if (current != null) {
                    val favorite = current.id in favorites
                    IconToggleButton(checked = favorite, onCheckedChange = { marks.toggleFavorite(current.id) }) {
                        Icon(
                            if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            "收藏", tint = colors.accent,
                        )
                    }
                    IconButton(onClick = { nav.navigate(Routes.shoppingEdit(current.id)) }) {
                        Icon(Icons.Outlined.Edit, "编辑", tint = colors.ink)
                    }
                }
            }
        }) { pad ->
            if (current == null) return@AppScaffold
            Box(Modifier.fillMaxSize().padding(pad)) {
                ShopPaper(Modifier.matchParentSize())
                ContentWidth(maxWidth = 660.dp) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(colors.pinkSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            val photo = current.photo
                            if (!photo.isNullOrBlank()) {
                                AsyncImage(File(photo), current.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                ShopArt(shopArt(current), current.name, Modifier.size(180.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        ShopText(current.name, 22, colors.ink, weight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            TagChip(current.category)
                            if (current.channel.isNotBlank()) TagChip(current.channel)
                            TagChip(ShoppingStatus.labels.getOrElse(current.status) { "计划购买" })
                        }
                        Spacer(Modifier.height(8.dp))
                        val isBought = current.status == ShoppingStatus.BOUGHT
                        ShopText(
                            current.recordedAmount?.let { ledgerMoney(it) } ?: if (isBought) "未记金额" else "待估价",
                            26, colors.accent, weight = FontWeight.Bold,
                        )
                        current.price?.let { unitPrice ->
                            if (current.recordedAmount != null) {
                                ShopText(
                                    "${if (isBought) "实付合计" else "预计合计"} · ${money(unitPrice)} × ${current.quantity}",
                                    12, colors.secondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        ShopText(
                            current.note.ifBlank { "记下规格、尺码或为什么想买它。点右上角可以继续编辑。" },
                            14, colors.secondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        ShopText("数量", 14, colors.ink, weight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StepperButton("−") { if (qty > 1) qty-- }
                            ShopText("$qty", 22, colors.ink, weight = FontWeight.Bold)
                            StepperButton("+") { if (qty < 99) qty++ }
                        }
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineButton(if (isBought) "编辑商品" else "记录购买", Modifier.weight(1f)) {
                                nav.navigate(Routes.shoppingEdit(
                                    id = current.id,
                                    status = if (isBought) ShoppingStatus.PLANNED else ShoppingStatus.BOUGHT,
                                ))
                            }
                            PinkButton("保存", Modifier.weight(1f)) {
                                scope.launch {
                                    app.db.shopping().upsert(current.copy(quantity = qty, updatedAt = System.currentTimeMillis()))
                                    nav.popBackStack()
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
