package com.example.travelcompanion.ui.stats

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter per il ViewPager2 delle statistiche
 * Gestisce la navigazione tra fragment storico e futuro
 */
class StatsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    /**
     * Numero totale di tab/fragment
     */
    override fun getItemCount(): Int = 2

    /**
     * Crea il fragment appropriato per ogni posizione
     */
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StatsMyTrips()
            1 -> StatsFutureFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}