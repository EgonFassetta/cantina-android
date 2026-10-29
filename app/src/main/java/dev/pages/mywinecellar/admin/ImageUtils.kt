package dev.pages.mywinecellar.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/** Foto pronta da inviare: ridotta, in JPEG, con l'orientamento corretto. */
class PreparedImage(val bytes: ByteArray, val dataUrl: String, val bitmap: Bitmap)

private const val MAX_SIDE = 1280
private const val MAX_BYTES = 1_500_000

private fun finish(source: Bitmap, matrix: Matrix): PreparedImage {
    val k = min(1f, MAX_SIDE.toFloat() / max(source.width, source.height))
    matrix.postScale(k, k)
    val bmp = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    var quality = 82
    var out = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    while (out.size() > MAX_BYTES && quality > 45) {
        quality -= 12
        out = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }
    val bytes = out.toByteArray()
    return PreparedImage(bytes, "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP), bmp)
}

/** Riduce la foto a massimo 1280 pixel per lato (come sul sito) e la converte in JPEG. */
fun prepareLabelImage(context: Context, uri: Uri): PreparedImage {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val longest = max(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) throw IOException("Non riesco a leggere questa foto: prova con un'altra.")
    var sample = 1
    while (longest / sample > 2560) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: throw IOException("Non riesco a leggere questa foto: prova con un'altra.")

    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
    }
    return finish(decoded, m)
}

/** Immagine di prova disegnata al volo: serve solo alle prove automatiche (edizione di collaudo). */
fun sampleLabelImage(): PreparedImage {
    val bmp = Bitmap.createBitmap(640, 900, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(Color.parseColor("#F4EADA"))
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4A0F1A"); textSize = 52f; textAlign = Paint.Align.CENTER }
    c.drawText("CANTINA PROVA", 320f, 260f, p)
    c.drawText("Nuovo Vino 2022", 320f, 360f, p)
    c.drawText("Rosso Superiore", 320f, 460f, p)
    return finish(bmp, Matrix())
}
