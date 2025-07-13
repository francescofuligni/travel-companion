import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.travelcompanion.ui.stats.StatsFutureFragment
import com.example.travelcompanion.ui.stats.StatsMyTrips

class StatsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StatsMyTrips()
            1 -> StatsFutureFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}