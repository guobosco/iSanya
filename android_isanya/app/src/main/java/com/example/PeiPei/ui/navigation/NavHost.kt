// 文件说明：应用导航图与路由注册，串联各业务界面。

package com.example.Lulu.ui.navigation

/**
 * 应用导航编排文件。
 * 负责声明所有路由、页面映射，以及通知唤起等入口场景下的跳转处理。
 */

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.Lulu.ui.screen.MainScreen
import com.example.Lulu.ui.screen.ServiceDetailScreen
import com.example.Lulu.ui.screen.ServiceHostProfileScreen
import com.example.Lulu.ui.screen.UserInfoScreen
import com.example.Lulu.ui.screen.SearchScreen

import com.example.Lulu.ui.screen.MyProfileEditScreen
import com.example.Lulu.ui.screen.MyQrCodeScreen
import com.example.Lulu.ui.screen.ServiceHostEditScreen
import com.example.Lulu.ui.screen.TagManageScreen
import com.example.Lulu.ui.screen.TagDetailScreen
import com.example.Lulu.ui.screen.ScanScreen

import com.example.Lulu.ui.screen.LoginScreen
import com.example.Lulu.ui.screen.WeChatBindPhoneScreen

import com.example.Lulu.ui.screen.CompleteNameScreen
import com.example.Lulu.ui.screen.CompleteProfileScreen
import com.example.Lulu.ui.screen.MessageThreadScreen
import com.example.Lulu.ui.screen.CreateServiceScreen
import androidx.compose.runtime.LaunchedEffect
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

import android.content.Intent
import com.example.Lulu.ui.screen.SplashScreen

import com.example.Lulu.ui.screen.SystemPermissionScreen
import com.example.Lulu.ui.screen.ExperienceCategoryFeedScreen
import com.example.Lulu.ui.screen.ExperienceDetailScreen
import com.example.Lulu.ui.screen.RealNameVerificationScreen
import com.example.Lulu.ui.screen.MyIncomeScreen
import com.example.Lulu.ui.screen.HostServiceOrderDetailScreen
import com.example.Lulu.ui.screen.MyHostIncomingOrdersScreen
import com.example.Lulu.ui.screen.MyPublishedServicesScreen
import com.example.Lulu.ui.screen.PublishedServiceCalendarScreen

/** 登录后主壳内层导航：保留底部 Tab 与首页组合，仅详情压栈。 */
private object MainShellInnerRoutes {
    const val Tabs = "main_tabs"
}

/**
 * 应用导航主机
 * 功能：配置应用的所有导航路由，定义不同屏幕之间的导航关系
 */
