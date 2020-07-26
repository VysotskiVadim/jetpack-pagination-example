package dev.vadzimv.example.jetpackpaging

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_example.*

class ExampleActivity : AppCompatActivity() {

    private val viewModel: PagingExampleViewModel by viewModels { PagingExampleViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        setSupportActionBar(toolbar)
        val exampleItemsAdapter = ExamplePagedListAdapter { id ->
            viewModel.removeItemWithId(id)
        }
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = exampleItemsAdapter
            setHasFixedSize(true)
        }
        viewModel.pages.observe(this) {
            exampleItemsAdapter.submitList(it)
        }
        viewModel.state.observe(this) { state ->
            when (state) {
                PagingExampleViewModel.State.Loading -> {
                    swipeToRefresh.isRefreshing = true
                }
                is PagingExampleViewModel.State.RetryableError -> {
                    swipeToRefresh.isRefreshing = false
                    Snackbar.make(rootView, "Error loading items", LENGTH_INDEFINITE)
                        .setAction("Retry") { state.retry() }
                        .show()
                }
                is PagingExampleViewModel.State.Loaded -> {
                    swipeToRefresh.isRefreshing = false
                    // show total items count
                }
            }
        }
        swipeToRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }
}
