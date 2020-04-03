package dev.vadzimv.example.jetpackpaging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

typealias PaginationCursor = String?

val NO_PAGE: PaginationCursor = null
val FIRST_PAGE: PaginationCursor = null

sealed class ItemsPagedResult<T> {
    data class ItemsPage<T>(
        val itemsCount: Int,
        val items: List<T>,
        val nextCursor: PaginationCursor
    ) : ItemsPagedResult<T>()

    data class Error<T>(val error: Throwable) : ItemsPagedResult<T>()

    fun <NewT> map(mapper: (List<T>) -> List<NewT>): ItemsPagedResult<NewT> = when (this) {
        is ItemsPage<T> -> ItemsPage(
            itemsCount = itemsCount,
            items = mapper(items),
            nextCursor = nextCursor
        )
        is Error<T> -> Error(error)
    }
}

class Item

interface ExampleUseCase {
    suspend fun requestPage(cursor: PaginationCursor, loadSize: Int): ItemsPagedResult<Item>
}

class PagingExampleViewModel(
    private val exampleUseCase: ExampleUseCase
) : ViewModel() {

    sealed class State {
        object Loading : State()
        class RetryableError(private val retry: () -> Unit) : State() {
            fun retry() = retry.invoke()
        }

        data class Loaded(val totalItemsCount: Int) : State()
    }

    private val _state = MutableLiveData<State>()
    val state: LiveData<State> get() = _state

    val pages = viewModelScope.transformToJetpackPagedResult(
        object : ItemsPageLoader<Item> {
            override suspend fun loadPage(
                cursor: PaginationCursor,
                loadSize: Int
            ): ItemsPagedResult.ItemsPage<Item> {
                return loadItemsPage(cursor, loadSize)
            }
        }
    )

    private suspend fun loadItemsPage(
        cursor: PaginationCursor,
        loadSize: Int
    ): ItemsPagedResult.ItemsPage<Item> {
        return TODO()
    }
}

fun <T> CoroutineScope.transformToJetpackPagedResult(pageLoader: ItemsPageLoader<T>): LiveData<PagedList<T>> {
    val scope = this
    return object : DataSource.Factory<PaginationCursor, T>() {
        override fun create(): DataSource<PaginationCursor, T> {
            return ItemsPaginationDataSource(scope, pageLoader)
        }
    }.toLiveData(Config(30, prefetchDistance = 30, enablePlaceholders = false))
}

interface ItemsPageLoader<T> {
    suspend fun loadPage(cursor: PaginationCursor, loadSize: Int): ItemsPagedResult.ItemsPage<T>
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
            val result = pageLoader.loadPage(FIRST_PAGE, params.requestedLoadSize)
            callback.onResult(result.items, NO_PAGE, result.nextCursor)
        }
    }

    override fun loadAfter(
        params: LoadParams<PaginationCursor>,
        callback: LoadCallback<PaginationCursor, T>
    ) {
        scope.launch {
            val result = pageLoader.loadPage(params.key, params.requestedLoadSize)
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