package com.example.controledegastos.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.controledegastos.listeners.OnClickInterface
import com.example.controledegastos.R
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.databinding.AdapterlayoutBinding
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.ui.model.TransactionDate

class MyAdapter(val listenerInterface: OnClickInterface) : RecyclerView.Adapter<MyAdapter.Mvh>() {

    private var itemsList: List<Items> = emptyList()

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

        val model = itemsList[position]

        holder.desc.text = model.description
        holder.obs.text = model.observation
        holder.iOtext.text = model.io
        holder.payMethod.text = model.paymentMethod
        holder.data.text = TransactionDate.format(model.occurredAtMillis)
        holder.month.text = model.yearMonth.toString()
        holder.ctg.text = model.category

        holder.iOtext.setTextColor(
            if (model.io == "Entrada") Color.parseColor("#1f4011")
            else Color.parseColor("#960802")
        )

        val value = Money.format(model.amountCents)
        holder.value.text = value

        holder.value.setTextColor(
            if (model.amountCents < 0) Color.parseColor("#ed1313")
            else Color.parseColor("#1ff024")
        )

        holder.editIcon.setOnClickListener { listenerInterface.onClickEdit(model, value) }
        holder.delete.setOnClickListener { listenerInterface.onClickDelete(model.id) }

    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    fun setItems(newItems: List<Items>) {
        val diffResult = DiffUtil.calculateDiff(ItemDiffCallback(itemsList, newItems))
        itemsList = newItems
        diffResult.dispatchUpdatesTo(this)
    }

}





