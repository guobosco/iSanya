// 文件说明：聊天「发送位置」全屏地图选点（高德底图 + 逆地理周边列表 + 关键词 POI 搜索）。

package com.example.Lulu.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeAddress
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.poisearch.PoiSearch.Query
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private val fallbackCenter = LatLng(39.9042, 116.4074)

private data class ChatMapPickRow(
    val title: String,
    val subtitle: String,
    val latLng: LatLng,
)

private fun distanceMeters(a: LatLng, b: LatLng): Int {
    val r = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r)
    return r[0].toInt().coerceAtLeast(0)
}

private fun poiItemToRow(center: LatLng, p: PoiItem): ChatMapPickRow {
    val pt = p.latLonPoint
    val ll = if (pt != null) LatLng(pt.latitude, pt.longitude) else center
    val d = distanceMeters(center, ll)
    val subParts = buildList {
        add("${d}m内")
        val snip = p.snippet?.trim().orEmpty()
        if (snip.isNotBlank()) add(snip)
        else {
            val city = p.cityName?.trim().orEmpty()
            val ad = p.adName?.trim().orEmpty()
            val tail = listOf(city, ad).filter { it.isNotBlank() }.joinToString(" ")
            if (tail.isNotBlank()) add(tail)
        }
    }
    return ChatMapPickRow(
        title = p.title?.trim().orEmpty().ifBlank { "地点" },
        subtitle = subParts.joinToString(" | "),
        latLng = ll,
    )
}

private fun buildRegeocodeRows(center: LatLng, addr: RegeocodeAddress): List<ChatMapPickRow> {
    val rows = mutableListOf<ChatMapPickRow>()
    val fmt = addr.formatAddress?.trim().orEmpty()
    val district = addr.district?.trim().orEmpty()
    if (fmt.isNotBlank()) {
        val sub = if (district.isNotBlank()) "地图上选点 | $district" else "地图上选点"
        rows.add(ChatMapPickRow(title = fmt, subtitle = sub, latLng = center))
    }
    addr.pois?.forEach { p -> rows.add(poiItemToRow(center, p)) }
    if (rows.isEmpty()) {
        rows.add(
            ChatMapPickRow(
                title = "地图选点",
                subtitle = "${center.latitude.format6()}, ${center.longitude.format6()}",
                latLng = center,
            )
        )
    }
    return rows
}

private suspend fun fetchRegeocodeRows(context: Context, center: LatLng): Result<List<ChatMapPickRow>> =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val gs = GeocodeSearch(context)
            val q = RegeocodeQuery(LatLonPoint(center.latitude, center.longitude), 250f, GeocodeSearch.AMAP)
            val listener = object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
                    gs.setOnGeocodeSearchListener(null)
                    if (cont.isCompleted) return
                    if (rCode != AMapException.CODE_AMAP_SUCCESS || result?.regeocodeAddress == null) {
                        cont.resume(Result.failure(IllegalStateException("regeocode $rCode")))
                    } else {
                        cont.resume(Result.success(buildRegeocodeRows(center, result.regeocodeAddress)))
                    }
                }

                override fun onGeocodeSearched(
                    result: com.amap.api.services.geocoder.GeocodeResult?,
                    rCode: Int,
                ) {
                }
            }
            cont.invokeOnCancellation { gs.setOnGeocodeSearchListener(null) }
            gs.setOnGeocodeSearchListener(listener)
            gs.getFromLocationAsyn(q)
        }
    }

private suspend fun fetchPoiRows(context: Context, keyword: String, center: LatLng): Result<List<ChatMapPickRow>> =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val query = Query(keyword.trim(), "", "")
            query.setPageSize(25)
            query.setPageNum(0)
            val ps = PoiSearch(context, query)
            ps.bound = PoiSearch.SearchBound(LatLonPoint(center.latitude, center.longitude), 50000)
            ps.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                    ps.setOnPoiSearchListener(null)
                    if (!cont.isActive) return
                    if (rCode != AMapException.CODE_AMAP_SUCCESS || result?.pois == null) {
                        cont.resume(Result.failure(IllegalStateException("poi $rCode")))
                        return
                    }
                    val rows = result.pois.map { poiItemToRow(center, it) }
                    if (rows.isEmpty()) {
                        cont.resume(Result.failure(IllegalStateException("empty")))
                    } else {
                        cont.resume(Result.success(rows))
                    }
                }

                override fun onPoiItemSearched(item: PoiItem?, rCode: Int) {}
            })
            cont.invokeOnCancellation { ps.setOnPoiSearchListener(null) }
            ps.searchPOIAsyn()
        }
    }

@SuppressLint("MissingPermission")
private fun lastKnownLatLng(context: Context): LatLng? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var best: Location? = null
    for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
        val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
        if (best == null || loc.accuracy < best!!.accuracy) best = loc
    }
    return best?.let { LatLng(it.latitude, it.longitude) }
}

