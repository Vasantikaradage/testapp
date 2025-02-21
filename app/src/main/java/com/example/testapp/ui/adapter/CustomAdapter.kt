package com.example.testapp.ui.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.testapp.AddActivity
import com.example.testapp.R
import com.example.testapp.ui.ItemsViewModel
import com.example.testapp.ui.home.HomeFragment


class CustomAdapter(private  val context: HomeFragment, private val mList: List<ItemsViewModel>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
    private var onClickListener: OnClickListener? = null
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = mList[position]
        Log.d("@@@",""+item.designation)
        Log.d("@@@1",""+item.username)
        Log.d("@@@2",""+item.dob)

        // sets the image to the imageview from our itemHolder class
      //  holder.imageView.setImageResource(ItemsViewModel.image)

        // sets the text to the textview from our itemHolder class
        holder.username.text = item.username
        holder.designation.text = item.designation
        holder.dob.text = item.dob

        holder.close.setOnClickListener{
            onClickListener?.onClick(position, item)
        }

        holder.edit.setOnClickListener{
            val intent = Intent(it.context, AddActivity::class.java)
            intent.putExtra("value","2")
            intent.putExtra("username",item.username)

            intent.putExtra("dob", item.dob)
            intent.putExtra("designation", item.designation)
            intent.putExtra("docId", item.documentId)

            context.startActivity(intent)
            notifyDataSetChanged()
        }



    }

    interface OnClickListener {
        fun onClick(position: Int, model: ItemsViewModel)
    }
    fun setOnClickListener(listener: OnClickListener) {
        this.onClickListener = listener
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageview)
        val username: TextView = itemView.findViewById(R.id.userName)
        val designation: TextView = itemView.findViewById(R.id.designation)
        val dob: TextView = itemView.findViewById(R.id.dob)
        val close: ImageView = itemView.findViewById(R.id.imagedelete)
        val edit: ImageView = itemView.findViewById(R.id.imageedit)
    }

    companion object {
        fun setOnClickListener(onClickListener: CustomAdapter.OnClickListener) {

        }
    }
}