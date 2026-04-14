package com.example.owensbikes;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.squareup.sdk.pos.ChargeRequest;
import com.squareup.sdk.pos.CurrencyCode;
import com.squareup.sdk.pos.PosApi;
import com.squareup.sdk.pos.PosClient;
import com.squareup.sdk.pos.PosSdk;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.squareup.sdk.pos.ChargeRequest.TenderType.CARD;
import static com.squareup.sdk.pos.ChargeRequest.TenderType.CASH;

public class MainActivity extends AppCompatActivity {

  private static final String ORDER_INFO = "ORDER_INFO";
  private static final String ORDER_NUMBER = "ORDER_NUMBER";
  private static final int FIRST_ORDER_NUMBER = 1;
  private static final int CHARGE_REQUEST_CODE = 0xCAFE;

  private PosClient posClient;
  private SharedPreferences orderInfoPrefs;
  private DialogComposer dialogComposer;
  private TransactionResultHandler transactionResultHandler;

  private Spinner graduationTermSpinner;
  private EditText payerNameInput;
  private Button amountSocialButton;
  private Button amountStudentButton;
  private Button amountDevelopmentButton;
  private Button checkoutButton;
  private int selectedAmount = 3000;
  private String selectedAmountLabel;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    orderInfoPrefs = getSharedPreferences(ORDER_INFO, MODE_PRIVATE);
    posClient = PosSdk.createClient(this, BuildConfig.CLIENT_ID);
    dialogComposer = new DialogComposer(this, posClient);
    transactionResultHandler = new TransactionResultHandler(this, posClient, dialogComposer);

    graduationTermSpinner = findViewById(R.id.graduation_term_spinner);
    payerNameInput = findViewById(R.id.payer_name_input);
    amountSocialButton = findViewById(R.id.amount_social);
    amountStudentButton = findViewById(R.id.amount_student);
    amountDevelopmentButton = findViewById(R.id.amount_development);
    checkoutButton = findViewById(R.id.checkout_button);

    setupGraduationTerms();
    setupAmountButtons();
    wireFormValidation();

    checkoutButton.setOnClickListener(v -> showConfirmationDialog());
  }

  private void setupGraduationTerms() {
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    int latestGraduateYear = currentYear + 1;
    List<String> terms = new ArrayList<>();
    for (int year = latestGraduateYear; year >= 1952; year--) {
      terms.add(formatGraduationLabel(year));
    }
    terms.add("不明");
    terms.add("非卒業生");

    ArrayAdapter<String> adapter = new ArrayAdapter<>(
        this,
        android.R.layout.simple_spinner_item,
        terms);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    graduationTermSpinner.setAdapter(adapter);
  }

  private String formatGraduationLabel(int year) {
    int term = year - 1902;
    String eraLabel;
    if (year >= 2019) {
      eraLabel = "令和" + (year - 2018) + "年卒";
    } else if (year >= 1989) {
      eraLabel = "平成" + (year - 1988) + "年卒";
    } else {
      eraLabel = "昭和" + (year - 1925) + "年卒";
    }
    return term + "期 (" + year + "年 / " + eraLabel + ")";
  }

  private void wireFormValidation() {
    payerNameInput.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateCheckoutEnabled();
      }

      @Override public void afterTextChanged(Editable s) {
      }
    });

    updateCheckoutEnabled();
  }

  private void setupAmountButtons() {
    selectedAmountLabel = getString(R.string.label_social);
    amountSocialButton.setOnClickListener(v -> selectAmount(
        3000,
        getString(R.string.label_social),
        amountSocialButton));
    amountStudentButton.setOnClickListener(v -> selectAmount(
        1000,
        getString(R.string.label_student),
        amountStudentButton));
    amountDevelopmentButton.setOnClickListener(v -> selectAmount(
        1,
        getString(R.string.label_development),
        amountDevelopmentButton));
    updateAmountButtonStyles(amountSocialButton);
  }

  private void selectAmount(int amount, String label, Button selectedButton) {
    selectedAmount = amount;
    selectedAmountLabel = label;
    updateAmountButtonStyles(selectedButton);
    updateCheckoutEnabled();
  }

  private void updateAmountButtonStyles(Button selectedButton) {
    updateAmountButtonStyle(amountSocialButton, selectedButton == amountSocialButton);
    updateAmountButtonStyle(amountStudentButton, selectedButton == amountStudentButton);
    updateAmountButtonStyle(amountDevelopmentButton, selectedButton == amountDevelopmentButton);
  }

  private void updateAmountButtonStyle(Button button, boolean selected) {
    if (selected) {
      button.setBackgroundResource(R.drawable.amount_button_selected);
      button.setTextColor(getResources().getColor(R.color.white));
    } else {
      button.setBackgroundResource(R.drawable.amount_button_unselected);
      button.setTextColor(getResources().getColor(R.color.owen_yellow));
    }
  }

  private void updateCheckoutEnabled() {
    boolean hasName = payerNameInput.getText() != null
        && !payerNameInput.getText().toString().trim().isEmpty();
    checkoutButton.setEnabled(hasName);
  }

  private void showConfirmationDialog() {
    int amount = selectedAmount;
    String label = selectedAmountLabel;
    String term = String.valueOf(graduationTermSpinner.getSelectedItem());
    String name = payerNameInput.getText().toString().trim();

    String message = getString(
        R.string.confirm_message_template,
        term,
        name,
        getString(R.string.yen_amount_format, amount),
        label
    );

    new AlertDialog.Builder(this)
        .setTitle(R.string.confirm_title)
        .setMessage(message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.ok, (dialog, which) -> checkout())
        .show();
  }

  public void checkout() {
    int amount = selectedAmount;
    String term = String.valueOf(graduationTermSpinner.getSelectedItem());
    String name = payerNameInput.getText().toString().trim();
    String label = selectedAmountLabel;

    String note = term + ":" + name;
    Set<ChargeRequest.TenderType> tenderTypes = EnumSet.of(CARD, CASH);
    long orderNumber = orderInfoPrefs.getLong(ORDER_NUMBER, FIRST_ORDER_NUMBER) + 1;
    orderInfoPrefs.edit().putLong(ORDER_NUMBER, orderNumber).apply();
    String requestMetadata = String.valueOf(orderNumber);

    ChargeRequest.Builder chargeRequest = new ChargeRequest.Builder(amount, CurrencyCode.JPY)
        .note(note)
        .autoReturn(PosApi.AUTO_RETURN_TIMEOUT_MIN_MILLIS, TimeUnit.MILLISECONDS)
        .requestMetadata(requestMetadata)
        .restrictTendersTo(tenderTypes);
    try {
      Intent chargeIntent = posClient.createChargeIntent(chargeRequest.build());
      startActivityForResult(chargeIntent, CHARGE_REQUEST_CODE);
    } catch (ActivityNotFoundException e) {
      dialogComposer.showPointOfSaleUninstalledDialog();
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == CHARGE_REQUEST_CODE) {
      if (data == null) {
        transactionResultHandler.onNoResult();
        return;
      }
      if (resultCode == RESULT_OK) {
        payerNameInput.setText("");
        selectAmount(3000, getString(R.string.label_social), amountSocialButton);
        graduationTermSpinner.setSelection(0);
        transactionResultHandler.onSuccess(data);
      } else {
        transactionResultHandler.onError(data);
      }
    }
  }
}
