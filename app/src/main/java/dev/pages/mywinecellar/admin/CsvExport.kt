package dev.pages.mywinecellar.admin

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.pages.mywinecellar.data.Backend
import dev.pages.mywinecellar.data.parseWines
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun csvCell(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""

/** Esporta in CSV (sola lettura) i vini della cantina indicata e apre la condivisione. */
suspend fun exportCsv(context: Context, cellar: String) {
    val json = Backend.api(context).fetchWinesJson(cellar)
    val wines = parseWines(json)
    val header = listOf("Cantina", "Nome", "Annata", "Tipologia", "Colore", "Caratteristica", "Regione", "Paese", "Formato", "Quantita", "Prezzo", "Posizione")
    val rows = wines.map { w ->
        listOf(
            w.cantina, w.nome, w.annata, w.tipologia, w.coloreVino, w.caratteristica,
            w.regione, w.paese, w.formato, w.quantita.toString(), w.prezzo?.toString().orEmpty(), w.posizione,
        ).joinToString(";") { csvCell(it) }
    }
    val text = "\uFEFF" + (listOf(header.joinToString(";") { csvCell(it) }) + rows).joinToString("\n", postfix = "\n")
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, "cantina-$cellar.csv").also { it.writeText(text, Charsets.UTF_8) }
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Esporta cantina").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
