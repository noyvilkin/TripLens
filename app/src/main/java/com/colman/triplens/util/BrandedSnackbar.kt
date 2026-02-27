package com.colman.triplens.util

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.colman.triplens.R
import com.google.android.material.snackbar.Snackbar

/**
 * Utility for displaying app-branded Snackbar notifications
 * styled with rounded corners, a subtle lavender background,
 * and an optional success-checkmark icon — matching the TripLens theme.
 */
object BrandedSnackbar {

    /**
     * Show a branded **success** snackbar with a ✓ icon.
     */
    fun showSuccess(view: View, message: String) {
        show(view, message, iconRes = R.drawable.ic_check_circle)
    }

    /**
     * Show a branded **error / info** snackbar (no icon).
     */
    fun showError(view: View, message: String) {
        show(view, message, iconRes = null, duration = Snackbar.LENGTH_LONG)
    }

    // ── Internal ────────────────────────────────────────────────────

    private fun show(
        view: View,
        message: String,
        iconRes: Int? = null,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        val sbView = snackbar.view

        // Rounded-corner background — clear Material 3's default tint
        // so our custom color actually shows through.
        sbView.background =
            ContextCompat.getDrawable(view.context, R.drawable.bg_snackbar_branded)
        sbView.backgroundTintList = null

        // Floating-card margins
        (sbView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            val margin = (16 * view.resources.displayMetrics.density).toInt()
            params.setMargins(margin, 0, margin, margin)
            sbView.layoutParams = params
        }

        // Subtle elevation
        sbView.elevation = 6 * view.resources.displayMetrics.density

        // Text colour + optional leading icon
        sbView.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.snackbar_text_color))
            if (iconRes != null) {
                val icon = ContextCompat.getDrawable(context, iconRes)
                setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
                compoundDrawablePadding =
                    (12 * resources.displayMetrics.density).toInt()
            }
        }

        snackbar.show()
    }
}
