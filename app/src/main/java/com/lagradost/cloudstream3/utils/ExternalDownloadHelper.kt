package com.lagradost.cloudstream3.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.UIHelper.toPx

object ExternalDownloadHelper {

    const val DOWNLOAD_PROVIDER_INTERNAL = 0
    const val DOWNLOAD_PROVIDER_ADM = 1
    const val DOWNLOAD_PROVIDER_DIRECT_LINK = 2
    const val DOWNLOAD_PROVIDER_ALWAYS_ASK = 3
    const val DOWNLOAD_PROVIDER_AB_DOWNLOAD_MANAGER = 4

    const val AB_DOWNLOAD_MANAGER_PACKAGE = "com.abdownloadmanager"

    private val ADM_PACKAGES = listOf(
        "com.dv.adm",
        "com.dv.adm.pay",
        "com.dv.get",
        "com.dv.get.pro"
    )

    fun getDownloadProvider(context: Context): Int {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(context.getString(R.string.download_provider_key), DOWNLOAD_PROVIDER_INTERNAL)
    }

    fun isAdmInstalled(context: Context): Boolean = getInstalledAdmPackage(context) != null

    fun isABDownloadManagerInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(AB_DOWNLOAD_MANAGER_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledAdmPackage(context: Context): String? {
        val pm = context.packageManager
        for (pkg in ADM_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (e: PackageManager.NameNotFoundException) {}
        }
        return null
    }

    private fun getAdmEditorComponent(context: Context, packageName: String): ComponentName? {
        val pm = context.packageManager
        val candidates = listOf(
            "$packageName.AEditor",
            "com.dv.get.AEditor",
            "com.dv.adm.AEditor"
        )
        for (className in candidates) {
            try {
                val component = ComponentName(packageName, className)
                pm.getActivityInfo(component, 0)
                return component
            } catch (e: Exception) {}
        }
        return null
    }

    /**
     * Launches ADM (Advanced Download Manager) safely.
     * Uses ACTION_MAIN targeting AEditor with Intent.EXTRA_TEXT and com.android.extra.filename.
     * Crucially avoids setting mimeType to video wildcard or passing String array for HTTP headers
     * which caused ADM's intent receivers to crash.
     */
    fun launchADMDownload(
        context: Context,
        url: String,
        fileName: String,
        headers: Map<String, String>? = null
    ) {
        val displayName = if (fileName.contains('.')) fileName else "$fileName.mp4"
        val admPackage = getInstalledAdmPackage(context)

        fun buildIntent(targetPackage: String?, component: ComponentName?): Intent {
            return Intent().apply {
                if (component != null) {
                    action = Intent.ACTION_MAIN
                    this.component = component
                } else if (targetPackage != null) {
                    action = Intent.ACTION_VIEW
                    setPackage(targetPackage)
                } else {
                    action = Intent.ACTION_VIEW
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // Pass the URL via data Uri and EXTRA_TEXT (as expected by ADM's AEditor)
                data = Uri.parse(url)
                putExtra(Intent.EXTRA_TEXT, url)

                // Pass filenames using standard extras recognized by ADM
                putExtra("com.android.extra.filename", displayName)
                putExtra("title", displayName)
                putExtra("com.dv.adm.extra.FILENAME", displayName)

                // Pass specific string headers safely
                headers?.forEach { (key, value) ->
                    when {
                        key.equals("referer", ignoreCase = true) -> {
                            putExtra("Referer", value)
                            putExtra("referer", value)
                        }
                        key.equals("user-agent", ignoreCase = true) -> {
                            putExtra("User-Agent", value)
                            putExtra("user-agent", value)
                        }
                        key.equals("cookie", ignoreCase = true) -> {
                            putExtra("Cookie", value)
                            putExtra("cookie", value)
                        }
                    }
                }

                // Pass header bundle for download managers supporting bundle extras
                if (!headers.isNullOrEmpty()) {
                    val headerBundle = Bundle().apply {
                        headers.forEach { (k, v) -> putString(k, v) }
                    }
                    putExtra("android.media.intent.extra.HTTP_HEADERS", headerBundle)
                    putExtra("com.android.browser.headers", headerBundle)
                }
            }
        }

        val component = admPackage?.let { getAdmEditorComponent(context, it) }
        val primaryIntent = buildIntent(admPackage, component)

        try {
            context.startActivity(primaryIntent)
        } catch (e: Throwable) {
            // If explicit component launch failed, fallback to package ACTION_VIEW
            try {
                val fallbackIntent = buildIntent(admPackage, null)
                context.startActivity(fallbackIntent)
            } catch (e2: Throwable) {
                // If package launch failed, show chooser or toast
                try {
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(Intent.EXTRA_TEXT, url)
                            putExtra("com.android.extra.filename", displayName)
                        },
                        context.getString(R.string.download_provider_pref)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (e3: Throwable) {
                    showToast(R.string.adm_not_installed, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    /**
     * Launches ADM's batch download interface with multiple URLs.
     * ADM parses newline-separated links passed via Intent.EXTRA_TEXT to populate its batch download window.
     * Also copies all links to clipboard as a safety backup.
     */
    fun launchADMBatchDownload(
        context: Context,
        urls: List<String>,
        headers: Map<String, String>? = null
    ) {
        if (urls.isEmpty()) return

        val batchText = urls.joinToString("\n")

        // Always copy links to clipboard as a safety backup
        clipboardHelper(txt(R.string.download_all), batchText)

        val admPackage = getInstalledAdmPackage(context)
        if (admPackage == null) {
            showToast(context.getString(R.string.adm_batch_copied_clipboard, urls.size), Toast.LENGTH_LONG)
            return
        }

        val component = getAdmEditorComponent(context, admPackage)

        val intent = Intent().apply {
            if (component != null) {
                action = Intent.ACTION_MAIN
                this.component = component
            } else {
                action = Intent.ACTION_VIEW
                setPackage(admPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Pass newline-separated URLs into EXTRA_TEXT for ADM's batch download parser
            putExtra(Intent.EXTRA_TEXT, batchText)

            if (!headers.isNullOrEmpty()) {
                val headerBundle = Bundle().apply {
                    headers.forEach { (k, v) -> putString(k, v) }
                }
                putExtra("android.media.intent.extra.HTTP_HEADERS", headerBundle)
                putExtra("headers", headerBundle)
                headers["Referer"]?.let { putExtra("Referer", it) }
                headers["User-Agent"]?.let { putExtra("User-Agent", it) }
            }
        }

        try {
            context.startActivity(intent)
            showToast(context.getString(R.string.adm_batch_success_toast, urls.size), Toast.LENGTH_SHORT)
        } catch (e: Exception) {
            logError(e)
            showToast(context.getString(R.string.adm_batch_copied_clipboard, urls.size), Toast.LENGTH_LONG)
        }
    }

    /**
     * Builds a curl command string recognized by AB Download Manager's DownloadCredentialsFromCurl extractor.
     * This preserves HTTP headers (User-Agent, Referer, Cookies, etc.) preventing 403 Forbidden / Access Denied
     * responses from streaming servers.
     */
    fun buildCurlCommand(url: String, headers: Map<String, String>?): String {
        val effectiveHeaders = (headers?.toMutableMap() ?: mutableMapOf())
        if (!effectiveHeaders.keys.any { it.equals("user-agent", ignoreCase = true) }) {
            effectiveHeaders["User-Agent"] = USER_AGENT
        }
        val sb = StringBuilder("curl \"$url\"")
        for ((key, value) in effectiveHeaders) {
            if (value.isNotBlank() && !key.equals("Host", ignoreCase = true)) {
                val cleanVal = value.replace("\"", "")
                sb.append(" -H \"$key: $cleanVal\"")
            }
        }
        return sb.toString()
    }

    /**
     * Launches AB Download Manager (https://github.com/amir1376/ab-download-manager).
     * Targets AddDownloadActivity with ACTION_SEND using curl-formatted command
     * so that stream headers (User-Agent, Referer) are passed and extracted, preventing
     * Access Denied (HTTP 403) errors.
     */
    fun launchABDownload(
        context: Context,
        url: String,
        fileName: String,
        headers: Map<String, String>? = null
    ) {
        val displayName = if (fileName.contains('.')) fileName else "$fileName.mp4"
        val curlCommand = buildCurlCommand(url, headers)

        // Try ACTION_SEND with curl command first so AB Download Manager extracts headers
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(AB_DOWNLOAD_MANAGER_PACKAGE)
            component = ComponentName(
                AB_DOWNLOAD_MANAGER_PACKAGE,
                "com.abdownloadmanager.android.pages.add.AddDownloadActivity"
            )
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_TEXT, curlCommand)
            putExtra("android.intent.extra.TEXT", curlCommand)
            putExtra("title", displayName)
            putExtra("com.android.extra.filename", displayName)
        }

        try {
            context.startActivity(intent)
        } catch (e: Throwable) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage(AB_DOWNLOAD_MANAGER_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Intent.EXTRA_TEXT, curlCommand)
                    putExtra("android.intent.extra.TEXT", curlCommand)
                    putExtra("title", displayName)
                    putExtra("com.android.extra.filename", displayName)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Throwable) {
                try {
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(Intent.EXTRA_TEXT, url)
                            putExtra("com.android.extra.filename", displayName)
                        },
                        context.getString(R.string.download_provider_pref)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (e3: Throwable) {
                    showToast(R.string.ab_download_manager_not_installed, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    /**
     * Launches AB Download Manager's batch download interface with multiple URLs.
     * Formats URLs as curl commands with their respective headers (User-Agent, Referer, etc.),
     * which AB Download Manager's DownloadCredentialsFromCurl parses to attach headers
     * to every individual queued download, avoiding Access Denied (HTTP 403) errors.
     * Also copies all clean direct links to clipboard as a safety backup.
     */
    fun launchABBatchDownload(
        context: Context,
        urls: List<String>,
        headers: Map<String, String>? = null,
        itemHeaders: List<Map<String, String>?>? = null
    ) {
        if (urls.isEmpty()) return

        val batchText = urls.joinToString("\n")

        // Always copy clean direct links to clipboard as a safety backup
        clipboardHelper(txt(R.string.download_all), batchText)

        if (!isABDownloadManagerInstalled(context)) {
            showToast(context.getString(R.string.ab_batch_copied_clipboard, urls.size), Toast.LENGTH_LONG)
            return
        }

        // Format each episode URL with its individual headers or safe shared headers
        val curlLines = urls.mapIndexed { index, url ->
            val h = itemHeaders?.getOrNull(index) ?: headers
            buildCurlCommand(url, h)
        }
        val batchCurlText = curlLines.joinToString("\n")

        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(AB_DOWNLOAD_MANAGER_PACKAGE)
            component = ComponentName(
                AB_DOWNLOAD_MANAGER_PACKAGE,
                "com.abdownloadmanager.android.pages.add.AddDownloadActivity"
            )
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_TEXT, batchCurlText)
            putExtra("android.intent.extra.TEXT", batchCurlText)

            if (!headers.isNullOrEmpty()) {
                val headerBundle = Bundle().apply {
                    headers.forEach { (k, v) -> putString(k, v) }
                }
                putExtra("android.media.intent.extra.HTTP_HEADERS", headerBundle)
                putExtra("headers", headerBundle)
                headers["Referer"]?.let { putExtra("Referer", it) }
                headers["User-Agent"]?.let { putExtra("User-Agent", it) }
            }
        }

        try {
            context.startActivity(intent)
            showToast(context.getString(R.string.ab_batch_success_toast, urls.size), Toast.LENGTH_SHORT)
        } catch (e: Exception) {
            logError(e)
            showToast(context.getString(R.string.ab_batch_copied_clipboard, urls.size), Toast.LENGTH_LONG)
        }
    }

    /**
     * Shows a dialog displaying the direct download link, file name, and headers,
     * copies the URL to the clipboard automatically, and provides buttons to
     * copy the link, open in AB Download Manager / ADM, or dismiss.
     */
    fun showDirectLinkDialog(
        context: Context,
        url: String,
        fileName: String,
        headers: Map<String, String>? = null
    ) {
        val displayName = if (fileName.contains('.')) fileName else "$fileName.mp4"

        // Auto copy direct link to clipboard immediately for convenient pasting
        clipboardHelper(txt(R.string.direct_download_link_title), url)

        val paddingPx = 16.toPx
        val scrollView = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Filename section
        val fileLabel = TextView(context).apply {
            text = context.getString(R.string.direct_link_filename_label)
            setTextAppearance(android.R.style.TextAppearance_Small)
            alpha = 0.7f
        }
        val fileValue = TextView(context).apply {
            text = displayName
            setTextIsSelectable(true)
            setTextAppearance(android.R.style.TextAppearance_Medium)
            setPadding(0, 0, 0, paddingPx / 2)
        }
        container.addView(fileLabel)
        container.addView(fileValue)

        // URL section
        val urlLabel = TextView(context).apply {
            text = context.getString(R.string.direct_link_url_label)
            setTextAppearance(android.R.style.TextAppearance_Small)
            alpha = 0.7f
        }
        val urlValue = TextView(context).apply {
            text = url
            setTextIsSelectable(true)
            setTextAppearance(android.R.style.TextAppearance_Small)
            setBackgroundResource(android.R.drawable.editbox_background_normal)
            setPadding(12.toPx, 12.toPx, 12.toPx, 12.toPx)
        }
        container.addView(urlLabel)
        container.addView(urlValue)

        // Headers section (if any)
        val headerText = headers?.entries?.joinToString("\n") { "${it.key}: ${it.value}" }
        if (!headerText.isNullOrBlank()) {
            val headersLabel = TextView(context).apply {
                text = context.getString(R.string.direct_link_headers_label)
                setTextAppearance(android.R.style.TextAppearance_Small)
                alpha = 0.7f
                setPadding(0, paddingPx / 2, 0, 0)
            }
            val headersValue = TextView(context).apply {
                text = headerText
                setTextIsSelectable(true)
                setTextAppearance(android.R.style.TextAppearance_Small)
                setBackgroundResource(android.R.drawable.editbox_background_normal)
                setPadding(12.toPx, 12.toPx, 12.toPx, 12.toPx)
            }
            container.addView(headersLabel)
            container.addView(headersValue)
        }

        scrollView.addView(container)

        val hasAb = isABDownloadManagerInstalled(context)
        val hasAdm = isAdmInstalled(context)
        val currentProvider = getDownloadProvider(context)

        val neutralTextRes = when {
            hasAb && !hasAdm -> R.string.direct_link_open_ab
            !hasAb && hasAdm -> R.string.direct_link_open_adm
            hasAb && hasAdm -> {
                if (currentProvider == DOWNLOAD_PROVIDER_AB_DOWNLOAD_MANAGER) R.string.direct_link_open_ab
                else R.string.direct_link_open_adm
            }
            else -> R.string.direct_link_open_external
        }

        AlertDialog.Builder(context, R.style.AlertDialogCustom)
            .setTitle(R.string.direct_download_link_title)
            .setView(scrollView)
            .setPositiveButton(R.string.direct_link_copy) { _, _ ->
                clipboardHelper(txt(R.string.direct_download_link_title), url)
            }
            .setNeutralButton(neutralTextRes) { _, _ ->
                when {
                    hasAb && !hasAdm -> launchABDownload(context, url, displayName, headers)
                    !hasAb && hasAdm -> launchADMDownload(context, url, displayName, headers)
                    hasAb && hasAdm -> {
                        if (currentProvider == DOWNLOAD_PROVIDER_AB_DOWNLOAD_MANAGER) {
                            launchABDownload(context, url, displayName, headers)
                        } else {
                            launchADMDownload(context, url, displayName, headers)
                        }
                    }
                    else -> launchABDownload(context, url, displayName, headers)
                }
            }
            .setNegativeButton(R.string.direct_link_close) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
