package com.fluxtv.app.utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.Window
import android.widget.*
import androidx.core.content.FileProvider
import com.fluxtv.app.models.AppVersion
import java.io.File
import java.net.URL

object AutoUpdater {
    fun check(ctx: Context, current: String, ver: AppVersion) {
        if (!isNewer(ver.version, current)) return
        showDialog(ctx, current, ver)
    }

    private fun isNewer(new: String, cur: String): Boolean {
        val n = new.split(".").mapNotNull { it.toIntOrNull() }
        val c = cur.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0..2) {
            val ni = n.getOrElse(i){0}; val ci = c.getOrElse(i){0}
            if (ni > ci) return true; if (ni < ci) return false
        }
        return false
    }

    private fun showDialog(ctx: Context, current: String, ver: AppVersion) {
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(!ver.forceUpdate)
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1825"))
            setPadding(60,60,60,60)
        }
        layout.addView(TextView(ctx).apply {
            text="NUEVA VERSIÓN"; textSize=18f; setTextColor(Color.WHITE)
            typeface=android.graphics.Typeface.DEFAULT_BOLD
            gravity=android.view.Gravity.CENTER; setPadding(0,0,0,20)
        })
        layout.addView(LinearLayout(ctx).apply {
            gravity=android.view.Gravity.CENTER
            addView(LinearLayout(ctx).apply {
                orientation=LinearLayout.VERTICAL; gravity=android.view.Gravity.CENTER
                addView(TextView(ctx).apply{text="Actual";textSize=11f;setTextColor(Color.GRAY);gravity=android.view.Gravity.CENTER})
                addView(TextView(ctx).apply{text=current;textSize=20f;setTextColor(Color.WHITE);typeface=android.graphics.Typeface.DEFAULT_BOLD;gravity=android.view.Gravity.CENTER})
            })
            addView(TextView(ctx).apply{text="→";textSize=20f;setTextColor(Color.parseColor("#A855F7"));setPadding(40,0,40,0)})
            addView(LinearLayout(ctx).apply {
                orientation=LinearLayout.VERTICAL; gravity=android.view.Gravity.CENTER
                addView(TextView(ctx).apply{text="Nueva";textSize=11f;setTextColor(Color.GRAY);gravity=android.view.Gravity.CENTER})
                addView(TextView(ctx).apply{text=ver.version;textSize=20f;setTextColor(Color.parseColor("#A855F7"));typeface=android.graphics.Typeface.DEFAULT_BOLD;gravity=android.view.Gravity.CENTER})
            })
        })
        if (ver.changelog.isNotEmpty()) layout.addView(TextView(ctx).apply{text=ver.changelog;textSize=12f;setTextColor(Color.LTGRAY);gravity=android.view.Gravity.CENTER;setPadding(0,20,0,0)})
        val progress = ProgressBar(ctx,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;visibility=android.view.View.GONE;setPadding(0,20,0,0)}
        val tvProg = TextView(ctx).apply{text="";textSize=12f;setTextColor(Color.GRAY);gravity=android.view.Gravity.CENTER;visibility=android.view.View.GONE}
        layout.addView(progress); layout.addView(tvProg)
        val btnRow = LinearLayout(ctx).apply{setPadding(0,32,0,0)}
        val btnLater = Button(ctx).apply{text="Más tarde";setTextColor(Color.WHITE);setBackgroundColor(Color.parseColor("#333333"));layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f).apply{marginEnd=16};isFocusable=true
            setOnClickListener{dialog.dismiss()}
            setOnFocusChangeListener{v,focused-> v.setBackgroundColor(if(focused) Color.parseColor("#555555") else Color.parseColor("#333333")) }}
        val btnUpdate = Button(ctx).apply{text="ACTUALIZAR";setTextColor(Color.BLACK);setBackgroundColor(Color.parseColor("#A855F7"));layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);isFocusable=true
            setOnFocusChangeListener{v,focused-> v.setBackgroundColor(if(focused) Color.parseColor("#80FFFF") else Color.parseColor("#A855F7")) }
            setOnClickListener{isEnabled=false;btnLater.isEnabled=false;progress.visibility=android.view.View.VISIBLE;tvProg.visibility=android.view.View.VISIBLE
                download(ctx,ver.apkUrl,ver.version,progress,tvProg){dialog.dismiss()}}}
        if(!ver.forceUpdate) btnRow.addView(btnLater); btnRow.addView(btnUpdate)
        layout.addView(btnRow); dialog.setContentView(layout); dialog.show()
        btnUpdate.requestFocus()
    }

    private fun download(ctx: Context, url: String, ver: String, pb: ProgressBar, tv: TextView, done: ()->Unit) {
        Thread {
            try {
                val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"flux-$ver.apk")
                if(file.exists()) file.delete()
                val conn = URL(url).openConnection(); conn.connect()
                val total = conn.contentLength; val inp = conn.getInputStream(); val out = file.outputStream()
                val buf = ByteArray(8192); var dl=0; var r: Int
                while(inp.read(buf).also{r=it}!=-1){out.write(buf,0,r);dl+=r;val p=if(total>0)dl*100/total else 0
                    (ctx as? android.app.Activity)?.runOnUiThread{pb.progress=p;tv.text="$p%"}}
                out.close();inp.close()
                (ctx as? android.app.Activity)?.runOnUiThread{done()}
                val uri = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
                    FileProvider.getUriForFile(ctx,"${ctx.packageName}.fileprovider",file)
                else Uri.fromFile(file)
                ctx.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"application/vnd.android.package-archive");flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION})
            } catch(e: Exception){(ctx as? android.app.Activity)?.runOnUiThread{tv.text="Error: ${e.message}"}}
        }.start()
    }
}
