package dev.vadzimv.example.jetpackpaging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_example.view.*

class ExamplePagedListAdapter: PagedListAdapter<ExampleListItem, ExampleListItemViewHolder>(
    AsyncDifferConfig.Builder(ExampleItemsComporator())
        .setBackgroundThreadExecutor(pagesDiffCallbackExecutor)
        .build()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleListItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_example, parent, false)
        return ExampleListItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExampleListItemViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }
}

class ExampleListItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val description = view.itemDescription

    fun bind(item: ExampleListItem) {
        description.text = item.description
    }
}