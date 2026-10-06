package com.example.controledegastos.ui.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.controledegastos.listeners.OnClickInterface
import com.example.controledegastos.R
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.databinding.AdapterlayoutBinding
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.ui.model.TransactionDate

class MyAdapter(private val listenerInterface: OnClickInterface) :
    ListAdapter<Items, MyAdapter.Mvh>(DIFF) {

    class Mvh(binding: AdapterlayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val desc = binding.DescTv
        val obs = binding.ObsTv
        val delete = binding.deleteIcon
        val iOtext = binding.textIO
        val payMethod = binding.textPayMethod
        val value = binding.textValue
        val data = binding.textData
        val month = binding.textMonth
        val editIcon = binding.editIcon
        val ctg = binding.CtgTv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Mvh {

        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.adapterlayout, parent, false)
        return Mvh(AdapterlayoutBinding.bind(view))

    }

    override fun onBindViewHolder(holder: Mvh, position: Int) {

        val model = getItem(position)

        val description = model.description.preview(120)
        val observation = model.observation.preview(650)
        holder.desc.text = description
        holder.obs.text = observation
        holder.obs.isVisible = observation.isNotBlank()
        holder.iOtext.text = model.io
        holder.payMethod.text = model.paymentMethod.preview(120)
        holder.data.text = TransactionDate.format(model.occurredAtMillis)
        holder.month.text = model.yearMonth.toString()
        holder.ctg.text = model.category.preview(120)

        holder.iOtext.setTextColor(
            ContextCompat.getColor(holder.itemView.context,
                if (model.io == "Entrada") R.color.chartGreen else R.color.chartRed)
        )

        val value = Money.format(model.amountCents)
        holder.value.text = value

        holder.value.setTextColor(
            ContextCompat.getColor(holder.itemView.context,
                if (model.amountCents < 0) R.color.chartRed else R.color.chartGreen)
        )

        holder.editIcon.setOnClickListener { listenerInterface.onClickEdit(model, value) }
        holder.delete.setOnClickListener { listenerInterface.onClickDelete(model.id) }
        holder.editIcon.contentDescription = holder.itemView.context.getString(R.string.edit_item, description)
        holder.delete.contentDescription = holder.itemView.context.getString(R.string.delete_item, description)

    }

    fun setItems(newItems: List<Items>) {
        submitList(newItems)
    }

    private fun String.preview(limit: Int): String = if (length > limit) take(limit) + "…" else this

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Items>() {
            override fun areItemsTheSame(oldItem: Items, newItem: Items) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Items, newItem: Items) = oldItem == newItem
        }
    }

}





