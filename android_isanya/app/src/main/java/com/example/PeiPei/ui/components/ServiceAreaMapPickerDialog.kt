// 文件说明：发布服务时在高德地图上点击选点构成多边形服务区域（底部下拉弹窗）。

package com.example.Lulu.ui.components

import android.graphics.Color as AndroidColor
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.Lulu.ui.theme.DialogTitleTopPadding
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polygon
import com.amap.api.maps.model.PolygonOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.example.Lulu.util.ServiceLocationPolygonCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 默认服务区域：海南省三亚市（市区大致中心，用于初始视野与标注）。 */
private val defaultSanyaLatLng = LatLng(18.2528, 109.5119)

private const val DEFAULT_REGION_LABEL = "海南省三亚市"

/**
 * 默认框选：与产品参考图接近的大范围梯形——西抵崖州一带、东含海棠沿海、北略进山、南伸入近海。
 * 非精确行政边界，可清空后重选。顺序：西北 → 东北 → 东南 → 西南（逆时针）。
 */
private val defaultSanyaCityBoundsPolygon: List<LatLng> = listOf(
    LatLng(18.52, 109.26),
    LatLng(18.50, 109.98),
    LatLng(17.94, 109.92),
    LatLng(17.96, 109.20),
)

private fun latLngSpan(points: List<LatLng>): Pair<Double, Double> {
    if (points.isEmpty()) return 0.0 to 0.0
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    return (maxLat - minLat) to (maxLon - minLon)
}

/** 根据多边形跨度选初始缩放（跨度大则更远，便于默认「全市」一眼看完）。 */
private fun initialZoomFor(points: List<LatLng>): Float {
    if (points.size < 2) return 10f
    val (latSpan, lonSpan) = latLngSpan(points)
    val maxDeg = maxOf(latSpan, lonSpan)
    return when {
        maxDeg > 0.55 -> 8.3f
        maxDeg > 0.32 -> 9.2f
        maxDeg > 0.18 -> 10.2f
        maxDeg > 0.08 -> 11f
        else -> 12.5f
    }
}

/** 点击靠近首点时视为「自动闭合」的距离阈值（米）。 */
private const val SNAP_CLOSE_TO_FIRST_METERS = 120f

/** 选点与连线用色（高对比，避免在卫星/浅色底图上发灰看不清）。 */
private val vertexStrokeColor = AndroidColor.argb(255, 220, 70, 30)
private val vertexFillColor = AndroidColor.argb(210, 255, 200, 120)
private val pathLineColor = AndroidColor.argb(255, 30, 120, 220)
private val closingHintLineColor = AndroidColor.argb(200, 160, 160, 160)

/** 仅移除本对话框绘制的覆盖物；勿用 [AMap.clear]，否则会打断地图点击等监听。 */
private class ServiceAreaOverlays {
    var polygon: Polygon? = null
    var lineOpen: Polyline? = null
    var lineClosingHint: Polyline? = null
    val vertexCircles = mutableListOf<Circle>()

    fun removeAll() {
        polygon?.remove()
        lineOpen?.remove()
        lineClosingHint?.remove()
        polygon = null
        lineOpen = null
        lineClosingHint = null
        vertexCircles.forEach { it.remove() }
        vertexCircles.clear()
    }
}

private fun centroidOf(points: List<LatLng>): LatLng {
    if (points.isEmpty()) return defaultSanyaLatLng
    var lat = 0.0
    var lon = 0.0
    for (p in points) {
        lat += p.latitude
        lon += p.longitude
    }
    val n = points.size.toDouble()
    return LatLng(lat / n, lon / n)
}

