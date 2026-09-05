package com.example.myscreentime.fragments.dashboardscreen

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import androidx.recyclerview.widget.RecyclerView
import com.example.myscreentime.R
import kotlinx.coroutines.*

class AppAdapter(private val itemList: List<RowItem>) :
    RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val currentItem = itemList[position]

        holder.appName.text = currentItem.appName
        holder.usageTime.text = currentItem.usageTime
        holder.progressIndicator.progress = (currentItem.progress * 100).toInt()

        // Set fallback first to avoid showing wrong icon for recycled view
        holder.appIcon.setImageResource(R.drawable.ic_app_fallback)
        
        // Cancel any previous load job for this holder
        holder.iconLoadJob?.cancel()
        
        // Load icon in background
        holder.iconLoadJob = adapterScope.launch {
            val icon = withContext(Dispatchers.IO) {
                try {
                    val pm = holder.itemView.context.packageManager
                    pm.getApplicationIcon(currentItem.packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            if (isActive) {
                if (icon != null) {
                    holder.appIcon.setImageDrawable(icon)
                } else {
                    holder.appIcon.setImageResource(R.drawable.ic_app_fallback)
                }
                holder.appIcon.imageTintList = null
            }
        }
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        super.onViewRecycled(holder)
        holder.iconLoadJob?.cancel()
    }

    override fun getItemCount(): Int = itemList.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon_row)
        val appName: TextView = itemView.findViewById(R.id.tv_app_name_row)
        val usageTime: TextView = itemView.findViewById(R.id.text_above_app_name_row)
        val progressIndicator: LinearProgressIndicator = itemView.findViewById(R.id.app_usage_progress)
        var iconLoadJob: Job? = null
    }
}
