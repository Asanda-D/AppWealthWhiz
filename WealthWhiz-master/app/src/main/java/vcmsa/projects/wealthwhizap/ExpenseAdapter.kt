package vcmsa.projects.wealthwhizap

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val context: Context,
    private var expenses: List<Expense>,
    private var categories: MutableMap<String, CategoryEntity> = mutableMapOf()
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    interface OnImageClickListener {
        fun onImageClick(imageUri: String)
    }

    interface OnItemClickListener {
        fun onEditClick(expense: Expense)
        fun onDeleteClick(expense: Expense)
    }

    private var onImageClickListener: OnImageClickListener? = null
    private var onItemClickListener: OnItemClickListener? = null

    fun setOnImageClickListener(listener: OnImageClickListener) {
        onImageClickListener = listener
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }

    fun updateCategories(newCategories: Map<String, CategoryEntity>) {
        categories.clear()
        categories.putAll(newCategories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        val category = categories[expense.categoryId]

        // Set category icon and name
        if (category != null) {
            holder.categoryIcon.setImageResource(category.iconResId)
            holder.categoryName.text = category.name
        } else {
            holder.categoryIcon.setImageResource(R.drawable.ic_category_default)
            holder.categoryName.text = "Uncategorized"
        }

        // Format and set date & time
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(expense.dateTime)
            holder.dateTime.text = date?.let { outputFormat.format(it) } ?: expense.dateTime
        } catch (e: Exception) {
            holder.dateTime.text = expense.dateTime
        }

        // Set amount
        holder.amount.text = String.format("R%.2f", expense.amount)

        // Set notes
        holder.notes.text = expense.notes ?: "No notes"

        // Set expense image if available
        if (expense.imageUri != null) {
            holder.expenseImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(expense.imageUri)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.expenseImage)

            holder.expenseImage.setOnClickListener {
                onImageClickListener?.onImageClick(expense.imageUri)
            }
        } else {
            holder.expenseImage.visibility = View.GONE
        }

        // Set long click listener for options
        holder.itemView.setOnLongClickListener {
            showOptionsDialog(context, expense)
            true
        }
    }

    override fun getItemCount(): Int = expenses.size

    // Helper function to show edit/delete options
    private fun showOptionsDialog(context: Context, expense: Expense) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(context)
            .setTitle("Choose an option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onItemClickListener?.onEditClick(expense) // Edit
                    1 -> onItemClickListener?.onDeleteClick(expense) // Delete
                }
            }
            .show()
    }

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val categoryName: TextView = view.findViewById(R.id.tvCategory)
        val dateTime: TextView = view.findViewById(R.id.tvDateTime)
        val amount: TextView = view.findViewById(R.id.tvAmount)
        val notes: TextView = view.findViewById(R.id.tvNotes)
        val expenseImage: ImageView = view.findViewById(R.id.ivExpenseImage)
    }
}