@Composable
fun ChatLocationMapPickerOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, address: String, latitude: Double, longitude: Double) -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var mapViewHolder by remember { mutableStateOf<TextureMapView?>(null) }
    var aMapState by remember { mutableStateOf<AMap?>(null) }
    var mapCenter by remember { mutableStateOf(fallbackCenter) }
    val mapCenterState = rememberUpdatedState(mapCenter)

    var candidates by remember { mutableStateOf<List<ChatMapPickRow>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var loadingPois by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun applyRows(rows: List<ChatMapPickRow>, fromSearch: Boolean) {
        candidates = rows
        selectedIndex = 0
        loadingPois = false
        if (fromSearch && rows.isNotEmpty()) {
            val target = rows.first().latLng
            aMapState?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
        }
    }

    val searchTrimState = rememberUpdatedState(newValue = searchText.trim())

    val scheduleLoadFromMapCenterLatest = rememberUpdatedState<() -> Unit>(
        newValue = {
            loadJob?.cancel()
            loadingPois = true
            loadJob = scope.launch {
                delay(320)
                ensureActive()
                val center = mapCenterState.value
                val r = fetchRegeocodeRows(context, center)
                ensureActive()
                r.fold(
                    onSuccess = { applyRows(it, fromSearch = false) },
                    onFailure = {
                        loadingPois = false
                        if (candidates.isEmpty()) {
                            Toast.makeText(context, "未能获取周边地点", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
            Unit
        },
    )

    fun schedulePoiSearch(keyword: String) {
        loadJob?.cancel()
        loadingPois = true
        loadJob = scope.launch {
            delay(400)
            ensureActive()
            val center = mapCenterState.value
            val r = fetchPoiRows(context, keyword, center)
            ensureActive()
            r.fold(
                onSuccess = { applyRows(it, fromSearch = true) },
                onFailure = {
                    loadingPois = false
                    candidates = emptyList()
                    Toast.makeText(context, "未找到相关地点", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    LaunchedEffect(searchText) {
        val q = searchText.trim()
        if (q.isEmpty()) {
            delay(120)
            scheduleLoadFromMapCenterLatest.value.invoke()
        } else {
            schedulePoiSearch(q)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            val ll = lastKnownLatLng(context)
            if (ll != null) {
                aMapState?.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16f))
            } else {
                Toast.makeText(context, "暂时无法获取当前位置", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "需要定位权限才能回到当前位置", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocate() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
            return
        }
        val ll = lastKnownLatLng(context)
        if (ll != null) {
            aMapState?.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16f))
        } else {
            Toast.makeText(context, "暂时无法获取当前位置", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = "发送位置",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    )
                    val sendEnabled = candidates.isNotEmpty()
                    TextButton(
                        onClick = {
                            val row = candidates.getOrNull(selectedIndex)
                            if (row == null) {
                                Toast.makeText(context, "请先等待地点加载完成", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            onConfirm(
                                row.title,
                                row.subtitle,
                                row.latLng.latitude,
                                row.latLng.longitude,
                            )
                        },
                        enabled = sendEnabled,
                    ) {
                        Text(
                            "发送",
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (sendEnabled) 1f else 0.38f,
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val mapH = maxHeight * 0.58f
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(mapH),
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    TextureMapView(ctx).apply {
                                        onCreate(null)
                                        mapViewHolder = this
                                        val amap = map
                                        aMapState = amap
                                        amap.mapType = AMap.MAP_TYPE_NORMAL
                                        amap.uiSettings.apply {
                                            isZoomControlsEnabled = false
                                            isScaleControlsEnabled = true
                                            isRotateGesturesEnabled = false
                                            isTiltGesturesEnabled = false
                                        }
                                        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackCenter, 14f))
                                        amap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                                            override fun onCameraChange(position: CameraPosition?) {}

                                            override fun onCameraChangeFinish(position: CameraPosition?) {
                                                val t = position?.target ?: return
                                                mapCenter = t
                                                if (searchTrimState.value.isEmpty()) {
                                                    scheduleLoadFromMapCenterLatest.value.invoke()
                                                }
                                            }
                                        })
                                        post { onResume() }
                                    }
                                },
                                update = { },
                            )

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF07C160),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp)
                                    .offset(y = (-22).dp),
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .size(44.dp)
                                    .clickable { requestLocate() },
                                shape = CircleShape,
                                shadowElevation = 3.dp,
                                color = Color.White,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "定位",
                                        tint = Color(0xFF1989FA),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface),
                        ) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                placeholder = { Text("搜索地点") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                            )

                            Box(Modifier.fillMaxSize()) {
                                if (loadingPois && candidates.isEmpty()) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                }
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(
                                        candidates,
                                        key = { idx, row -> "$idx-${row.title}-${row.latLng.latitude}" },
                                    ) { idx, row ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedIndex = idx
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = row.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = row.subtitle,
                                                    color = Color(0xFF888888),
                                                    fontSize = 12.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            if (idx == selectedIndex) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(mapViewHolder) {
        val mv = mapViewHolder
        mv?.onResume()
        onDispose {
            loadJob?.cancel()
            mv?.onPause()
            mv?.onDestroy()
            if (mapViewHolder === mv) mapViewHolder = null
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

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        delay(100)
        val ll = lastKnownLatLng(context)
        val start = ll ?: fallbackCenter
        val zoom = if (ll != null) 16f else 14f
        aMapState?.moveCamera(CameraUpdateFactory.newLatLngZoom(start, zoom))
        mapCenter = start
    }
}

private fun Double.format6(): String = String.format(java.util.Locale.US, "%.6f", this)
