package soko.ekibun.assistant.util

import android.widget.Toast
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

object CoroutineUtil: CoroutineScope by MainScope() {
    private val jobCollection = ConcurrentHashMap<String, Job>()
    fun cancel(check: (String) -> Boolean) {
        jobCollection.keys.forEach {
            if (check(it)) jobCollection.remove(it)
        }
    }

    fun subscribe(
        onError: (t: Throwable) -> Unit = {},
        onComplete: () -> Unit = {},
        key: String? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val oldJob = if (key.isNullOrEmpty()) null else jobCollection[key]
        return launch {
            try {
                oldJob?.cancelAndJoin()
                block.invoke(this)
            } catch (_: CancellationException) {
            } catch (t: Throwable) {
                t.printStackTrace()
                onError(t)
            }
            if (isActive) onComplete()
        }.also {
            if (!key.isNullOrEmpty()) jobCollection[key] = it
        }
    }
}