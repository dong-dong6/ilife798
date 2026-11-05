package com.github.ilife798.ui.page.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ilife798.data.model.ScoreFilter
import com.github.ilife798.data.model.ScoreRecord
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScorePage(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val state = viewModel.state
    val scores = viewModel.scoreRecords
    val isLoggedIn = state.account.hasAnyToken
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    val scrollState = rememberScrollState()

    val scoreFilterEntry =
        remember(viewModel.scoreFilter) {
            DropdownEntry(
                items =
                    ScoreFilter.entries.map { filter ->
                        DropdownItem(
                            text = scoreFilterLabel(filter),
                            selected = viewModel.scoreFilter == filter,
                            onClick = { viewModel.selectScoreFilter(filter) },
                        )
                    },
            )
        }

    LaunchedEffect(Unit) {
        if (state.account.hasAnyToken) {
            viewModel.refreshScorePage()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            max > 0 && scrollState.value >= max - LOAD_MORE_THRESHOLD_PX
        }
    }
    LaunchedEffect(shouldLoadMore, scores.size) {
        if (shouldLoadMore) viewModel.loadMoreScores()
    }
    LaunchedEffect(viewModel.scoreFilter) {
        scrollState.animateScrollTo(0)
    }
    Scaffold(
        topBar = {
            BlurredTopAppBar(
                title = "积分明细",
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
                actions = {
                    OverlayIconDropdownMenu(entry = scoreFilterEntry) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "选择积分类型",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AppPullToRefresh(viewModel.scoreRefreshing, { viewModel.refreshScores() }, scrollBehavior, paddingValues) {
            PageScrollColumn(
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                contentPadding = paddingValues,
                scrollState = scrollState,
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "积分概览",
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            ScoreSummaryItem(
                                label = "可用积分",
                                value = state.points.available,
                                alignEnd = false,
                            )
                            ScoreSummaryItem(
                                label = "累计积分",
                                value = state.points.total,
                                alignEnd = true,
                            )
                        }
                    }
                }

                ScoreRecordsCard(
                    records = scores,
                    loadingMore = viewModel.scoreLoadingMore,
                    hasMore = viewModel.scoreHasMore,
                    isLoggedIn = isLoggedIn,
                )
            }
        }
    }
}

@Composable
private fun ScoreRecordsCard(
    records: List<ScoreRecord>,
    loadingMore: Boolean,
    hasMore: Boolean,
    isLoggedIn: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("积分记录")
            if (records.isEmpty()) {
                EmptyStateText(isLoggedIn)
            } else {
                records.forEach { record ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.name,
                                style = MiuixTheme.textStyles.subtitle,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = formatDateTime(record.time),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Text(
                            text = if (record.score > 0) "+${record.score}" else "${record.score}",
                            style = MiuixTheme.textStyles.title3,
                            fontWeight = FontWeight.Medium,
                            color = if (record.score > 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error,
                        )
                    }
                }
                ListLoadMoreFooter(
                    loadingMore = loadingMore,
                    hasMore = hasMore,
                )
            }
        }
    }
}

@Composable
private fun ScoreSummaryItem(
    label: String,
    value: Int?,
    alignEnd: Boolean,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = value?.toString() ?: "-",
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = "≈${formatMoney((value ?: 0) / 1000.0)}元",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun scoreFilterLabel(filter: ScoreFilter): String =
    when (filter) {
        ScoreFilter.All -> "全部"
        ScoreFilter.Income -> "收入"
        ScoreFilter.Expense -> "支出"
    }
