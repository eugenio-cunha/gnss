package br.com.b256.presentation.skyplot.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import br.com.b256.domain.entities.Photo
import br.com.b256.presentation.R
import br.com.b256.presentation.designsystem.asset.Asset
import br.com.b256.presentation.designsystem.theme.PaddingSingle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodifica a imagem em [uri] fora da thread principal, limitando o lado maior a [maxPx] px
 * (via `setTargetSampleSize`) para não estourar a memória. Enquanto carrega — ou se o arquivo não
 * for encontrado — retorna `null`.
 *
 * @param uri URI (`content://…`) da imagem.
 * @param maxPx Limite do maior lado do bitmap resultante.
 */
@Composable
internal fun rememberImageBitmap(uri: String, maxPx: Int): ImageBitmap? {
    val context = LocalContext.current
    var image by remember(uri, maxPx) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri, maxPx) {
        image = withContext(Dispatchers.IO) {
            runCatching { decodeBounded(context, uri.toUri(), maxPx).asImageBitmap() }.getOrNull()
        }
    }

    return image
}

private fun decodeBounded(context: Context, uri: Uri, maxPx: Int): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longestSide = maxOf(info.size.width, info.size.height)
        if (longestSide > maxPx) {
            decoder.setTargetSampleSize((longestSide / maxPx).coerceAtLeast(1))
        }
    }
}

/**
 * Visualização em tela cheia de uma [Photo]: fundo preto, imagem ajustada (`Fit`) e uma barra
 * superior com os botões de fechar, compartilhar e remover da lista.
 *
 * @param photo Foto a exibir.
 * @param onDismiss Fecha o visualizador.
 * @param onShare Compartilha a foto (folha de compartilhamento nativa).
 * @param onRemove Remove a foto da lista de pré-visualização (e fecha o visualizador).
 */
@Composable
internal fun PhotoViewerDialog(
    photo: Photo,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val image = rememberImageBitmap(uri = photo.uri, maxPx = 2048)

            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = photo.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(PaddingSingle),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ViewerAction(
                    icon = Asset.Close,
                    contentDescription = stringResource(
                        R.string.presentation_skyplot_photo_viewer_close,
                    ),
                    onClick = onDismiss,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PaddingSingle)) {
                    ViewerAction(
                        icon = Asset.Share,
                        contentDescription = stringResource(
                            R.string.presentation_skyplot_photo_share_title,
                        ),
                        onClick = onShare,
                    )
                    ViewerAction(
                        icon = Asset.Delete,
                        contentDescription = stringResource(
                            R.string.presentation_skyplot_photo_remove,
                        ),
                        onClick = onRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}
