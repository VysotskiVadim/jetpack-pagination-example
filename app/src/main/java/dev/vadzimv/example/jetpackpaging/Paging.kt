package dev.vadzimv.example.jetpackpaging

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

class SimplePage<T>(
    override val items: List<T>,
    override val nextCursor: PaginationCursor
) : Page<T>

data class PageLoadingArgs(val cursor: PaginationCursor, val loadSize: Int)
typealias PageLoader<T> = suspend (PageLoadingArgs) -> Page<T>

fun <T> CoroutineScope.createPagedList(
    pageLoader: PageLoader<T>,
    initialContent: Page<T>? = null
): PagedList<T> {
    val scope = this
    val immediateExecutor = Executor { it.run() }
    val config = Config(
        pageSize = 30,
        prefetchDistance = 30 / 2,
        initialLoadSizeHint = 30
    )
    val dataSource = PaginationDataSource(scope, pageLoader, initialContent)
    return PagedList.Builder(dataSource, config)
        .setNotifyExecutor(immediateExecutor)
        .setFetchExecutor(immediateExecutor)
        .setInitialKey(FIRST_PAGE)
        .build()
}

private class PaginationDataSource<T>(
    private val scope: CoroutineScope,
    private val pageLoader: PageLoader<T>,
    private val initialContent: Page<T>?
) : PageKeyedDataSource<PaginationCursor, T>() {

    override fun loadInitial(
        params: LoadInitialParams<PaginationCursor>,
        callback: LoadInitialCallback<PaginationCursor, T>
    ) {
        if (initialContent != null) {
            callback.onResult(initialContent.items, NO_PAGE, initialContent.nextCursor)
        } else {
            scope.launch {
                val result = pageLoader(PageLoadingArgs(FIRST_PAGE, params.requestedLoadSize))
                callback.onResult(result.items, NO_PAGE, result.nextCursor)
            }
        }
    }

    override fun loadAfter(
        params: LoadParams<PaginationCursor>,
        callback: LoadCallback<PaginationCursor, T>
    ) {
        scope.launch {
            val result = pageLoader(PageLoadingArgs(params.key, params.requestedLoadSize))
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