@ExperimentalMaterial3Api
@Composable
fun AppNavHost(intentState: Intent? = null) {
    // 创建导航控制器
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // 冷启动进入首页，不要求先登录
    val targetDestination = Screen.Home.route
    // 启动页作为初始页面
    val startDestination = Screen.Splash.route

    // 处理从通知跳转过来的意图
    LaunchedEffect(intentState) {
        val intent = intentState ?: (context as? Activity)?.intent
        val navigateTo = intent?.getStringExtra("navigate_to")
        val userId = intent?.getStringExtra("userId")?.trim()
        val threadId = intent?.getStringExtra("threadId")?.trim()
        
        android.util.Log.d("AppNavHost", "Checking intent: navigateTo=$navigateTo, userId=$userId")
        
        if (navigateTo == "service_host_profile" && !userId.isNullOrEmpty()) {
            navController.navigate(Screen.ServiceHostProfile.createRoute(userId))
            intent.removeExtra("navigate_to")
            intent.removeExtra("userId")
        } else if (navigateTo == "my_qr_code") {
            navController.navigate(Screen.MyQrCode.route)
            intent.removeExtra("navigate_to")
        } else if (navigateTo == "scan") {
            navController.navigate(Screen.Scan.route)
            intent.removeExtra("navigate_to")
        } else if (navigateTo == "create_service") {
            navController.navigate(Screen.CreateService.createRoute())
            intent.removeExtra("navigate_to")
        } else if (navigateTo == "message_thread" && !threadId.isNullOrEmpty()) {
            navController.navigate(Screen.MessageThread.createRoute(threadId))
            intent.removeExtra("navigate_to")
            intent.removeExtra("threadId")
        }
    }
    
    // 导航主机，定义所有路由和对应的屏幕
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 启动页路由
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController, nextRoute = targetDestination)
        }
        
        // 主页：内层 NavHost 使从详情返回时 MainScreen/HomeScreen 不离开组合
        composable(Screen.Home.route) { homeEntry ->
            val outerNavController = navController
            val forceTab = homeEntry.savedStateHandle.get<Int>("force_main_tab")
            if (forceTab != null) {
                homeEntry.savedStateHandle.remove<Int>("force_main_tab")
            }
            val mainShellNavController = rememberNavController()
            NavHost(
                navController = mainShellNavController,
                startDestination = MainShellInnerRoutes.Tabs,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MainShellInnerRoutes.Tabs) {
                    MainScreen(
                        navController = outerNavController,
                        mainShellNavController = mainShellNavController,
                        homeBackStackEntry = homeEntry,
                        initialSelectedItem = forceTab ?: 0
                    )
                }
                composable(
                    route = Screen.ServiceDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("serviceId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("from") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        androidx.navigation.navArgument("openCreatorPrice") {
                            type = androidx.navigation.NavType.IntType
                            defaultValue = 0
                        },
                    )
                ) { detailEntry ->
                    val serviceId = detailEntry.arguments?.getString("serviceId")
                    val from = detailEntry.arguments?.getString("from")
                    val openCreatorPrice = detailEntry.arguments?.getInt("openCreatorPrice") == 1
                    ServiceDetailScreen(
                        rootNavController = outerNavController,
                        mainShellNavController = mainShellNavController,
                        serviceId = serviceId,
                        entrySource = from,
                        openCreatorPriceOnLaunch = openCreatorPrice,
                    )
                }
                composable(
                    route = Screen.ExperienceCategoryFeed.route,
                    arguments = listOf(
                        navArgument("categoryTitle") { type = NavType.StringType },
                    ),
                ) { feedEntry ->
                    val categoryTitle = feedEntry.arguments?.getString("categoryTitle").orEmpty()
                    ExperienceCategoryFeedScreen(
                        navController = mainShellNavController,
                        categoryTitle = categoryTitle,
                    )
                }
                composable(
                    route = Screen.ExperienceDetail.route,
                    arguments = listOf(
                        navArgument("experienceId") { type = NavType.StringType },
                    ),
                ) { expEntry ->
                    val expId = expEntry.arguments?.getString("experienceId")
                    ExperienceDetailScreen(
                        rootNavController = outerNavController,
                        mainShellNavController = mainShellNavController,
                        experienceId = expId,
                    )
                }
            }
        }
        composable(
            route = Screen.CreateService.route,
            arguments = listOf(
                androidx.navigation.navArgument("serviceId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("initialUserIds") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("initialTitle") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("initialCategory") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("openBooking") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = 0
                },
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")
            val initialUserIds = backStackEntry.arguments?.getString("initialUserIds")
            val initialTitle = backStackEntry.arguments?.getString("initialTitle")
            val initialCategory = backStackEntry.arguments?.getString("initialCategory")
            val openBooking = backStackEntry.arguments?.getInt("openBooking") == 1
            CreateServiceScreen(
                navController = navController, 
                serviceId = serviceId,
                initialUserIds = initialUserIds,
                initialTitle = initialTitle,
                initialCategory = initialCategory,
                openBookingOnLaunch = openBooking,
            )
        }
        composable(
            route = Screen.ServiceDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("serviceId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("from") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("openCreatorPrice") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = 0
                },
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")
            val from = backStackEntry.arguments?.getString("from")
            val openCreatorPrice = backStackEntry.arguments?.getInt("openCreatorPrice") == 1
            ServiceDetailScreen(
                rootNavController = navController,
                mainShellNavController = null,
                serviceId = serviceId,
                entrySource = from,
                openCreatorPriceOnLaunch = openCreatorPrice,
            )
        }
        // 服务主资料屏幕路由
        composable(
            route = Screen.ServiceHostProfile.route,
            arguments = listOf(androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.trim()?.takeIf { it.isNotEmpty() }
            ServiceHostProfileScreen(navController = navController, userId = userId)
        }
        composable(Screen.MyPublishedServices.route) {
            MyPublishedServicesScreen(navController = navController)
        }
        composable(Screen.MyHostIncomingOrders.route) {
            MyHostIncomingOrdersScreen(navController = navController)
        }
        composable(
            route = Screen.PublishedServiceCalendar.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType }),
        ) { entry ->
            val sid = entry.arguments?.getString("serviceId").orEmpty()
            PublishedServiceCalendarScreen(navController = navController, serviceId = sid)
        }
        composable(
            route = Screen.HostServiceOrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { entry ->
            val oid = android.net.Uri.decode(entry.arguments?.getString("orderId").orEmpty())
            HostServiceOrderDetailScreen(navController = navController, orderId = oid)
        }
        composable(Screen.MyIncome.route) {
            MyIncomeScreen(navController = navController)
        }
        composable(Screen.UserInfo.route) {
            UserInfoScreen(navController = navController)
        }
        composable(Screen.RealNameVerification.route) {
            RealNameVerificationScreen(navController = navController)
        }
        // 搜索屏幕路由
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(
            route = Screen.MessageThread.route,
            arguments = listOf(
                androidx.navigation.navArgument("threadId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("inquiryDate") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("serviceTitle") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("from") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("contextServiceId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("contextExperienceId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("peerUserId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId")
            val inquiryDate = backStackEntry.arguments?.getString("inquiryDate")
            val serviceTitle = backStackEntry.arguments?.getString("serviceTitle")
            val from = backStackEntry.arguments?.getString("from")
            val contextServiceId = backStackEntry.arguments?.getString("contextServiceId")
            val contextExperienceId = backStackEntry.arguments?.getString("contextExperienceId")
            val peerUserId = backStackEntry.arguments?.getString("peerUserId")
            MessageThreadScreen(
                navController = navController,
                threadId = threadId,
                initialInquiryDate = inquiryDate,
                initialServiceTitle = serviceTitle,
                entrySource = from,
                initialContextServiceId = contextServiceId,
                initialContextExperienceId = contextExperienceId,
                initialPeerUserId = peerUserId
            )
        }
        
        // 新增页面路由
        composable(Screen.MyProfileEdit.route) {
            MyProfileEditScreen(navController = navController)
        }
        composable(Screen.MyQrCode.route) {
            MyQrCodeScreen(navController = navController)
        }
        composable(Screen.TagManage.route) {
            TagManageScreen(navController = navController)
        }
        composable(
            route = Screen.TagDetail.route,
            arguments = listOf(androidx.navigation.navArgument("tagName") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val tagName = backStackEntry.arguments?.getString("tagName")
            TagDetailScreen(navController = navController, tagName = tagName)
        }
        composable(Screen.Scan.route) {
            ScanScreen(navController = navController)
        }
        composable(
            route = Screen.ServiceHostEdit.route,
            arguments = listOf(
                androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("focus") { 
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val focus = backStackEntry.arguments?.getString("focus")
            ServiceHostEditScreen(navController = navController, userId = userId, focusField = focus)
        }
        composable(
            route = Screen.Login.route,
            arguments = listOf(androidx.navigation.navArgument("phone") { 
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone")
            LoginScreen(navController = navController, initialPhone = phone)
        }
        composable(Screen.WeChatBindPhone.route) {
            WeChatBindPhoneScreen(navController = navController)
        }
        composable(Screen.CompleteName.route) {
            CompleteNameScreen(navController = navController)
        }
        composable(Screen.CompleteProfile.route) {
            CompleteProfileScreen(navController = navController)
        }
        composable(Screen.SystemPermission.route) {
            SystemPermissionScreen(navController = navController)
        }
    }
}

/**
 * 屏幕路由密封类
 * 功能：定义应用中所有可用的屏幕路由，便于统一管理和使用
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object CreateService : Screen(
        "create_service?serviceId={serviceId}&initialUserIds={initialUserIds}&initialTitle={initialTitle}" +
            "&initialCategory={initialCategory}&openBooking={openBooking}"
    ) {
        fun createRoute(
            serviceId: String? = null,
            initialUserIds: String? = null,
            initialTitle: String? = null,
            initialCategory: String? = null,
            openBooking: Boolean = false,
        ): String {
            val sb = StringBuilder("create_service")
            val params = mutableListOf<String>()
            if (serviceId != null) params.add("serviceId=$serviceId")
            if (initialUserIds != null) params.add("initialUserIds=$initialUserIds")
            if (initialTitle != null) params.add("initialTitle=$initialTitle")
            if (initialCategory != null) params.add("initialCategory=${android.net.Uri.encode(initialCategory)}")
            if (openBooking) params.add("openBooking=1") else params.add("openBooking=0")

            if (params.isNotEmpty()) {
                sb.append("?")
                sb.append(params.joinToString("&"))
            }
            return sb.toString()
        }
    }
    object ServiceDetail : Screen("service_detail/{serviceId}?from={from}&openCreatorPrice={openCreatorPrice}") {
        fun createRoute(serviceId: String, from: String? = null, openCreatorPrice: Boolean = false): String {
            val base = "service_detail/$serviceId"
            val params = mutableListOf<String>()
            if (!from.isNullOrBlank()) params.add("from=${android.net.Uri.encode(from)}")
            if (openCreatorPrice) params.add("openCreatorPrice=1") else params.add("openCreatorPrice=0")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    object ServiceHostProfile : Screen("service_host_profile/{userId}") {
        fun createRoute(userId: String) = "service_host_profile/$userId"
    }
    object MyPublishedServices : Screen("my_published_services")
    object MyHostIncomingOrders : Screen("my_host_incoming_orders")
    object PublishedServiceCalendar : Screen("published_service_calendar/{serviceId}") {
        fun createRoute(serviceId: String) = "published_service_calendar/$serviceId"
    }
    object HostServiceOrderDetail : Screen("host_service_order_detail/{orderId}") {
        fun createRoute(orderId: String) =
            "host_service_order_detail/${android.net.Uri.encode(orderId)}"
    }
    object MyIncome : Screen("my_income")
    object UserInfo : Screen("user_info")
    object RealNameVerification : Screen("real_name_verification")
    object Search : Screen("search")
    object ExperienceCategoryFeed : Screen("experience_category_feed/{categoryTitle}") {
        fun createRoute(categoryTitle: String) = "experience_category_feed/${android.net.Uri.encode(categoryTitle)}"
    }
    object ExperienceDetail : Screen("experience_detail/{experienceId}") {
        fun createRoute(experienceId: String) = "experience_detail/${android.net.Uri.encode(experienceId)}"
    }
    object MessageThread : Screen(
        "message_thread/{threadId}?inquiryDate={inquiryDate}&serviceTitle={serviceTitle}&from={from}&contextServiceId={contextServiceId}&contextExperienceId={contextExperienceId}&peerUserId={peerUserId}"
    ) {
        fun createRoute(
            threadId: String,
            inquiryDate: String? = null,
            serviceTitle: String? = null,
            from: String? = null,
            contextServiceId: String? = null,
            contextExperienceId: String? = null,
            peerUserId: String? = null
        ): String {
            val sb = StringBuilder("message_thread/$threadId")
            val params = mutableListOf<String>()
            if (!inquiryDate.isNullOrBlank()) {
                params.add("inquiryDate=${android.net.Uri.encode(inquiryDate)}")
            }
            if (!serviceTitle.isNullOrBlank()) {
                params.add("serviceTitle=${android.net.Uri.encode(serviceTitle)}")
            }
            if (!from.isNullOrBlank()) {
                params.add("from=${android.net.Uri.encode(from)}")
            }
            if (!contextServiceId.isNullOrBlank()) {
                params.add("contextServiceId=${android.net.Uri.encode(contextServiceId)}")
            }
            if (!contextExperienceId.isNullOrBlank()) {
                params.add("contextExperienceId=${android.net.Uri.encode(contextExperienceId)}")
            }
            if (!peerUserId.isNullOrBlank()) {
                params.add("peerUserId=${android.net.Uri.encode(peerUserId)}")
            }
            if (params.isNotEmpty()) {
                sb.append("?")
                sb.append(params.joinToString("&"))
            }
            return sb.toString()
        }
    }
    
    // 新增页面
    object MyProfileEdit : Screen("my_profile_edit")
    object MyQrCode : Screen("my_qr_code")
    object TagManage : Screen("tag_manage")
    object TagDetail : Screen("tag_detail/{tagName}") {
        fun createRoute(tagName: String) = "tag_detail/$tagName"
    }
    object Scan : Screen("scan")
    object ServiceHostEdit : Screen("service_host_edit/{userId}?focus={focus}") {
        fun createRoute(userId: String, focus: String? = null): String {
            return if (focus != null) "service_host_edit/$userId?focus=$focus" else "service_host_edit/$userId"
        }
    }
    object Login : Screen("login?phone={phone}") {
        fun createRoute(phone: String? = null): String {
            return if (phone != null) "login?phone=$phone" else "login"
        }
    }
    object WeChatBindPhone : Screen("wechat_bind_phone")
    object CompleteName : Screen("complete_name")
    object CompleteProfile : Screen("complete_profile")

    // Settings
    object SystemPermission : Screen("system_permission")
}
