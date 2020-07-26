package dev.vadzimv.example.jetpackpaging

import androidx.lifecycle.*
import androidx.paging.PagedList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PagingExampleViewModel(
    private val getItemsUseCase: GetItemsUseCase
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

    private var currentPagesScope = viewModelScope.createPagesScope(::getPage)
        set(value) {
            field.cancel()
            field = value
            _pages.value = value.pages
        }
    private val _pages = MutableLiveData<PagedList<ExampleListItem>>().apply {
        value = currentPagesScope.pages
    }
    val pages: LiveData<PagedList<ExampleListItem>> get() = _pages

    fun refresh() {
        currentPagesScope = viewModelScope.createPagesScope(::getPage)
    }

    fun removeItemWithId(id: Long) {
        val loadedItems = currentPagesScope.pages.snapshot().filterNotNull()
        val updatedList = loadedItems.filter { it.id != id }
        currentPagesScope = viewModelScope.createPagesScope(::getPage, SimplePage(updatedList, currentPagesScope.nextCursorToLoad))
    }

    private suspend fun getPage(args: PageLoadingArgs): Page<ExampleListItem> {
        _state.value = State.Loading
        val page = loadItemsPage(args)
        _state.value = State.Loaded(page.itemsCount)
        return page
    }

    private suspend fun loadItemsPage(pageArgs: PageLoadingArgs): ItemsPagedResult.ItemsPage<ExampleListItem> {
        return when (val loadPageResult = getItemsUseCase.requestPage(pageArgs)) {
            is ItemsPagedResult.ItemsPage -> loadPageResult
            is ItemsPagedResult.Error -> {
                retryWhenUserAskForIt(pageArgs)
            }
        }
    }

    private suspend fun retryWhenUserAskForIt(args: PageLoadingArgs): ItemsPagedResult.ItemsPage<ExampleListItem> {
        val retryAfterUserAction = CompletableDeferred<ItemsPagedResult.ItemsPage<ExampleListItem>>()
        _state.value =
            State.RetryableError {
                viewModelScope.launch {
                    retryAfterUserAction.complete(loadItemsPage(args))
                }
            }
        return retryAfterUserAction.await()
    }
}

class PagingExampleViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass == PagingExampleViewModel::class.java) {
            return PagingExampleViewModel(FakeDataSource()) as T
        }
        throw IllegalArgumentException("can't create view model of type ${modelClass.name}")
    }
}