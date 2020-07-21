package dev.vadzimv.example.jetpackpaging

import androidx.lifecycle.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

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

    val pages = viewModelScope.transformToJetpackPagedResult {
        _state.value =
            State.Loading
        val page = loadItemsPage(it)
        _state.value =
            State.Loaded(
                page.itemsCount
            )
        page
    }

    private suspend fun loadItemsPage(pageParams: ItemsPageLoadingParams): ItemsPagedResult.ItemsPage<ExampleListItem> {
        val loadPageResult = getItemsUseCase.requestPage(pageParams)
        return when (loadPageResult) {
            is ItemsPagedResult.ItemsPage -> loadPageResult
            is ItemsPagedResult.Error -> {
                retryWhenUserAskForIt(pageParams)
            }
        }
    }

    private suspend fun retryWhenUserAskForIt(params: ItemsPageLoadingParams): ItemsPagedResult.ItemsPage<ExampleListItem> {
        val retryAfterUserAction = CompletableDeferred<ItemsPagedResult.ItemsPage<ExampleListItem>>()
        _state.value =
            State.RetryableError {
                viewModelScope.launch {
                    retryAfterUserAction.complete(loadItemsPage(params))
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