package dev.farukh.grpcchat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.grpc.CallOptions
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import text
import java.util.concurrent.TimeUnit

class GrpcViewModel: ViewModel() {
    var uiText by mutableStateOf("")
    var ready by mutableStateOf(false)

    private var grpcTextStream = MutableSharedFlow<Example.Text>(0, 1)
    private var client: TextingGrpcKt.TextingCoroutineStub? = null

    init {
        try {
            val channel = ManagedChannelBuilder
                .forAddress("10.0.2.2", 8080)
                .usePlaintext()
                .build()
            client = TextingGrpcKt.TextingCoroutineStub(
                channel = channel,
                callOptions = CallOptions.DEFAULT
            )
            ready = true
            try {
                client?.initChat(grpcTextStream)
                    ?.onEach { Log.i("reply", it.text) }
                    ?.flowOn(Dispatchers.IO)
                    ?.launchIn(viewModelScope)
                ready = true
            } catch (e: StatusException) {
                Log.e("reply", "connect error", e)
            }
        } catch (e: Exception) {
            Log.e("reply", "connect error", e)
            ready = false
        }
    }

    fun sendText() {
        viewModelScope.launch {
            grpcTextStream.emit(uiText.toGrpc())
            uiText = ""
            Log.i("reply", "sendText: $uiText")
        }
    }

    fun sendText2() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client?.sendText(uiText.toGrpc())
                uiText = ""
                Log.i("reply", "sent with success")
            } catch (e: StatusException) {
                Log.e("reply", "send error", e)
            }
        }
    }
}

private fun String.toGrpc(): Example.Text {
    return text { text = this@toGrpc }
}
