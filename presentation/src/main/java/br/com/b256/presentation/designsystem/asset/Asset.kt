package br.com.b256.presentation.designsystem.asset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share

/**
 * Registro centralizado de recursos visuais (assets) dentro do design system.
 *
 * Este objeto serve como ponto de acesso único para ícones do Material Design ([ImageVector])
 * e identificadores de recursos drawable locais ([R.drawable]). Ele mapeia ícones padronizados para nomes
 * semanticamente relevantes ao domínio da aplicação e fornece uma função utilitária para recuperação
 * dinâmica de recursos via identificador de texto.
 */
object Asset {
    val Add = Icons.Outlined.Add
    val Camera = Icons.Outlined.PhotoCamera
    val Close = Icons.Outlined.Close
    val Delete = Icons.Outlined.Delete
    val Settings = Icons.Outlined.Settings
    val Share = Icons.Outlined.Share
}
