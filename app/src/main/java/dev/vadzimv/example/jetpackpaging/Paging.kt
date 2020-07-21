package dev.vadzimv.example.jetpackpaging

import android.nfc.tech.MifareUltralight.PAGE_SIZE
import androidx.paging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

typealias PaginationCursor = String?

val NO_PAGE: PaginationCursor = null
val FIRST_PAGE: PaginationCursor = null

interface Page<T> {
    val items: List<T>
    val nextCursor: PaginationCursor
}

data class ItemsPageLoadingParams(val cursor: PaginationCursor, val loadSize: Int)
typealias PageLoader<T> = suspend (ItemsPageLoadingParams) -> Page<T>

fun <T> CoroutineScope.createPagedList(
    pageLoader: PageLoader<T>
): PagedList<T> {
    val scope = this
    val immediateExecutor = Executor { it.run() }
    val config = Config(
        pageSize = 30,
        prefetchDistance = 30 / 2,
        initialLoadSizeHint = PAGE_SIZE
    )
    val dataSource = PaginationDataSource(scope, pageLoader)
    return PagedList.Builder(dataSource, config)
        .setNotifyExecutor(immediateExecutor)
        .setFetchExecutor(immediateExecutor)
        .setInitialKey(FIRST_PAGE)
        .build()
}

private class PaginationDataSource<T>(
    private val scope: CoroutineScope,
    private val pageLoader: PageLoader<T>
) : PageKeyedDataSource<PaginationCursor, T>() {

    override fun loadInitial(
        params: LoadInitialParams<PaginationCursor>,
        callback: LoadInitialCallback<PaginationCursor, T>
    ) {
        scope.launch {
            val result = pageLoader(ItemsPageLoadingParams(FIRST_PAGE, params.requestedLoadSize))
            callback.onResult(result.items, NO_PAGE, result.nextCursor)
        }
    }

    override fun loadAfter(
        params: LoadParams<PaginationCursor>,
        callback: LoadCallback<PaginationCursor, T>
    ) {
        scope.launch {
            val result = pageLoader(ItemsPageLoadingParams(params.key, params.requestedLoadSize))
            callback.onResult(result.items, result.nextCursor)
        }
    }

    override fun loadBefore(
        params: LoadParams<PaginationCursor>,
        callback: LoadCallback<PaginationCursor, T>
    ) {
        error("this should never happen")
    }
}