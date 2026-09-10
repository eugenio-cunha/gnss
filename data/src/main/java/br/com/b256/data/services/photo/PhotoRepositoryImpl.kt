package br.com.b256.data.services.photo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import br.com.b256.data.database.dao.PhotoDao
import br.com.b256.data.database.entities.PhotoEntity
import br.com.b256.data.database.mapper.asDomain
import br.com.b256.domain.entities.GpsLocation
import br.com.b256.domain.entities.Photo
import br.com.b256.domain.entities.TitleBlock
import br.com.b256.domain.interfaces.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Implementação de [PhotoRepository] sobre as APIs de mídia do Android + Room.
 *
 * Fluxo de [capture]:
 * 1. Decodifica a imagem de origem (a captura crua da câmera), respeitando a orientação EXIF.
 * 2. Desenha o [TitleBlock] num cartão translúcido no canto inferior direito ([drawTitleBlock]).
 * 3. Grava o JPEG num arquivo temporário no cache e injeta os metadados de GPS no EXIF.
 * 4. Publica o arquivo no [MediaStore] em `Pictures/GNSS SkyPlot/` — armazenamento público, sem
 *    necessidade de `WRITE_EXTERNAL_STORAGE` (scoped storage, minSdk 29), visível na galeria e
 *    disponível para compartilhamento com outros apps.
 * 5. Registra a referência no banco ([PhotoDao]) para alimentar a lista de pré-visualização.
 *
 * A lista exposta por [photos] é auto-saneada: entradas cujo arquivo não existe mais no
 * dispositivo são apagadas do banco a cada emissão.
 *
 * Toda a operação de I/O roda em [Dispatchers.IO].
 */
