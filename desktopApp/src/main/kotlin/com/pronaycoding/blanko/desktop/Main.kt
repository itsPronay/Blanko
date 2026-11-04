package com.pronaycoding.blanko.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.pronaycoding.blanko.BlankoApp
import com.pronaycoding.blanko.appModule
import com.pronaycoding.blanko.core.service.playback.GlobalPlaybackState
import com.pronaycoding.blanko.core.ui.theme.AppLanguageProvider
import com.pronaycoding.blanko.resources.Res
import com.pronaycoding.blanko.resources.app_name
import com.pronaycoding.blanko.resources.notification_action_pause
import com.pronaycoding.blanko.resources.notification_action_play
import com.pronaycoding.blanko.resources.tray_quit
import com.pronaycoding.blanko.resources.tray_show
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.awt.Dimension

fun main() {
    startKoin {
        modules(appModule, desktopPlatformModule(AppDirectories.data))
    }

    application {
        val icon = remember { BitmapPainter(useResource("icon.png", ::loadImageBitmap)) }
        val windowState = rememberWindowState(size = DpSize(1000.dp, 760.dp))
        var windowVisible by remember { mutableStateOf(true) }
        val trayAvailable = remember { isTraySupported }

        // Compose Desktop does not provide a ViewModelStoreOwner by default; the navigation graph
        // and the Koin view models need one.
        val viewModelStoreOwner =
            remember {
                object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                }
            }

        // Ambient sounds should keep playing while the window is hidden, so closing the window only
        // hides it when a tray icon exists to bring it back.
        val quit = {
            GlobalContext.get().get<com.pronaycoding.blanko.desktopaudio.AudioMixer>().close()
            exitApplication()
        }

        CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
            if (trayAvailable) {
                KoinContext {
                    AppLanguageProvider {
                        PlaybackTray(
                            icon = icon,
                            onShow = { windowVisible = true },
                            onQuit = quit,
                        )
                    }
                }
            }

            Window(
                onCloseRequest = { if (trayAvailable) windowVisible = false else quit() },
                visible = windowVisible,
                state = windowState,
                title = "Blanko",
                icon = icon,
            ) {
                window.minimumSize = Dimension(420, 560)
                KoinContext {
                    BlankoApp()
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.ui.window.ApplicationScope.PlaybackTray(
    icon: BitmapPainter,
    onShow: () -> Unit,
    onQuit: () -> Unit,
) {
    val playbackState = remember { GlobalContext.get().get<GlobalPlaybackState>() }
    val canPlay by playbackState.canPlay.collectAsState()

    Tray(
        icon = icon,
        tooltip = stringResource(Res.string.app_name),
        onAction = onShow,
        menu = {
            Item(stringResource(Res.string.tray_show), onClick = onShow)
            Item(
                stringResource(if (canPlay) Res.string.notification_action_pause else Res.string.notification_action_play),
                onClick = { playbackState.togglePlayPause() },
            )
            Separator()
            Item(stringResource(Res.string.tray_quit), onClick = onQuit)
        },
    )
}
