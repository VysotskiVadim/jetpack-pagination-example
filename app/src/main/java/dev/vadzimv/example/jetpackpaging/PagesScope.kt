package dev.vadzimv.example.jetpackpaging

import androidx.paging.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class PagesScope<T> internal constructor(
    override val coroutineContext: CoroutineContext,
    private val pageLoader: PageLoader<T>,
    initialContent: Page<T>? = null
) : CoroutineScope {

    var nextCursorToLoad: PaginationCursor = initialContent?.nextCursor
        private set

    val pages: PagedList<T> by lazy {
        createPagedList(::loadPage, initialContent)
    }

    private suspend fun loadPage(args: PageLoadingArgs): Page<T> {
        nextCursorToLoad = args.cursor
        val result = pageLoader(args)
        nextCursorToLoad = result.nextCursor
        return result
    }
}

fun <T> CoroutineScope.createPagesScope(
    pageLoader: PageLoader<T>,
    initialContent: Page<T>? = null
): PagesScope<T> {
    val newContext = coroutineContext + SupervisorJob(coroutineContext[Job])
    return PagesScope(newContext, pageLoader, initialContent)
}