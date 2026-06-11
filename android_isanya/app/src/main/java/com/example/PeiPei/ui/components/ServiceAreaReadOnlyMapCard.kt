// 文件说明：服务详情页内嵌只读高德地图，展示服务区域多边形（支持缩放与全屏查看）。

package com.example.Lulu.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Polygon
import com.amap.api.maps.model.PolygonOptions
import com.example.Lulu.util.ServiceLocationPolygonCodec
import kotlinx.coroutines.delay

private fun decodeLatLngs(encoded: String): List<LatLng> {
    val pts = ServiceLocationPolygonCodec.decodePolygonLatLng(encoded) ?: return emptyList()
    if (pts.size < 3) return emptyList()
    return pts.map { LatLng(it.first, it.second) }
}

private fun latLngSpan(points: List<LatLng>): Pair<Double, Double> {
    if (points.isEmpty()) return 0.0 to 0.0
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    return (maxLat - minLat) to (maxLon - minLon)
}

private fun centroidOf(points: List<LatLng>): LatLng {
    if (points.isEmpty()) return LatLng(18.2528, 109.5119)
    var lat = 0.0
    var lon = 0.0
    for (p in points) {
        lat += p.latitude
        lon += p.longitude
    }
    val n = points.size.toDouble()
    return LatLng(lat / n, lon / n)
}

private fun zoomForSpan(points: List<LatLng>): Float {
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

private class ReadonlyPolygonHolder {
    var polygon: Polygon? = null

    fun redraw(amap: AMap, points: List<LatLng>) {
        polygon?.remove()
        polygon = null
        if (points.size < 3) return
        val opts = PolygonOptions()
        for (p in points) opts.add(p)
        opts.strokeWidth(4f)
        opts.strokeColor(AndroidColor.argb(255, 88, 88, 88))
        opts.fillColor(AndroidColor.argb(110, 170, 170, 170))
        polygon = amap.addPolygon(opts)
    }

    fun clear() {
        polygon?.remove()
        polygon = null
    }
}

@Composable
private fun ServiceAreaReadonlyAmap(
    encodedLocation: String,
    modifier: Modifier = Modifier,
    scrollGesturesEnabled: Boolean,
    onMapReady: ((AMap) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val encodedState = rememberUpdatedState(encodedLocation)
    val holder = remember { ReadonlyPolygonHolder() }
    /** 列表/圆角 clip 下 [MapView]（GLSurfaceView）易白屏，改用 [TextureMapView]。 */
    var mapViewHolder by remember { mutableStateOf<TextureMapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureMapView(ctx).apply {
                onCreate(null)
                mapViewHolder = this
                val amap = map
                amap.mapType = AMap.MAP_TYPE_NORMAL
                amap.uiSettings.apply {
                    isZoomControlsEnabled = false
                    isScaleControlsEnabled = true
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isScrollGesturesEnabled = scrollGesturesEnabled
                    isZoomGesturesEnabled = true
                }
                fun currentPoints() = decodeLatLngs(encodedState.value)
                val pts0 = currentPoints()
                if (pts0.size >= 3) {
                    val c = centroidOf(pts0)
                    amap.moveCamera(CameraUpdateFactory.newLatLngZoom(c, zoomForSpan(pts0)))
                } else {
                    amap.moveCamera(CameraUpdateFactory.newLatLngZoom(centroidOf(emptyList()), 10f))
                }
                amap.setOnMapLoadedListener {
                    holder.redraw(amap, currentPoints())
                    post { holder.redraw(amap, currentPoints()) }
                }
                onMapReady?.invoke(amap)
                // LazyColumn 内首次布局后再 resume，否则底图偶发白屏
                post {
                    onResume()
                    holder.redraw(amap, currentPoints())
                }
            }
        },
        update = { view ->
            view.map.uiSettings.isScrollGesturesEnabled = scrollGesturesEnabled
        },
    )

    DisposableEffect(mapViewHolder) {
        val mv = mapViewHolder
        mv?.onResume()
        onDispose {
            holder.clear()
            mv?.onPause()
            mv?.onDestroy()
            if (mapViewHolder === mv) {
                mapViewHolder = null
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapViewHolder) {
        val mv = mapViewHolder ?: return@DisposableEffect onDispose { }
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

    LaunchedEffect(mapViewHolder, encodedLocation, scrollGesturesEnabled) {
        val mv = mapViewHolder ?: return@LaunchedEffect
        delay(60)
        val amap = mv.map
        amap.uiSettings.isScrollGesturesEnabled = scrollGesturesEnabled
        val pts = decodeLatLngs(encodedLocation)
        if (pts.size >= 3) {
            val c = centroidOf(pts)
            amap.moveCamera(CameraUpdateFactory.newLatLngZoom(c, zoomForSpan(pts)))
        }
        holder.redraw(amap, pts)
        mv.post { holder.redraw(amap, decodeLatLngs(encodedState.value)) }
    }
}

@Composable
fun ServiceAreaReadOnlyMapCard(
    encodedLocation: String,
    modifier: Modifier = Modifier,
) {
    var showFullscreen by remember { mutableStateOf(false) }
    var amapForZoom by remember { mutableStateOf<AMap?>(null) }
    val corner = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        ServiceAreaReadonlyAmap(
            encodedLocation = encodedLocation,
            modifier = Modifier
                .fillMaxSize()
                .clip(corner),
            scrollGesturesEnabled = false,
            onMapReady = { amapForZoom = it },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showFullscreen = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = "全屏查看地图",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            FloatingActionButton(
                onClick = { amapForZoom?.animateCamera(CameraUpdateFactory.zoomIn()) },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "放大")
            }
            FloatingActionButton(
                onClick = { amapForZoom?.animateCamera(CameraUpdateFactory.zoomOut()) },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "缩小")
            }
        }
    }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Box(Modifier.fillMaxSize()) {
                    ServiceAreaReadonlyAmap(
                        encodedLocation = encodedLocation,
                        modifier = Modifier.fillMaxSize(),
                        scrollGesturesEnabled = true,
                    )
                    IconButton(
                        onClick = { showFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
