package com.miniyou.app.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.miniyou.app.data.db.ShoppingStatus
import com.miniyou.app.ui.AppScaffold
import com.miniyou.app.ui.components.BackTopBar
import com.miniyou.app.ui.components.app

@Composable
fun ShoppingAnalysisScreen(nav: NavController) {
    ShopTheme {
        val colors = LocalShopColors.current
        val app = app()
        val items by app.db.shopping().all().collectAsState(initial = emptyList())
        val settings by app.settings.state.collectAsState()
        val groups = remember(items) { items.groupBy { it.status } }
        val bought = groups[ShoppingStatus.BOUGHT].orEmpty()
        AppScaffold(containerColor = colors.paper, topBar = {
            BackTopBar("消费分析", { nav.popBackStack() }, containerColor = colors.paper)
        }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = 660.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { SpendingOverview(bought) }
                    item {
                        BudgetPreview(
                            spent = bought.amounts(),
                            decided = groups[ShoppingStatus.DECIDED].orEmpty(),
                            planned = groups[ShoppingStatus.PLANNED].orEmpty(),
                            budget = settings.shoppingBudget?.toBigDecimalOrNull(),
                            onBudgetChanged = { amount ->
                                app.settings.update { it.copy(shoppingBudget = amount?.toPlainString()) }
                            },
                        )
                    }
                }
            }
        }
    }
}
