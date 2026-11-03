package com.vknxworks.sumctrl.ui.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.ui.components.Group
import com.vknxworks.sumctrl.ui.components.GroupRow
import com.vknxworks.sumctrl.ui.components.TabTitle
import com.vknxworks.sumctrl.ui.components.bottomClearance
import com.vknxworks.sumctrl.ui.nav.LocalNavigator
import com.vknxworks.sumctrl.ui.nav.ToolRoute

/** The work tools, each a small calculator of its own. */
enum class Tool(val id: String, @StringRes val title: Int, @StringRes val subtitle: Int, val icon: ImageVector) {
    VAT("vat", R.string.tool_vat, R.string.tool_vat_sub, Icons.AutoMirrored.Outlined.ReceiptLong),
    DISCOUNT("discount", R.string.tool_discount, R.string.tool_discount_sub, Icons.Outlined.LocalOffer),
    MARGIN("margin", R.string.tool_margin, R.string.tool_margin_sub, Icons.AutoMirrored.Outlined.TrendingUp),
    INSTALMENTS("instalments", R.string.tool_instalments, R.string.tool_instalments_sub, Icons.Outlined.CalendarMonth),
    PERCENT("percent", R.string.tool_percent, R.string.tool_percent_sub, Icons.Outlined.Percent),
    ;

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: VAT
    }
}

@Composable
fun ToolsScreen() {
    val nav = LocalNavigator.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .bottomClearance(),
    ) {
        TabTitle(stringResource(R.string.tab_tools))
        Group(footer = stringResource(R.string.tools_footer)) {
            Tool.entries.forEach { t ->
                GroupRow(
                    stringResource(t.title),
                    stringResource(t.subtitle),
                    icon = t.icon,
                    showChevron = true,
                    onClick = { nav.navigate(ToolRoute(t.id)) },
                )
            }
        }
    }
}