private fun redrawAmapOverlays(
    aMap: AMap,
    overlays: ServiceAreaOverlays,
    vertices: List<LatLng>,
    polygonLocked: Boolean
) {
    overlays.removeAll()
    when {
        vertices.size >= 3 -> {
            val opts = PolygonOptions()
            for (p in vertices) {
                opts.add(p)
            }
            opts.strokeWidth(14f)
            opts.strokeColor(pathLineColor)
            opts.fillColor(AndroidColor.argb(140, 60, 150, 255))
            overlays.polygon = aMap.addPolygon(opts)
        }
        vertices.size == 2 -> {
            overlays.lineOpen = aMap.addPolyline(
                PolylineOptions()
                    .add(vertices[0], vertices[1])
                    .width(12f)
                    .color(pathLineColor)
            )
            if (!polygonLocked) {
                overlays.lineClosingHint = aMap.addPolyline(
                    PolylineOptions()
                        .add(vertices[1], vertices[0])
                        .width(8f)
                        .color(closingHintLineColor)
                )
            }
        }
    }
    // 每个顶点用地面圆标出（后添加，压在线/面之上）；原先仅 2、3+ 点才画线/面，1 个点时界面全空。
    val vertexRadiusMeters = 38.0
    for (p in vertices) {
        overlays.vertexCircles.add(
            aMap.addCircle(
                CircleOptions()
                    .center(p)
                    .radius(vertexRadiusMeters)
                    .strokeWidth(5f)
                    .strokeColor(vertexStrokeColor)
                    .fillColor(vertexFillColor)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceAreaMapPickerDialog(
    initialEncodedLocation: String,
    onDismiss: () -> Unit,
    onConfirm: (encodedLocation: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapOverlays = remember { ServiceAreaOverlays() }
    val points = remember { mutableStateListOf<LatLng>() }
    var mapViewHolder by remember { mutableStateOf<MapView?>(null) }
    var aMapHolder by remember { mutableStateOf<AMap?>(null) }
    val defaultRegionMarkerRef = remember { arrayOfNulls<Marker>(1) }
    var polygonDrawingLocked by remember { mutableStateOf(false) }
    var appliedUserMapCenter by remember { mutableStateOf(false) }
    val lockedUpdated = rememberUpdatedState(polygonDrawingLocked)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lifecycleOwner = LocalLifecycleOwner.current
    val mvForLifecycle = mapViewHolder
    DisposableEffect(mvForLifecycle) {
        mvForLifecycle?.onResume()
        onDispose {
            mapOverlays.removeAll()
            defaultRegionMarkerRef[0]?.remove()
            defaultRegionMarkerRef[0] = null
            aMapHolder = null
            mvForLifecycle?.onPause()
            mvForLifecycle?.onDestroy()
            if (mapViewHolder === mvForLifecycle) {
                mapViewHolder = null
            }
        }
    }
    // 从后台回到前台时再次 onResume，否则 MapView 可能不刷新底图与覆盖物
    DisposableEffect(lifecycleOwner, mvForLifecycle) {
        val mv = mvForLifecycle ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mv.onResume()
                Lifecycle.Event.ON_PAUSE -> mv.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(initialEncodedLocation) {
        points.clear()
        polygonDrawingLocked = false
        val decoded = ServiceLocationPolygonCodec.decodePolygonLatLng(initialEncodedLocation)
        if (decoded != null && decoded.size >= 3) {
            decoded.forEach { (lat, lng) -> points.add(LatLng(lat, lng)) }
            polygonDrawingLocked = true
        } else {
            // 无已保存多边形时默认框选三亚全市；点「清空」后可重新选点
            defaultSanyaCityBoundsPolygon.forEach { points.add(it) }
            polygonDrawingLocked = true
        }
        appliedUserMapCenter = false
    }

    LaunchedEffect(aMapHolder, appliedUserMapCenter, points.size, initialEncodedLocation) {
        val map = aMapHolder ?: return@LaunchedEffect
        if (appliedUserMapCenter) return@LaunchedEffect
        if (points.isNotEmpty()) {
            val z = initialZoomFor(points)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(centroidOf(points), z))
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultSanyaLatLng, 10f))
        }
        appliedUserMapCenter = true
    }

    // 点击监听只挂一次；覆盖物刷新单独依赖 points / 锁定状态（避免与 factory 首帧空点竞态、且 snapshotFlow 对 StateList 偶发不刷）
    LaunchedEffect(aMapHolder) {
        val map = aMapHolder ?: return@LaunchedEffect
        map.setOnMapClickListener { latLng ->
            if (lockedUpdated.value) return@setOnMapClickListener
            if (points.size >= 3) {
                val first = points[0]
                val dist = FloatArray(1)
                Location.distanceBetween(
                    first.latitude, first.longitude,
                    latLng.latitude, latLng.longitude,
                    dist
                )
                if (dist[0] <= SNAP_CLOSE_TO_FIRST_METERS) {
                    polygonDrawingLocked = true
                    Toast.makeText(context, "已自动闭合选区", Toast.LENGTH_SHORT).show()
                    return@setOnMapClickListener
                }
            }
            if (points.size < 40) {
                points.add(latLng)
            }
        }
    }

    LaunchedEffect(aMapHolder, points.size, polygonDrawingLocked, initialEncodedLocation) {
        val map = aMapHolder ?: return@LaunchedEffect
        val pts = points.toList()
        val locked = polygonDrawingLocked
        // 略延迟，避免 GL/底图未就绪时 addPolygon/addCircle 被吞
        delay(50)
        redrawAmapOverlays(map, mapOverlays, pts, locked)
        mapViewHolder?.post {
            redrawAmapOverlays(map, mapOverlays, points.toList(), polygonDrawingLocked)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = "我可以前往以下框选区域提供服务",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = DialogTitleTopPadding)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "点击地图选点，框选服务区域",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    onCreate(null)
                                    mapViewHolder = this
                                    // 10.0.600 起为同步 getMap()，已无 getMapAsyn
                                    val amap = getMap()
                                    amap.setMapType(AMap.MAP_TYPE_NORMAL)
                                    amap.uiSettings.apply {
                                        setZoomControlsEnabled(false)
                                        setScaleControlsEnabled(false)
                                        setRotateGesturesEnabled(false)
                                    }
                                    val start = when {
                                        points.isNotEmpty() -> centroidOf(points)
                                        else -> defaultSanyaLatLng
                                    }
                                    val zoom = if (points.isNotEmpty()) initialZoomFor(points.toList()) else 10f
                                    amap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, zoom))
                                    defaultRegionMarkerRef[0]?.remove()
                                    defaultRegionMarkerRef[0] = amap.addMarker(
                                        MarkerOptions()
                                            .position(defaultSanyaLatLng)
                                            .title(DEFAULT_REGION_LABEL)
                                    ).apply { showInfoWindow() }
                                    // 底图未完成纹理时 addPolygon 可能不显示；加载后再绘（勿在 update 里 map 未就绪就 return，会永远不画）
                                    amap.setOnMapLoadedListener {
                                        redrawAmapOverlays(amap, mapOverlays, points.toList(), polygonDrawingLocked)
                                        post {
                                            redrawAmapOverlays(amap, mapOverlays, points.toList(), polygonDrawingLocked)
                                        }
                                    }
                                    aMapHolder = amap
                                }
                            },
                            update = { view ->
                                view.post {
                                    redrawAmapOverlays(
                                        view.map,
                                        mapOverlays,
                                        points.toList(),
                                        polygonDrawingLocked
                                    )
                                }
                            }
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FloatingActionButton(
                                onClick = { aMapHolder?.animateCamera(CameraUpdateFactory.zoomOut()) },
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "缩小")
                            }
                            FloatingActionButton(
                                onClick = { aMapHolder?.animateCamera(CameraUpdateFactory.zoomIn()) },
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "放大")
                            }
                        }
                    }
                }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        points.clear()
                        polygonDrawingLocked = false
                        appliedUserMapCenter = false
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Text("清空")
                }
                TextButton(
                    onClick = {
                        if (points.isNotEmpty()) {
                            polygonDrawingLocked = false
                            points.removeAt(points.lastIndex)
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Text("撤销")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        if (points.size < 3) {
                            Toast.makeText(context, "请至少点击地图添加 3 个顶点", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch(Dispatchers.Default) {
                            val center = centroidOf(points)
                            val display = try {
                                when {
                                    !Geocoder.isPresent() -> "地图框选区域（${points.size} 个顶点）"
                                    else -> {
                                        @Suppress("DEPRECATION")
                                        val list = Geocoder(context, java.util.Locale.CHINA)
                                            .getFromLocation(center.latitude, center.longitude, 1)
                                        val addr = list?.firstOrNull()
                                        if (addr == null) {
                                            "地图框选区域（${points.size} 个顶点）"
                                        } else {
                                            buildString {
                                                addr.adminArea?.takeIf { it.isNotBlank() }?.let { append(it) }
                                                addr.locality?.takeIf { it.isNotBlank() }?.let {
                                                    if (isNotEmpty()) append(" ")
                                                    append(it)
                                                }
                                                if (isEmpty()) append("地图框选区域（${points.size} 个顶点）")
                                            }
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                "地图框选区域（${points.size} 个顶点）"
                            }
                            val encoded = ServiceLocationPolygonCodec.encode(
                                display,
                                points.map { it.latitude to it.longitude }
                            )
                            withContext(Dispatchers.Main) {
                                onConfirm(encoded)
                            }
                        }
                    }
                ) {
                    Text("确定")
                }
            }
        }
    }
}
