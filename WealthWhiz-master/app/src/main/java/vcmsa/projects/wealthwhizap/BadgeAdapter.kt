package vcmsa.projects.wealthwhizap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class BadgeAdapter(private val context: Context, private val badgeList: List<BadgeItem>) : BaseAdapter() {

    override fun getCount(): Int = badgeList.size

    override fun getItem(position: Int): Any = badgeList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.badge_item, parent, false)

        val badgeIcon = view.findViewById<ImageView>(R.id.badgeIcon)
        val badgeLabel = view.findViewById<TextView>(R.id.badgeLabel)

        val badge = badgeList[position]
        badgeLabel.text = badge.name
        badgeIcon.setImageResource(if (badge.unlocked) badge.iconRes else R.drawable.ic_badge_placeholder)

        return view
    }
}
