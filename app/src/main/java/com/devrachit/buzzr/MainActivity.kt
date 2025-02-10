package com.devrachit.buzzr

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import com.devrachit.buzzr.ui.theme.BuzzrTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Observer

class MainActivity : ComponentActivity() {

    private lateinit var networkStateObserver: NetworkStateObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars =
            false
        enableEdgeToEdge()

        networkStateObserver = NetworkStateObserver(this)


        setContent {
            BuzzrTheme {
                val isLoading = remember { mutableStateOf(true) }
                val showErrorDialog = remember { mutableStateOf(false) }
                val isNetworkAvailable = remember { mutableStateOf(true) }

                networkStateObserver.getNetworkState().observe(this, Observer { isConnected ->
                    isNetworkAvailable.value = isConnected
                    if (!isConnected) {
                        showErrorDialog.value = true
                    } else {
                        showErrorDialog.value = false
                    }
                })

                if (isLoading.value)
                    LoadingScreen()
                if (showErrorDialog.value) {
                    ErrorDialog(onDismiss = {
                        networkStateObserver.checkNetworkState() })
                }
                if (!isNetworkAvailable.value) {
                    ErrorDialog(onDismiss = {
                        networkStateObserver.checkNetworkState() })
                } else {
                    WebViewScreen(
                        windowColorSetup = {
                            window.statusBarColor = android.graphics.Color.BLACK
                            window.navigationBarColor = android.graphics.Color.BLACK
                        },
                        onPageFinished = { isLoading.value = false },
                        onError = {
                            showErrorDialog.value = true
                            isLoading.value = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Dialog(onDismissRequest = { }) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = colorResource(id=R.color.surface_card_normal_default),
        onDismissRequest = onDismiss,
        title = { Text(text = "Network Error") },
        text = { Text(text = "Please check your internet connection and try again") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Retry")
            }
        }
    )
}

@Composable
fun WebViewScreen(windowColorSetup: () -> Unit, onPageFinished: () -> Unit, onError: () -> Unit) {
    AndroidView(
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        view?.loadUrl("https://buzzr.silive.in/")
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        windowColorSetup()
                        onPageFinished()
                        view?.evaluateJavascript(
                            """
    document.querySelector('.w-fit.text-sm.mt-auto.self-center.text-lprimary').style.visibility = 'hidden';
    """.trimIndent()
                        ) { result ->
                            Log.d("JavaScript", "Result: $result")
                        }

                    }


                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        onError()
                        super.onReceivedError(view, request, error)
                        Log.e("WebViewError", "Error loading: ${error?.description}")

                    }
                }
                loadUrl("https://buzzr.silive.in/")
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    )
}