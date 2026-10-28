package dev.pages.mywinecellar.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import dev.pages.mywinecellar.data.GeoAssets
import dev.pages.mywinecellar.data.GeoRegion
import dev.pages.mywinecellar.data.Wine
import dev.pages.mywinecellar.data.geoCounts
import dev.pages.mywinecellar.data.loadGeoAssets
import kotlin.math.min
import kotlin.math.roundToInt

/** Vista geografica: mappa dell'Italia e della Francia, come sul sito. */
@Composable
fun GeoScreen(wines: List<Wine>, onOpen: (Wine) -> Unit) {
    val context = LocalContext.current
    val assets: GeoAssets = remember { loadGeoAssets(context) }
    val (itCounts, frCounts) = remember(wines) { geoCounts(wines, assets.info) }
    var selected by remember { mutableStateOf<Pair<String, String>?>(null) } // ("it"|"fr", id)

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Vista geografica", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent)
        Spacer(Modifier.height(8.dp))
        Text("Italia", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Accent)
        GeoMapView(assets.it, assets.info.viewBoxIt, itCounts) { id -> selected = "it" to id }
        Spacer(Modifier.height(12.dp))
        Text("Francia", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Accent)
        GeoMapView(assets.fr, assets.info.viewBoxFr, frCounts) { id -> selected = "fr" to id }

        selected?.let { (country, id) ->
            val name = if (country == "it") assets.info.itIdToRegione[id] ?: id else assets.info.frAdminNames[id] ?: id
            val list = wines.filter { w ->
                w.quantita > 0 && when (country) {
                    "it" -> assets.info.itIdToRegione[id] == w.regione.trim()
                    else -> assets.info.frRegioneToAdmin[w.regione.trim()] == id
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
            list.forEach { w ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(w) }.padding(vertical = 6.dp)) {
                    Text("${w.cantina} ${w.nome} · ${w.quantita} bott.", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun GeoMapView(
    regions: List<GeoRegion>,
    viewBox: String,
    counts: Map<String, Int>,
    onRegion: (String) -> Unit,
) {
    val vb = remember(viewBox) { viewBox.trim().split(Regex("\\s+")).map { it.toFloat() } }
    val (vx, vy, vw, vh) = listOf(vb[0], vb[1], vb[2], vb[3])
    val paths: List<Pair<GeoRegion, android.graphics.Path>> = remember(regions) {
        regions.map { it to (PathParser.createPathFromPathData(it.d) ?: android.graphics.Path()) }
    }
    val maxCount = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val empty = Parchment
    val full = Accent

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(vw / vh)
            .pointerInput(paths) {
                detectTapGestures { offset ->
                    val s = min(size.width / vw, size.height / vh)
                    val ox = (size.width - vw * s) / 2f
                    val oy = (size.height - vh * s) / 2f
                    val x = (offset.x - ox) / s + vx
                    val y = (offset.y - oy) / s + vy
                    val hit = paths.firstOrNull { (_, p) ->
                        val r = android.graphics.Region()
                        r.setPath(p, android.graphics.Region(-100000, -100000, 100000, 100000))
                        r.contains(x.roundToInt(), y.roundToInt())
                    }
                    hit?.let { (reg, _) -> onRegion(reg.id) }
                }
            },
    ) {
        val s = min(size.width / vw, size.height / vh)
        val ox = (size.width - vw * s) / 2f
        val oy = (size.height - vh * s) / 2f
        translate(ox, oy) {
            scale(s, s, pivot = Offset.Zero) {
                translate(-vx, -vy) {
                    paths.forEach { (reg, p) ->
                        val c = counts[reg.id] ?: 0
                        val fill = if (c == 0) empty else lerp(empty, full, (c.toFloat() / maxCount).coerceIn(0.25f, 1f))
                        drawPath(p.asComposePath(), color = fill)
                        drawPath(p.asComposePath(), color = Color(0x55000000), style = Stroke(width = 0.6f / s, cap = StrokeCap.Round))
                    }
                    paths.forEach { (reg, p) ->
                        val c = counts[reg.id] ?: 0
                        if (c > 0) {
                            val b = RectF()
                            p.computeBounds(b, true)
                            val cx = (b.left + b.right) / 2f
                            val cy = (b.top + b.bottom) / 2f
                            drawCircle(color = Color(0xFFF4EADA), radius = 13f, center = Offset(cx, cy))
                            drawContext.canvas.nativeCanvas.drawText(
                                "$c", cx, cy + 5f,
                                Paint().apply { textSize = 14f; textAlign = Paint.Align.CENTER; color = android.graphics.Color.rgb(0x5a, 0x12, 0x1e); isFakeBoldText = true },
                            )
                        }
                    }
                }
            }
        }
    }
}
