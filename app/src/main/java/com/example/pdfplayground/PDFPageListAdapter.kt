package com.example.pdfplayground

import android.graphics.Bitmap
import android.os.Build
import android.text.Editable
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView

// The adapter that hands bitmaps over to the page viewer's RecyclerView.  Considering how many of
// these a real application would have to make, it's a little surprising that the Kotlin people
// haven't found a way to get rid of this boilerplate.
@RequiresApi(Build.VERSION_CODES.Q)
class PDFPageListAdapter(
    private val bitmaps: PageLoader,
    private val currentPageSelector: EditText,
) : RecyclerView.Adapter<PDFPageListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.pdf_list_item, parent, false)
        );
    }

    // Gets the next page it needs from the PageLoader and sets the text of the position indicator
    // to the corresponding number.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.pdfPage.apply {
            setImageBitmap(bitmaps[position]);
        }
        currentPageSelector.text = Editable.Factory.getInstance().newEditable(position.toString())
    }

    override fun getItemCount() = bitmaps.size;

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pdfPage: ImageView = itemView.findViewById(R.id.pdfPage);
    }
}