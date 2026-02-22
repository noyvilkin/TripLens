package com.colman.triplens.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.colman.triplens.R
import com.squareup.picasso.Picasso

/**
 * Simple adapter for ViewPager2 that displays a list of image URLs.
 * Used in both the feed card and the post detail hero section.
 */
class ImagePagerAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_page, parent, false) as ImageView
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = imageUrls[position]
        if (url.isNotEmpty()) {
            Picasso.get()
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .fit()
                .centerCrop()
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}