internal class PhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoDao: PhotoDao,
) : PhotoRepository {

    override val photos: Flow<List<Photo>> =
        photoDao.observeAll()
            .map { entities -> prune(entities).asDomain() }
            .flowOn(Dispatchers.IO)

    override suspend fun capture(
        sourceUri: String,
        titleBlock: TitleBlock,
        location: GpsLocation,
    ): String = withContext(Dispatchers.IO) {
        val source = sourceUri.toUri()

        val original = decodeOriented(source)
            ?: error("Não foi possível ler a imagem capturada em $sourceUri")

        val composed = try {
            drawTitleBlock(original, titleBlock)
        } finally {
            if (!original.isRecycled) original.recycle()
        }

        val displayName = buildDisplayName()
        val tempFile = File.createTempFile("gnss_photo_", ".jpg", context.cacheDir)
        try {
            tempFile.outputStream().use { out ->
                composed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            composed.recycle()

            writeExifGps(tempFile, location)

            val uri = publish(tempFile, displayName).toString()
            photoDao.upsert(
                PhotoEntity(
                    uri = uri,
                    displayName = displayName,
                    date = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                ),
            )
            uri
        } finally {
            tempFile.delete()
        }
    }

    override suspend fun remove(uri: String) {
        photoDao.deleteByUri(uri)
    }

    /**
     * Devolve apenas as entradas cujo arquivo ainda é encontrado no dispositivo, apagando do banco
     * as que sumiram (removidas pelo usuário por fora do app).
     */
    private suspend fun prune(entities: List<PhotoEntity>): List<PhotoEntity> {
        val present = ArrayList<PhotoEntity>(entities.size)
        entities.forEach { entity ->
            if (exists(entity.uri.toUri())) {
                present += entity
            } else {
                runCatching { photoDao.deleteByUri(entity.uri) }
            }
        }
        return present
    }

    /** `true` se [uri] ainda resolve para uma imagem existente no [MediaStore]. */
    private fun exists(uri: Uri): Boolean =
        runCatching {
            context.contentResolver
                .query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
                ?.use { it.moveToFirst() } == true
        }.getOrDefault(false)

    /**
     * Decodifica [uri] para um [Bitmap] mutável já rotacionado conforme a orientação EXIF, com o
     * lado maior limitado a [MAX_DIMENSION] px para não estourar a memória com fotos grandes.
     */
    private fun decodeOriented(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = orientationMatrix(orientation) ?: return decoded
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it != decoded) decoded.recycle() }
    }

    /**
     * Desenha o [titleBlock] sobre uma cópia mutável de [source] e a retorna. O tamanho da fonte,
     * das margens e do cartão é proporcional ao lado maior da imagem, para o bloco ficar legível
     * tanto numa miniatura quanto numa foto em resolução total.
     */
    private fun drawTitleBlock(source: Bitmap, titleBlock: TitleBlock): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: error("Não foi possível copiar o bitmap para compor o title block")
        val canvas = Canvas(bitmap)

        val base = maxOf(bitmap.width, bitmap.height).toFloat()
        val bodySize = (base * 0.018f).coerceIn(22f, 88f)
        val headingSize = bodySize * 1.15f
        val pad = bodySize * 0.7f
        val gap = bodySize * 0.5f
        val margin = bodySize * 0.9f

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_COLOR
            textSize = bodySize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT_COLOR
            textSize = headingSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val bodyLineHeight = bodyPaint.descent() - bodyPaint.ascent()
        val headingHeight = headingPaint.descent() - headingPaint.ascent()

        val contentWidth = maxOf(
            headingPaint.measureText(titleBlock.heading),
            titleBlock.lines.maxOfOrNull { bodyPaint.measureText(it) } ?: 0f,
        )
        val blockWidth = (contentWidth + pad * 2).coerceAtMost(bitmap.width - margin * 2)
        val blockHeight = pad * 2 + headingHeight + gap + titleBlock.lines.size * bodyLineHeight

        val right = bitmap.width - margin
        val bottom = bitmap.height - margin
        val left = (right - blockWidth).coerceAtLeast(margin)
        val top = (bottom - blockHeight).coerceAtLeast(margin)
        val rect = RectF(left, top, right, bottom)
        val radius = bodySize * 0.4f

        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BACKGROUND_COLOR
                style = Paint.Style.FILL
            },
        )
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ACCENT_COLOR
                style = Paint.Style.STROKE
                strokeWidth = maxOf(2f, bodySize * 0.06f)
            },
        )

        val textLeft = left + pad
        var baseline = top + pad - headingPaint.ascent()
        canvas.drawText(titleBlock.heading, textLeft, baseline, headingPaint)
        baseline += headingPaint.descent() + gap - bodyPaint.ascent()
        titleBlock.lines.forEach { line ->
            canvas.drawText(line, textLeft, baseline, bodyPaint)
            baseline += bodyLineHeight
        }

        return bitmap
    }

    /** Grava latitude/longitude/altitude e data-hora de [location] no EXIF de [file]. */
    private fun writeExifGps(file: File, location: GpsLocation) {
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            exif.setLatLong(location.latitude, location.longitude)
            location.altitude?.let { exif.setAltitude(it) }
            exif.setAttribute(
                ExifInterface.TAG_DATETIME_ORIGINAL,
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    .format(Date(location.date.toEpochMilliseconds())),
            )
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "GNSS SkyPlot")
            exif.saveAttributes()
        }
    }

    /**
     * Insere [file] no [MediaStore] público (coleção de imagens do volume primário externo), em
     * `Pictures/GNSS SkyPlot/`, e devolve o URI resultante. Usa `IS_PENDING` para tornar o item
     * visível só depois de o conteúdo estar totalmente escrito.
     */
    private fun publish(file: File, displayName: String): Uri {
        val resolver = context.contentResolver
        val collection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$ALBUM",
            )
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: error("O MediaStore recusou a inserção da imagem")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: error("Não foi possível abrir o stream de saída para $uri")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    private fun buildDisplayName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "GNSS_$stamp.jpg"
    }

    private fun sampleSize(width: Int, height: Int, max: Int): Int {
        var sample = 1
        while (width / sample > max || height / sample > max) sample *= 2
        return sample
    }

    private fun orientationMatrix(orientation: Int): Matrix? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return null
        }
        return matrix
    }

    private companion object {
        const val JPEG_QUALITY = 95
        const val MAX_DIMENSION = 4096
        const val ALBUM = "GNSS SkyPlot"

        val TEXT_COLOR = Color.parseColor("#F2F0F2")
        val ACCENT_COLOR = Color.parseColor("#DD912E")

        // Azul-noite do "instrumento" de sky plot, ~85% opaco, para o cartão do title block.
        val BACKGROUND_COLOR = Color.parseColor("#D9102A54")
    }
}
