package com.naztube.app

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(private val onClick: (Video) -> Unit) : RecyclerView.Adapter<VideoAdapter.Holder>() {
    private var videos = emptyList<Video>()
    fun submit(items: List<Video>) { videos = items; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val card = LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(30, 43, 45)); setPadding(20, 18, 20, 18); layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(10, 10, 10, 10) } }
        val icon = TextView(parent.context).apply { text = "▶"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(Color.rgb(39, 183, 166)); setBackgroundColor(Color.rgb(19, 30, 32)); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 116) }
        val title = TextView(parent.context).apply { textSize = 16f; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); setPadding(0, 14, 0, 4) }
        val category = TextView(parent.context).apply { textSize = 13f; setTextColor(Color.rgb(170, 190, 190)) }
        card.addView(icon); card.addView(title); card.addView(category); return Holder(card, title, category)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) { val video = videos[position]; holder.title.text = video.title; holder.category.text = video.category; holder.itemView.setOnClickListener { onClick(video) } }
    override fun getItemCount() = videos.size
    class Holder(view: LinearLayout, val title: TextView, val category: TextView) : RecyclerView.ViewHolder(view)
}
