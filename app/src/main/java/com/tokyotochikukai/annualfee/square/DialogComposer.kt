package com.tokyotochikukai.annualfee.square

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.squareup.sdk.pos.PosClient

class DialogComposer(
  private val activity: MainActivity,
  private val posClient: PosClient
) {

  companion object {
    private const val POINT_OF_SALE_PACKAGE = "com.squareup"
  }

  fun showErrorDialog(titleResId: Int, messageResId: Int) {
    AlertDialog.Builder(activity)
      .setTitle(titleResId)
      .setMessage(messageResId)
      .setPositiveButton(activity.getString(R.string.ok), null)
      .show()
  }

  fun showErrorDialogWithRetry(titleResId: Int, messageResId: Int) {
    AlertDialog.Builder(activity)
      .setTitle(titleResId)
      .setMessage(messageResId)
      .setNegativeButton(activity.getString(R.string.ok), null)
      .setPositiveButton(R.string.retry) { _, _ -> activity.checkout() }
      .show()
  }

  fun showPointOfSaleUninstalledDialog() {
    AlertDialog.Builder(activity)
      .setTitle(R.string.error_install_point_of_sale)
      .setMessage(activity.getString(R.string.error_install_point_of_sale_message))
      .setPositiveButton(activity.getString(R.string.install_point_of_sale_confirm)) { _, _ ->
        posClient.openPointOfSalePlayStoreListing()
      }
      .setNegativeButton(R.string.cancel, null)
      .show()
  }

  fun showTransactionInProgressDialog() {
    AlertDialog.Builder(activity)
      .setTitle(R.string.error_transaction_in_progress)
      .setMessage(R.string.error_transaction_in_progress_message)
      .setPositiveButton(R.string.open_point_of_sale) { _, _ ->
        val packageManager = activity.packageManager
        val intent: Intent? = packageManager.getLaunchIntentForPackage(POINT_OF_SALE_PACKAGE)
        if (intent != null) {
          activity.startActivity(intent)
        }
      }
      .setNegativeButton(R.string.cancel, null)
      .show()
  }

  fun showUpdatePointOfSaleDialog() {
    AlertDialog.Builder(activity)
      .setTitle(R.string.update_point_of_sale_title)
      .setMessage(R.string.update_point_of_sale_message)
      .setPositiveButton(activity.getString(R.string.install_point_of_sale_confirm)) { _, _ ->
        posClient.openPointOfSalePlayStoreListing()
      }
      .setNegativeButton(R.string.cancel, null)
      .show()
  }
}
