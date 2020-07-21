package dev.vadzimv.example.jetpackpaging

import androidx.recyclerview.widget.DiffUtil

class ExampleItemsComporator : DiffUtil.ItemCallback<ExampleListItem>() {
    override fun areItemsTheSame(oldItem: ExampleListItem, newItem: ExampleListItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExampleListItem, newItem: ExampleListItem): Boolean {
        return oldItem == newItem
    }
}