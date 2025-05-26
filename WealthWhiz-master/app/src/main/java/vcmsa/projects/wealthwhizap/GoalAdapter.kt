package vcmsa.projects.wealthwhizap


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat

class GoalAdapter(private var goals: List<Goal>) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    interface OnGoalClickListener {
        fun onGoalClick(goal: Goal)
        fun onGoalDelete(goal: Goal)
    }

    private var listener: OnGoalClickListener? = null

    fun setOnGoalClickListener(listener: OnGoalClickListener) {
        this.listener = listener
    }

    fun updateGoals(newGoals: List<Goal>) {
        goals = newGoals
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount(): Int = goals.size

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val monthTextView: TextView = itemView.findViewById(R.id.textViewMonth)
        private val minGoalTextView: TextView = itemView.findViewById(R.id.textViewMinGoal)
        private val maxGoalTextView: TextView = itemView.findViewById(R.id.textViewMaxGoal)
        private val budgetTextView: TextView = itemView.findViewById(R.id.textViewBudget)

        private val randFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 2
            currency = java.util.Currency.getInstance("ZAR")
        }

        fun bind(goal: Goal) {
            monthTextView.text = goal.month
            minGoalTextView.text = "Min: ${randFormat.format(goal.minGoal)}"
            maxGoalTextView.text = "Max: ${randFormat.format(goal.maxGoal)}"
            budgetTextView.text = "Budget: ${randFormat.format(goal.monthlyBudget)}"

            itemView.setOnClickListener {
                listener?.onGoalClick(goal)
            }

            itemView.setOnLongClickListener {
                listener?.onGoalDelete(goal)
                true
            }
        }
    }
}
