package com.gcashagent.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.ui.theme.CashInGreen
import com.gcashagent.tracker.ui.theme.CashInGreenContainer
import com.gcashagent.tracker.ui.theme.CashOutRed
import com.gcashagent.tracker.ui.theme.CashOutRedContainer

@Composable
fun CashFlowChip(cashFlow: CashFlow, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (cashFlow) {
        CashFlow.CASH_IN -> Triple(CashInGreenContainer, CashInGreen, "CASH IN")
        CashFlow.CASH_OUT -> Triple(CashOutRedContainer, CashOutRed, "CASH OUT")
    }
    Text(
        text = label,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
