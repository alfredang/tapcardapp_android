package com.tertiaryinfotech.tapcard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.tertiaryinfotech.tapcard.MainActivity
import com.tertiaryinfotech.tapcard.R
import com.tertiaryinfotech.tapcard.util.VCard

/**
 * Home-screen widget showing the primary card's QR (scannable straight off the
 * home screen) and name. Tapping it opens the app. The card comes from
 * [WidgetStore]; an empty state shows until the user has a card.
 */
class CardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val card = WidgetStore.loadPrimary(context)
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_card)

            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            if (card != null && !card.isBlank) {
                views.setViewVisibility(R.id.widget_qr, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_qr, VCard.qrBitmap(card, 360))
                views.setTextViewText(R.id.widget_name, card.displayName)
                views.setViewVisibility(R.id.widget_empty, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_qr, View.GONE)
                views.setTextViewText(R.id.widget_name, "Tapcard")
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            }

            manager.updateAppWidget(id, views)
        }
    }
}
