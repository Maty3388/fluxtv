package com.fluxtv.app.utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.core.content.FileProvider
import com.fluxtv.app.models.AppVersion
import java.io.File

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

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun showDialog(ctx: Context, current: String, ver: AppVersion) {
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(!ver.forceUpdate)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx,28), dp(ctx,28), dp(ctx,28), dp(ctx,24))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF0D1B2A.toInt(), 0xFF0A1520.toInt())).apply {
                cornerRadius = dp(ctx,20).toFloat()
                setStroke(1, 0x4400E5FF.toInt())
            }
            elevation = dp(ctx,8).toFloat()
        }

        // Badge FLUX TV
        val badge = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(ctx,50).toFloat()
                setColor(0x1500E5FF.toInt())
                setStroke(1, 0x3300E5FF.toInt())
            }
            setPadding(dp(ctx,16), dp(ctx,6), dp(ctx,16), dp(ctx,6))
        }
        badge.addView(TextView(ctx).apply {
            text = "⚡ FLUX TV"
            textSize = 13f
            setTextColor(0xFF00E5FF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
        })
        val badgeWrap = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,12) }
        }
        badgeWrap.addView(badge)
        root.addView(badgeWrap)

        // Título
        root.addView(TextView(ctx).apply {
            text = "Nueva Actualización"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,4) }
        })
        root.addView(TextView(ctx).apply {
            text = "Hay una versión más reciente disponible"
            textSize = 12f
            setTextColor(0xFF8899AA.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,20) }
        })

        // Version row
        val versionRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,20) }
        }
        versionRow.addView(TextView(ctx).apply {
            text = "v$current"
            textSize = 13f
            setTextColor(0xFF8899AA.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply { cornerRadius = dp(ctx,8).toFloat(); setColor(0x15FFFFFF.toInt()) }
            setPadding(dp(ctx,14), dp(ctx,6), dp(ctx,14), dp(ctx,6))
        })
        versionRow.addView(TextView(ctx).apply {
            text = " → "
            textSize = 18f
            setTextColor(0xFF00E5FF.toInt())
            setPadding(dp(ctx,8), 0, dp(ctx,8), 0)
        })
        versionRow.addView(TextView(ctx).apply {
            text = "v${ver.version}"
            textSize = 13f
            setTextColor(0xFF00E5FF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = dp(ctx,8).toFloat()
                setColor(0x2000E5FF.toInt())
                setStroke(1, 0x4400E5FF.toInt())
            }
            setPadding(dp(ctx,14), dp(ctx,6), dp(ctx,14), dp(ctx,6))
        })
        root.addView(versionRow)

        // Changelog
        if (ver.changelog.isNotEmpty()) {
            val changelogBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx,12).toFloat()
                    setColor(0x08FFFFFF.toInt())
                }
                setPadding(dp(ctx,16), dp(ctx,12), dp(ctx,16), dp(ctx,12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,24) }
            }
            // Borde izquierdo cyan
            val borderWrap = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx,12).toFloat()
                    setColor(0x08FFFFFF.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,24) }
            }
            val leftBorder = View(ctx).apply {
                background = GradientDrawable().apply { setColor(0x4400E5FF.toInt()); cornerRadius = dp(ctx,4).toFloat() }
                layoutParams = LinearLayout.LayoutParams(dp(ctx,3), LinearLayout.LayoutParams.MATCH_PARENT)
            }
            val changelogInner = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx,12), dp(ctx,12), dp(ctx,12), dp(ctx,12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            changelogInner.addView(TextView(ctx).apply {
                text = "📋 NOVEDADES"
                textSize = 11f
                setTextColor(0xFF00E5FF.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                letterSpacing = 0.1f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,8) }
            })
            changelogInner.addView(TextView(ctx).apply {
                text = ver.changelog
                textSize = 12f
                setTextColor(0xFFCCDDEE.toInt())
                setLineSpacing(dp(ctx,3).toFloat(), 1f)
            })
            borderWrap.addView(leftBorder)
            borderWrap.addView(changelogInner)
            root.addView(borderWrap)
        }

        // Botones
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx,12) }
        }

        val btnLater = TextView(ctx).apply {
            text = "DESPUÉS"
            textSize = 13f
            setTextColor(0xFF8899AA.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { cornerRadius = dp(ctx,12).toFloat(); setColor(Color.TRANSPARENT); setStroke(1, 0x22FFFFFF.toInt()) }
            layoutParams = LinearLayout.LayoutParams(0, dp(ctx,44), 1f).apply { marginEnd = dp(ctx,10) }
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = false
            setOnFocusChangeListener { _, focused ->
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx,12).toFloat()
                    setColor(if (focused) 0x22FFFFFF.toInt() else Color.TRANSPARENT)
                    setStroke(if (focused) 2 else 1, if (focused) 0xFFFFFFFF.toInt() else 0x22FFFFFF.toInt())
                }
                setTextColor(if (focused) Color.WHITE else 0xFF8899AA.toInt())
            }
            setOnClickListener { dialog.dismiss() }
        }

        val btnUpdate = TextView(ctx).apply {
            text = "⬇ ACTUALIZAR"
            textSize = 13f
            setTextColor(Color.BLACK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFF00E5FF.toInt(), 0xFF0088CC.toInt())).apply {
                cornerRadius = dp(ctx,12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(ctx,44), 2f)
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = false
            setOnFocusChangeListener { _, focused ->
                background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(0xFF00E5FF.toInt(), 0xFF0088CC.toInt())).apply {
                    cornerRadius = dp(ctx,12).toFloat()
                    if (focused) setStroke(2, Color.WHITE)
                }
                animate().scaleX(if (focused) 1.05f else 1f).scaleY(if (focused) 1.05f else 1f).setDuration(100).start()
            }
            setOnClickListener {
                dialog.dismiss()
                downloadAndInstall(ctx, ver.apkUrl)
            }
        }

        if (!ver.forceUpdate) btnRow.addView(btnLater)
        btnRow.addView(btnUpdate)
        root.addView(btnRow)

        // Footer
        root.addView(TextView(ctx).apply {
            text = "La descarga comenzará automáticamente"
            textSize = 10f
            setTextColor(0xFF445566.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        })

        dialog.setContentView(root)
        dialog.window?.setLayout((ctx.resources.displayMetrics.widthPixels * 0.9).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun downloadAndInstall(ctx: Context, url: String) {
        if (url.isEmpty()) return
        try {
            val fileName = "fluxtv_update.apk"
            val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("FluxTV Update")
                setDescription("Descargando actualización...")
                setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            val downloadId = dm.enqueue(request)
            // Esperar descarga e instalar
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post(object : Runnable {
                override fun run() {
                    val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                        if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                            cursor.close()
                            val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                            val apkUri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", file)
                            val install = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(apkUri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(install)
                            return
                        } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                            cursor.close()
                            // Fallback al browser
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(intent)
                            return
                        }
                    }
                    cursor.close()
                    handler.postDelayed(this, 1000)
                }
            })
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
