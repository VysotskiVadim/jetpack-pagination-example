package dev.vadzimv.example.jetpackpaging

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

/**
 * In real application it's better to use models in use case,
 * and then map them to view objects on view model level.
 * But I want to keep example as simple as possible.
 */
interface GetItemsUseCase {
    suspend fun requestPage(
        pageParams: ItemsPageLoadingParams
    ): ItemsPagedResult<ExampleListItem>
}

sealed class ItemsPagedResult<T> {
    data class ItemsPage<T>(
        val itemsCount: Int,
        override val items: List<T>,
        override val nextCursor: PaginationCursor
    ) : ItemsPagedResult<T>(), Page<T>

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

class FakeDataSource : GetItemsUseCase {

    private val totalCount = 101;
    private val errorProbability = 0.5f

    override suspend fun requestPage(
        pageParams: ItemsPageLoadingParams
    ): ItemsPagedResult<ExampleListItem> {
        delay(700) // emulate network delay
        val isErrorThisTime = Random.nextFloat() > errorProbability
        return if (isErrorThisTime) {
            ItemsPagedResult.Error(Error("test error"))
        } else {
            val startIndex = pageParams.cursor?.toIntOrNull() ?: 0
            val finishIndex = min(startIndex + pageParams.loadSize, totalCount)
            val isNextPageTheLast = finishIndex == totalCount
            val items = (startIndex..finishIndex).map { id ->
                ExampleListItem(id.toLong(), "Test item #$id")
            }
            val nextPageCursor = if (isNextPageTheLast) NO_PAGE else finishIndex.toString()
            ItemsPagedResult.ItemsPage(totalCount, items, nextPageCursor)
        }
    }
}