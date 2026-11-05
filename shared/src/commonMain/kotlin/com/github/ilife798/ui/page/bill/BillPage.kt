package com.github.ilife798.ui.page.bill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ilife798.data.model.BillRecord
import com.github.ilife798.data.model.WalletAccount
import com.github.ilife798.data.model.billStatusName
import com.github.ilife798.data.model.paymentTypeName
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.component.AppPullToRefresh
import com.github.ilife798.ui.component.BlurredTopAppBar
import com.github.ilife798.ui.component.EmptyStateText
import com.github.ilife798.ui.component.LOAD_MORE_THRESHOLD_PX
import com.github.ilife798.ui.component.ListLoadMoreFooter
import com.github.ilife798.ui.component.PageScrollColumn
import com.github.ilife798.ui.component.SectionHeader
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.formatDateTime
import com.github.ilife798.util.formatMoney
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class BillStatusOption(
    val status: Int,
    val label: String,
)

private val billStatusOptions =
    listOf(
        BillStatusOption(1, "未付款"),
        BillStatusOption(3, "已付款"),
        BillStatusOption(2, "待确认"),
        BillStatusOption(4, "付款失败"),
        BillStatusOption(9, "已取消"),
    )

@Composable
fun BillPage(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onRecordClick: (String) -> Unit = {},
) {
    val state = viewModel.state
    val wallets = state.wallets
    val isLoggedIn = state.account.hasAnyToken
    val activeWallet = wallets.firstOrNull { it.id == state.activeWalletId } ?: wallets.firstOrNull()
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    val scrollState = rememberScrollState()

    val billStatusEntry =
        remember(viewModel.billStatus) {
            DropdownEntry(
                items =
                    billStatusOptions.map { option ->
                        DropdownItem(
                            text = option.label,
                            selected = option.status == viewModel.billStatus,
                            onClick = { viewModel.selectBillStatus(option.status) },
                        )
                    },
            )
        }

    LaunchedEffect(Unit) {
        if (state.account.hasAnyToken) {
            viewModel.refreshBillPage()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            max > 0 && scrollState.value >= max - LOAD_MORE_THRESHOLD_PX
        }
    }
    LaunchedEffect(shouldLoadMore, state.billRecords.size) {
        if (shouldLoadMore) viewModel.loadMoreBills()
    }
    LaunchedEffect(viewModel.billStatus) {
        scrollState.animateScrollTo(0)
    }

    Scaffold(
        topBar = {
            BlurredTopAppBar(
                title = "我的账单",
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
                actions = {
                    OverlayIconDropdownMenu(entry = billStatusEntry) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "选择账单类型",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AppPullToRefresh(viewModel.billRefreshing, { viewModel.refreshBills() }, scrollBehavior, paddingValues) {
            PageScrollColumn(
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                contentPadding = paddingValues,
                scrollState = scrollState,
            ) {
                WalletHeaderCard(
                    wallets = wallets,
                    activeWallet = activeWallet,
                    isLoggedIn = isLoggedIn,
                    onSelectWallet = { viewModel.selectWallet(it) },
                )
                BillRecordsCard(
                    records = state.billRecords,
                    loadingMore = viewModel.billLoadingMore,
                    hasMore = viewModel.billHasMore,
                    isLoggedIn = isLoggedIn,
                    onRecordClick = onRecordClick,
                )
            }
        }
    }
}

@Composable
private fun WalletHeaderCard(
    wallets: List<WalletAccount>,
    activeWallet: WalletAccount?,
    isLoggedIn: Boolean,
    onSelectWallet: (String) -> Unit,
) {
    val entries =
        remember(wallets, activeWallet?.id) {
            listOf(
                DropdownEntry(
                    items =
                        wallets.map { wallet ->
                            DropdownItem(
                                text = wallet.name.ifEmpty { "钱包" },
                                selected = wallet.id == activeWallet?.id,
                                onClick = { onSelectWallet(wallet.id) },
                            )
                        },
                ),
            )
        }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            OverlayDropdownMenu(
                entries = entries,
                title = activeWallet?.name?.ifEmpty { "钱包" } ?: "钱包",
                summary = if (wallets.size > 1) "点击切换钱包" else null,
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.BankCards,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                },
                enabled = wallets.isNotEmpty(),
            )
            HorizontalDivider()
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "总余额",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = if (isLoggedIn) "¥${formatMoney(activeWallet?.total)}" else "--",
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                BalanceBreakdown(activeWallet)
            }
        }
    }
}

@Composable
private fun BillRecordsCard(
    records: List<BillRecord>,
    loadingMore: Boolean,
    hasMore: Boolean,
    isLoggedIn: Boolean,
    onRecordClick: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("账单记录")
            if (records.isEmpty()) {
                EmptyStateText(isLoggedIn)
            } else {
                records.forEach { record ->
                    BillRecordRow(record, onRecordClick)
                }
                ListLoadMoreFooter(loadingMore = loadingMore, hasMore = hasMore)
            }
        }
    }
}

@Composable
private fun BillRecordRow(
    record: BillRecord,
    onRecordClick: (String) -> Unit,
) {
    val isRefund = record.dir == 2
    val amountColor = if (isRefund) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
    val statusColor = billStatusColor(record.status, record.dir)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onRecordClick(record.id) }
                .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.msg.ifEmpty { "账单记录" },
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = formatDateTime(record.time),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = billStatusName(record.status, record.dir),
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                )
                Text(
                    text = "·",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = paymentTypeName(record.type),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Text(
            text = (if (isRefund) "+¥" else "¥") + formatMoney(record.payment),
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Medium,
            color = amountColor,
        )
    }
}

@Composable
private fun BalanceBreakdown(wallet: WalletAccount?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BalanceText(label = "线上余额", value = wallet?.olCash, modifier = Modifier.weight(1f))
            BalanceText(label = "线下余额", value = wallet?.ofCash, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BalanceText(label = "线上赠送", value = wallet?.olGift, modifier = Modifier.weight(1f))
            BalanceText(label = "线下赠送", value = wallet?.ofGift, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BalanceText(
    label: String,
    value: Double?,
    modifier: Modifier,
) {
    Text(
        text = "$label：${formatMoney(value)}元",
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier,
    )
}

// 账单状态展示色（列表行与详情页共用）
@Composable
internal fun billStatusColor(
    status: Int,
    dir: Int,
): Color =
    when {
        dir == 2 -> if (status == 3) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
        status == 3 -> MiuixTheme.colorScheme.primary
        status == 4 -> MiuixTheme.colorScheme.error
        status == 1 -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
