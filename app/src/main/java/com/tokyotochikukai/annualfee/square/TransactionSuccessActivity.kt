package com.tokyotochikukai.annualfee.square

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TransactionSuccessActivity : AppCompatActivity() {

  companion object {
    private const val ORDER_NUMBER = "ORDER_NUMBER"

    fun start(context: Context, orderNumber: String) {
      val intent = Intent(context, TransactionSuccessActivity::class.java)
      intent.putExtra(ORDER_NUMBER, orderNumber)
      context.startActivity(intent)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.order_complete)

    val orderNumberMessage = findViewById<TextView>(R.id.order_complete_subtitle)
    val customizeNewBike: View = findViewById(R.id.customize_new_bike)

    val orderNumber = intent.getStringExtra(ORDER_NUMBER)
    orderNumberMessage.text = getString(R.string.order_info, orderNumber)

    customizeNewBike.setOnClickListener { finish() }
  }
}
