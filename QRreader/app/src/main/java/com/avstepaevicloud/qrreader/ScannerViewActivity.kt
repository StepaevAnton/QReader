package com.avstepaevicloud.qrreader

import android.app.ActionBar
import android.app.Activity
import android.os.Bundle
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import android.util.Log
import android.content.Intent
import android.graphics.Color
import android.text.Layout
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import com.google.zxing.BarcodeFormat
import java.text.AttributedCharacterIterator
import android.widget.LinearLayout


class ScannerActivityView : Activity(), ZXingScannerView.ResultHandler {
companion object {
    var flash : Boolean = false
}

    private var scannerView: ZXingScannerView? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main2)



        //var layout = FrameLayout(this)

        //val button = Button(this)




        scannerView = ZXingScannerView(this)
        scannerView!!.setFormats(listOf(BarcodeFormat.QR_CODE))

//        val button_dynamic = Button(this)
//        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        params.gravity = Gravity.CENTER
//        button_dynamic.text = "Dynamic Button"
//        //button_dynamic.gravity = Gravity.CENTER_HORIZONTAL
//        button_dynamic.setBackgroundColor(Color.WHITE)
//
//        button_dynamic.layoutParams = params
//
//
//        val frameLayout = LinearLayout(this)
//        //frameLayout.layoutParams = ViewGroup.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT)
//frameLayout.addView(scannerView)
//        frameLayout.addView(button_dynamic)


        scannerView!!.setOnClickListener{flash()}

        //setContentView(scannerView)
        scannerView!!.setResultHandler(this)
        scannerView!!.startCamera()
        scannerView!!.setAutoFocus(true)

//        scannerView!!.addView(button_dynamic, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        //button_dynamic.bringToFront()

        setContentView(scannerView)

try {
    scannerView!!.flash = flash

}
catch (e:Exception){
    // ignored
}
//        scannerView!!.toggleFlash()

    }

    private fun flash() {
        flash = !flash
        try {
            scannerView!!.flash = flash
        }
        catch (e: Exception){
            // ignored
        }

    }

    override fun onResume(){
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        try {
            scannerView!!.stopCamera()
        }
        catch (e: Exception){
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

        try{
            scannerView!!.stopCamera()
            val resultIntent = Intent()
            resultIntent.putExtra("BarCode", rawResult.text)
            setResult(2, resultIntent)
            finish()
        }
        catch(e: Exception){
            Log.e("Error", e.message)
        }

    }

}


//package com.avstepaevicloud.qrreader
//
//import android.app.ActionBar
//import android.app.Activity
//import android.os.Bundle
////import com.google.zxing.Result
//import me.dm7.barcodescanner.zbar.ZBarScannerView
//import android.util.Log
//import android.content.Intent
//import android.graphics.Color
//import android.text.Layout
//import android.util.AttributeSet
//import android.view.Gravity
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.FrameLayout
//import me.dm7.barcodescanner.zbar.Result
////import com.google.zxing.BarcodeFormat
//import java.text.AttributedCharacterIterator
//import android.widget.LinearLayout
////import me.dm7.barcodescanner.zbar.ZBarScannerView
//
//
//class ScannerActivityView : Activity(), ZBarScannerView.ResultHandler {
//
//    private var scannerView: ZBarScannerView? = null
//
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        //setContentView(R.layout.activity_main2)
//
//
//
//        //var layout = FrameLayout(this)
//
//        //val button = Button(this)
//
//
//
//
//        scannerView = ZBarScannerView(this)
//        //scannerView!!.setFormats(listOf(BarcodeFormat.QR_CODE))
//
////        val button_dynamic = Button(this)
////        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
////        params.gravity = Gravity.CENTER
////        button_dynamic.text = "Dynamic Button"
////        //button_dynamic.gravity = Gravity.CENTER_HORIZONTAL
////        button_dynamic.setBackgroundColor(Color.WHITE)
////
////        button_dynamic.layoutParams = params
//
//
////        val frameLayout = LinearLayout(this)
////        //frameLayout.layoutParams = ViewGroup.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT)
////        frameLayout.addView(scannerView)
////        frameLayout.addView(button_dynamic)
////
////        setContentView(frameLayout)
//
//        setContentView(scannerView)
//        scannerView!!.setResultHandler(this)
//        scannerView!!.startCamera()
//
////        scannerView!!.addView(button_dynamic, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
//
//        //button_dynamic.bringToFront()
//
//
//
//        scannerView!!.flash = true
////        scannerView!!.toggleFlash()
//
//    }
//
//    override fun onResume(){
//        super.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//
//        try {
//            scannerView!!.stopCamera()
//        }
//        catch (e: Exception){
//            Log.e("Error", e.message)
//        }
//
//        val resultIntent = Intent()
//        resultIntent.putExtra("BarCode", "")
//        setResult(2, resultIntent)
//        finish()
//    }
//
//    override fun handleResult(rawResult: Result) {
//        Log.e("handler", rawResult.contents)
//        Log.e("handler", rawResult.barcodeFormat.toString())
//
//        try{
//            scannerView!!.stopCamera()
//            val resultIntent = Intent()
//            resultIntent.putExtra("BarCode", rawResult.contents)
//            setResult(2, resultIntent)
//            finish()
//        }
//        catch(e: Exception){
//            Log.e("Error", e.message)
//        }
//
//    }
//
//}
