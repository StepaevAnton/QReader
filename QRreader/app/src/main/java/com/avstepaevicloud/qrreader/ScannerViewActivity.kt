package com.avstepaevicloud.qrreader

import android.app.Activity
import android.os.Bundle
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import android.util.Log
import android.content.Intent

import com.google.zxing.BarcodeFormat

class ScannerActivityView : Activity(), ZXingScannerView.ResultHandler {
    companion object {
        var flash: Boolean = false
    }

    private var scannerView: ZXingScannerView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scannerView = ZXingScannerView(this)
        scannerView!!.setFormats(listOf(BarcodeFormat.QR_CODE))

        scannerView!!.setOnClickListener { flash() }

        scannerView!!.setResultHandler(this)
        scannerView!!.startCamera()
        scannerView!!.setAutoFocus(true)

        setContentView(scannerView)

        try {
            scannerView!!.flash = flash

        } catch (e: Exception) {
            // ignored
        }
    }

    private fun flash() {
        flash = !flash
        try {
            scannerView!!.flash = flash
        } catch (e: Exception) {
            // ignored
        }

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        try {
            scannerView!!.stopCamera()
        } catch (e: Exception) {
            Log.e("Error", e.message)
        }

        val resultIntent = Intent()
        resultIntent.putExtra("BarCode", "")
        setResult(2, resultIntent)
        finish()
    }

    override fun handleResult(rawResult: Result) {
        Log.e("handler", rawResult.text)
        Log.e("handler", rawResult.barcodeFormat.toString())

        try {
            scannerView!!.stopCamera()
            val resultIntent = Intent()
            resultIntent.putExtra("BarCode", rawResult.text)
            setResult(2, resultIntent)
            finish()
        } catch (e: Exception) {
            Log.e("Error", e.message)
        }

    }

}
