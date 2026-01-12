package com.example.unlocklogger

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.unlocklogger.data.UnlockEvent
import java.text.SimpleDateFormat
import java.util.*

class UnlockEventsAdapter :
    ListAdapter<UnlockEvent, UnlockEventsAdapter.VH>(DIFF) {

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_unlock_event, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ev = getItem(position)
        holder.timeText.text = formatter.format(Date(ev.timestamp))
        holder.relTimeText.text = DateUtils.getRelativeTimeSpanString(
            ev.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        // optional: set icon tint or content description
        holder.icon.contentDescription = "Unlock at ${holder.timeText.text}"
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val relTimeText: TextView = view.findViewById(R.id.relTimeText)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UnlockEvent>() {
            override fun areItemsTheSame(oldItem: UnlockEvent, newItem: UnlockEvent): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: UnlockEvent, newItem: UnlockEvent): Boolean =
                oldItem.timestamp == newItem.timestamp
        }
    }
}
