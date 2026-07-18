package com.example.myscreentime.fragments.dashboardscreen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myscreentime.R

class AppAdapter(private val itemList: List<RowItem>) :
    RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val currentItem = itemList[position]

        holder.appName.text = currentItem.appName
        holder.usageTime.text = currentItem.usageTime

        if (currentItem.appIcon != null) {
            holder.appIcon.setImageDrawable(currentItem.appIcon)
            holder.appIcon.imageTintList = null
        } else {
            holder.appIcon.setImageResource(R.drawable.ic_app_fallback)
            holder.appIcon.imageTintList = null
        }
    }

    override fun getItemCount(): Int = itemList.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon_row)
        val appName: TextView = itemView.findViewById(R.id.tv_app_name_row)
        val usageTime: TextView = itemView.findViewById(R.id.text_above_app_name_row)
    }
}
