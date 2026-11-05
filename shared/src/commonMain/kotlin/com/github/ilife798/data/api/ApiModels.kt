package com.github.ilife798.data.api

import com.github.ilife798.data.model.BillRecord
import com.github.ilife798.data.model.RefundProgress
import com.github.ilife798.data.model.WalletAccount

data class AccountInfoDto(
    val id: String = "",
    val img: String = "",
    val name: String = "",
    val pn: String = "",
)

// 依据任务列表 stype 交集判定凭据所属渠道
enum class TokenPlatform {
    APP, // 设备控制（stype 交集只剩 1）
    MAIN, // 积分查询（stype 交集只剩 5）
    UNKNOWN,
}

data class TokenProbe(
    val appValid: Boolean,
    val mainValid: Boolean,
    val uid: String = "",
    val platform: TokenPlatform = TokenPlatform.UNKNOWN,
)

data class DeviceDto(
    val id: String,
    val name: String,
    val geneStatus: Int = 0,
    val dtype: Int = 0,
    val deviceStatus: Int = 1,
)

data class MasterResult(
    val accountId: String = "",
    val devices: List<DeviceDto> = emptyList(),
)

data class DevStatusResult(
    val deviceStatus: Int,
    val geneStatus: Int,
    // gene.time：会话预计结束时间（毫秒），随状态轮询刷新用于“剩余X分钟”
    val geneEndTime: Long = 0L,
)

data class DevResult(
    val success: Boolean,
    val code: Int,
    val message: String,
)

data class QrUseResult(
    val success: Boolean,
    val type: Int?,
    val deviceId: String,
)

data class ScoreDto(
    val score: Int,
    val name: String,
    val time: String,
    val adId: String = "",
)

data class ScoreListResult(
    val records: List<ScoreDto>,
    val total: Int,
)

data class WalletOwnerResult(
    val active: WalletAccount? = null,
    val wallets: List<WalletAccount> = emptyList(),
    val refundProgress: RefundProgress? = null,
)

data class BillListResult(
    val records: List<BillRecord>,
    val total: Int,
)
