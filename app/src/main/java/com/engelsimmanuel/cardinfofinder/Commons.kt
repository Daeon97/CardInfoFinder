package com.engelsimmanuel.cardinfofinder

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar

class Commons(
    private val activity: Activity,
    private val cardBrand: TextView,
    private val cardType: TextView,
    private val bank: TextView,
    private val country: TextView,
    private val cardBrandShimmer: ShimmerFrameLayout,
    private val cardTypeShimmer: ShimmerFrameLayout,
    private val bankShimmer: ShimmerFrameLayout,
    private val countryShimmer: ShimmerFrameLayout,
) {

    fun showLoadingCardDetails() {
        startShimmerOnLoadStarted()
        cardBrandShimmer.visibility = View.VISIBLE
        cardTypeShimmer.visibility = View.VISIBLE
        bankShimmer.visibility = View.VISIBLE
        countryShimmer.visibility = View.VISIBLE
    }

    fun hideLoadingCardDetails() {
        stopShimmer()
        cardBrandShimmer.visibility = View.GONE
        cardTypeShimmer.visibility = View.GONE
        bankShimmer.visibility = View.GONE
        countryShimmer.visibility = View.GONE
    }

    private fun startShimmerOnLoadStarted() {
        cardBrand.setBackgroundColor(activity.resources.getColor(R.color.default_font_color))
        cardType.setBackgroundColor(activity.resources.getColor(R.color.default_font_color))
        bank.setBackgroundColor(activity.resources.getColor(R.color.default_font_color))
        country.setBackgroundColor(activity.resources.getColor(R.color.default_font_color))

        cardBrandShimmer.showShimmer(true)
        cardTypeShimmer.showShimmer(true)
        bankShimmer.showShimmer(true)
        countryShimmer.showShimmer(true)
    }

    fun stopShimmerOnLoadSuccessful(
        cardBrandText: String,
        cardTypeText: String,
        bankText: String,
        countryText: String
    ) {
        cardBrand.text = cardBrandText
        cardType.text = cardTypeText
        bank.text = bankText
        country.text = countryText

        cardBrand.setBackgroundColor(activity.resources.getColor(android.R.color.transparent))
        cardType.setBackgroundColor(activity.resources.getColor(android.R.color.transparent))
        bank.setBackgroundColor(activity.resources.getColor(android.R.color.transparent))
        country.setBackgroundColor(activity.resources.getColor(android.R.color.transparent))

        cardBrandShimmer.hideShimmer()
        cardTypeShimmer.hideShimmer()
        bankShimmer.hideShimmer()
        countryShimmer.hideShimmer()
    }

    fun stopShimmer() {
        cardBrandShimmer.hideShimmer()
        cardTypeShimmer.hideShimmer()
        bankShimmer.hideShimmer()
        countryShimmer.hideShimmer()
    }

    fun showSnackBarWithoutAction(view: View, message: String) {
        Snackbar.make(
            view,
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    fun log(tag: String, messageToLog: String) {
        Log.wtf(tag, messageToLog)
    }
}