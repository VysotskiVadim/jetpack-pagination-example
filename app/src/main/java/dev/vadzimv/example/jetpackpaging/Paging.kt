package dev.vadzimv.example.jetpackpaging

import androidx.lifecycle.LiveData
import androidx.paging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

typealias PaginationCursor = String?

val NO_PAGE: PaginationCursor = null
val FIRST_PAGE: PaginationCursor = null

interface Page<T> {
    val items: List<T>
    val nextCursor: PaginationCursor
}

data class ItemsPageLoadingParams(val cursor: PaginationCursor, val loadSize: Int)
typealias ItemsPageLoader<T> = suspend (ItemsPageLoadingParams) -> Page<T>

fun <T> CoroutineScope.transformToJetpackPagedResult(pageLoader: ItemsPageLoader<T>): LiveData<PagedList<T>> {
    val scope = this
    return object : DataSource.Factory<PaginationCursor, T>() {
        override fun create(): DataSource<PaginationCursor, T> {
            return ItemsPaginationDataSource(scope, pageLoader)
        }
    }.toLiveData(Config(30, prefetchDistance = 30, enablePlaceholders = false))
}

private class ItemsPaginationDataSource<T>(
    private val scope: CoroutineScope,
    private val pageLoader: ItemsPageLoader<T>
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