package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CategoriesAdapter(
    private var categories: List<CategoryEntity>,
    private val onCategoryClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.cardCategory)
        val iconView: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val nameView: TextView = view.findViewById(R.id.tvCategoryName)
        val subcategoryView: TextView = view.findViewById(R.id.tvSubcategory)
        val budgetView: TextView = view.findViewById(R.id.tvBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.iconView.setImageResource(category.iconResId)
        holder.nameView.text = category.name
        holder.subcategoryView.text = category.subcategory ?: ""
        holder.budgetView.text = if (category.budget != null) {
            "R${String.format("%.2f", category.budget)}"
        } else {
            "No budget set"
        }

        holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor(category.backgroundColor))

        holder.cardView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CategoryExpensesActivity::class.java).apply {
                putExtra("CATEGORY_ID", category.id)
                putExtra("CATEGORY_NAME", category.name)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<CategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
