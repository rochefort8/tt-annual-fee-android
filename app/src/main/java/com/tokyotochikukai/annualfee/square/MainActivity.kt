package com.tokyotochikukai.annualfee.square

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.squareup.sdk.pos.ChargeRequest
import com.squareup.sdk.pos.CurrencyCode
import com.squareup.sdk.pos.PosApi
import com.squareup.sdk.pos.PosClient
import com.squareup.sdk.pos.PosSdk
import java.util.Calendar
import java.util.EnumSet
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

  companion object {
    private const val ORDER_INFO = "ORDER_INFO"
    private const val ORDER_NUMBER = "ORDER_NUMBER"
    private const val FIRST_ORDER_NUMBER = 1L
    private const val CHARGE_REQUEST_CODE = 0xCAFE
  }

  private lateinit var posClient: PosClient
  private lateinit var orderInfoPrefs: SharedPreferences
  private lateinit var dialogComposer: DialogComposer
  private lateinit var transactionResultHandler: TransactionResultHandler

  private lateinit var graduationTermSpinner: Spinner
  private lateinit var payerNameInput: EditText
  private lateinit var amountSocialButton: Button
  private lateinit var amountStudentButton: Button
  private lateinit var amountDevelopmentButton: Button
  private lateinit var checkoutButton: Button

  private var selectedAmount: Int = 3000
  private lateinit var selectedAmountLabel: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    orderInfoPrefs = getSharedPreferences(ORDER_INFO, MODE_PRIVATE)
    posClient = PosSdk.createClient(this, BuildConfig.CLIENT_ID)
    dialogComposer = DialogComposer(this, posClient)
    transactionResultHandler = TransactionResultHandler(this, posClient, dialogComposer)

    graduationTermSpinner = findViewById(R.id.graduation_term_spinner)
    payerNameInput = findViewById(R.id.payer_name_input)
    amountSocialButton = findViewById(R.id.amount_social)
    amountStudentButton = findViewById(R.id.amount_student)
    amountDevelopmentButton = findViewById(R.id.amount_development)
    checkoutButton = findViewById(R.id.checkout_button)

    setupGraduationTerms()
    setupAmountButtons()
    wireFormValidation()

    checkoutButton.setOnClickListener { showConfirmationDialog() }
  }

  private fun setupGraduationTerms() {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val latestGraduateYear = currentYear + 1
    val terms = mutableListOf<String>()
    for (year in latestGraduateYear downTo 1952) {
      terms.add(formatGraduationLabel(year))
    }
    terms.add("不明")
    terms.add("非卒業生")

    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terms)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    graduationTermSpinner.adapter = adapter
  }

  private fun formatGraduationLabel(year: Int): String {
    val term = year - 1902
    val eraLabel = when {
      year >= 2019 -> "令和${year - 2018}年卒"
      year >= 1989 -> "平成${year - 1988}年卒"
      else -> "昭和${year - 1925}年卒"
    }
    return "${term}期 (${year}年 / $eraLabel)"
  }

  private fun wireFormValidation() {
    payerNameInput.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        updateCheckoutEnabled()
      }
      override fun afterTextChanged(s: Editable?) = Unit
    })
    updateCheckoutEnabled()
  }

  private fun setupAmountButtons() {
    selectedAmountLabel = getString(R.string.label_social)

    amountSocialButton.setOnClickListener {
      selectAmount(3000, getString(R.string.label_social), amountSocialButton)
    }
    amountStudentButton.setOnClickListener {
      selectAmount(1000, getString(R.string.label_student), amountStudentButton)
    }
    amountDevelopmentButton.setOnClickListener {
      selectAmount(1, getString(R.string.label_development), amountDevelopmentButton)
    }

    updateAmountButtonStyles(amountSocialButton)
  }

  private fun selectAmount(amount: Int, label: String, selectedButton: Button) {
    selectedAmount = amount
    selectedAmountLabel = label
    updateAmountButtonStyles(selectedButton)
    updateCheckoutEnabled()
  }

  private fun updateAmountButtonStyles(selectedButton: Button) {
    updateAmountButtonStyle(amountSocialButton, selectedButton == amountSocialButton)
    updateAmountButtonStyle(amountStudentButton, selectedButton == amountStudentButton)
    updateAmountButtonStyle(amountDevelopmentButton, selectedButton == amountDevelopmentButton)
  }

  private fun updateAmountButtonStyle(button: Button, selected: Boolean) {
    if (selected) {
      button.setBackgroundResource(R.drawable.amount_button_selected)
      button.setTextColor(ContextCompat.getColor(this, R.color.white))
    } else {
      button.setBackgroundResource(R.drawable.amount_button_unselected)
      button.setTextColor(ContextCompat.getColor(this, R.color.owen_yellow))
    }
  }

  private fun updateCheckoutEnabled() {
    val hasName = !payerNameInput.text?.toString()?.trim().isNullOrEmpty()
    checkoutButton.isEnabled = hasName
  }

  private fun showConfirmationDialog() {
    val term = graduationTermSpinner.selectedItem.toString()
    val name = payerNameInput.text.toString().trim()

    val message = getString(
      R.string.confirm_message_template,
      term,
      name,
      getString(R.string.yen_amount_format, selectedAmount),
      selectedAmountLabel
    )

    AlertDialog.Builder(this)
      .setTitle(R.string.confirm_title)
      .setMessage(message)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.ok) { _, _ -> checkout() }
      .show()
  }

  fun checkout() {
    val term = graduationTermSpinner.selectedItem.toString()
    val name = payerNameInput.text.toString().trim()
    val note = "$term:$name"

    val tenderTypes = EnumSet.of(
      ChargeRequest.TenderType.CARD,
      ChargeRequest.TenderType.CASH
    )

    val orderNumber = orderInfoPrefs.getLong(ORDER_NUMBER, FIRST_ORDER_NUMBER) + 1
    orderInfoPrefs.edit().putLong(ORDER_NUMBER, orderNumber).apply()

    val chargeRequest = ChargeRequest.Builder(selectedAmount, CurrencyCode.JPY)
      .note(note)
      .autoReturn(PosApi.AUTO_RETURN_TIMEOUT_MIN_MILLIS, TimeUnit.MILLISECONDS)
      .requestMetadata(orderNumber.toString())
      .restrictTendersTo(tenderTypes)
      .build()

    try {
      val chargeIntent = posClient.createChargeIntent(chargeRequest)
      startActivityForResult(chargeIntent, CHARGE_REQUEST_CODE)
    } catch (e: ActivityNotFoundException) {
      dialogComposer.showPointOfSaleUninstalledDialog()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != CHARGE_REQUEST_CODE) return

    if (data == null) {
      transactionResultHandler.onNoResult()
      return
    }

    if (resultCode == RESULT_OK) {
      payerNameInput.setText("")
      selectAmount(3000, getString(R.string.label_social), amountSocialButton)
      graduationTermSpinner.setSelection(0)
      transactionResultHandler.onSuccess(data)
    } else {
      transactionResultHandler.onError(data)
    }
  }
}
