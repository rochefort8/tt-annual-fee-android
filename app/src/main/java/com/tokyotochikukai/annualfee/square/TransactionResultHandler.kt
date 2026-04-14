package com.tokyotochikukai.annualfee.square

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.squareup.sdk.pos.ChargeRequest
import com.squareup.sdk.pos.PosClient

class TransactionResultHandler(
  private val activity: AppCompatActivity,
  private val posClient: PosClient,
  private val dialogComposer: DialogComposer
) {

  fun onNoResult() {
    dialogComposer.showErrorDialog(R.string.error_no_result, R.string.error_no_result_message)
  }

  fun onSuccess(data: Intent) {
    val success = posClient.parseChargeSuccess(data)
    TransactionSuccessActivity.start(activity, success.requestMetadata ?: "")
  }

  fun onError(data: Intent) {
    val error = posClient.parseChargeError(data)
    showErrorDialog(error)
  }

  fun showErrorDialog(errorResult: ChargeRequest.Error) {
    Log.e(errorResult.code.toString(), errorResult.debugDescription)
    when (errorResult.code) {
      ChargeRequest.ErrorCode.DISABLED -> dialogComposer.showErrorDialog(
        R.string.error_api_disabled,
        R.string.error_api_disabled_message
      )

      ChargeRequest.ErrorCode.ILLEGAL_LOCATION_ID -> throw IllegalStateException(
        "This sample app never passes a location id to the Point of Sale API."
      )

      ChargeRequest.ErrorCode.INVALID_REQUEST -> dialogComposer.showErrorDialog(
        R.string.error_unspecified,
        R.string.error_invalid_request_message
      )

      ChargeRequest.ErrorCode.NO_NETWORK -> dialogComposer.showErrorDialogWithRetry(
        R.string.error_network,
        R.string.error_network_message
      )

      ChargeRequest.ErrorCode.TRANSACTION_ALREADY_IN_PROGRESS ->
        dialogComposer.showTransactionInProgressDialog()

      ChargeRequest.ErrorCode.TRANSACTION_CANCELED -> dialogComposer.showErrorDialogWithRetry(
        R.string.error_transaction_cancelled,
        R.string.error_transaction_cancelled_message
      )

      ChargeRequest.ErrorCode.UNSUPPORTED_API_VERSION ->
        dialogComposer.showUpdatePointOfSaleDialog()

      ChargeRequest.ErrorCode.USER_NOT_ACTIVATED -> dialogComposer.showErrorDialogWithRetry(
        R.string.error_not_activated,
        R.string.error_not_activated_message
      )

      ChargeRequest.ErrorCode.USER_NOT_LOGGED_IN -> dialogComposer.showErrorDialogWithRetry(
        R.string.error_not_logged_in,
        R.string.error_not_logged_in_message
      )

      ChargeRequest.ErrorCode.NO_RESULT,
      ChargeRequest.ErrorCode.UNEXPECTED -> dialogComposer.showErrorDialogWithRetry(
        R.string.error_unspecified,
        R.string.try_again
      )

      else -> dialogComposer.showErrorDialogWithRetry(
        R.string.error_unspecified,
        R.string.try_again
      )
    }
  }
}
