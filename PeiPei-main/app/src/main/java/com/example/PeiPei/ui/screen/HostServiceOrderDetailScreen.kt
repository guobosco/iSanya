// 文件说明：发布者从日历头像进入的订单详情（演示数据，占位页）。

package com.example.Lulu.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Lulu.data.local.MockDataStore
import com.example.Lulu.ui.components.CommonAvatar
private val PageBg = Color(0xFFFFFFFF)
private val TitleInk = Color(0xFF000000)
private val Muted = Color(0xFF666666)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostServiceOrderDetailScreen(
    navController: NavController,
    orderId: String,
) {
    val booking = remember(orderId) { MockDataStore.hostBookingByOrderId(orderId) }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("订单详情", color = TitleInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TitleInk)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PageBg),
            )
        },
    ) { padding ->
        if (booking == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("未找到该订单", color = Muted, fontSize = 15.sp)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CommonAvatar(
                imageUrl = booking.guestPhotoUrl.ifBlank { null },
                name = booking.guestName,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
                shape = CircleShape,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = booking.guestName.ifBlank { "预订用户" },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleInk,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "服务日期：${booking.dateIso}",
                fontSize = 15.sp,
                color = Muted,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "订单号：${booking.orderId}",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "此处可接入支付状态、改期与沟通等完整订单流程；当前为演示数据。",
                fontSize = 14.sp,
                color = Muted,
                lineHeight = 20.sp,
            )
        }
    }
}
