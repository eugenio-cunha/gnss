package br.com.b256.presentation.skyplot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.b256.domain.entities.Photo
import br.com.b256.domain.entities.TitleBlock
import br.com.b256.domain.usecases.CapturePhotoUseCase
import br.com.b256.domain.usecases.ObserveGnssStatusUseCase
import br.com.b256.domain.usecases.ObserveLocationUseCase
import br.com.b256.domain.usecases.ObserveOrientationUseCase
import br.com.b256.domain.usecases.ObservePhotosUseCase
import br.com.b256.domain.usecases.RemovePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `ViewModel` de referência para uma feature: `@HiltViewModel` + `@Inject constructor`, injetado
 * na tela via `hiltViewModel()` (ver [SkyPlotScreen]). Casos de uso do
 * `:domain` seriam injetados aqui como dependências do construtor, como em
 * [MainActivityViewModel].
 */
@HiltViewModel
class SkyPlotViewModel @Inject constructor(
    private val observeLocationUseCase: ObserveLocationUseCase,
    private val observeGnssStatusUseCase: ObserveGnssStatusUseCase,
    private val observeOrientationUseCase: ObserveOrientationUseCase,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val observePhotosUseCase: ObservePhotosUseCase,
    private val removePhotoUseCase: RemovePhotoUseCase,
) : ViewModel() {
    // Ações que podem ser disparadas para atualizar as informações de localização.
    private val refreshSignal = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    // Estado da localização atualizada.
    val locationState = refreshSignal
        .flatMapLatest { observeLocationUseCase() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // Estado do status do sinal Gnss.
    @OptIn(ExperimentalCoroutinesApi::class)
    val gnssStatus = refreshSignal
        .flatMapLatest { observeGnssStatusUseCase() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // Estado da orientação do dispositivo.
    @OptIn(ExperimentalCoroutinesApi::class)
    val orientation = refreshSignal
        .flatMapLatest { observeOrientationUseCase() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // Estado da captura/gravação da foto georreferenciada.
    private val _photoState = MutableStateFlow<PhotoUiState>(PhotoUiState.Idle)
    val photoState: StateFlow<PhotoUiState> = _photoState.asStateFlow()

    // Lista de fotos capturadas (mais recentes primeiro), já sem as que sumiram do dispositivo.
    val photos: StateFlow<List<Photo>> = observePhotosUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Atualiza as informações de localização.
     * */
    fun refresh() {
        viewModelScope.launch {
            refreshSignal.emit(Unit)
        }
    }

    /**
     * Compõe a foto recém-capturada em [sourceUri] com o [titleBlock] da posição atual e a grava
     * no armazenamento público. Ignora a chamada se já houver uma gravação em andamento e reporta
     * erro se ainda não há uma posição conhecida.
     */
    fun onPhotoCaptured(sourceUri: String, titleBlock: TitleBlock) {
        if (_photoState.value is PhotoUiState.Saving) return

        val location = locationState.value
        if (location == null) {
            _photoState.value = PhotoUiState.Error(hasLocation = false)
            return
        }

        _photoState.value = PhotoUiState.Saving
        viewModelScope.launch {
            _photoState.value = runCatching {
                capturePhotoUseCase(
                    sourceUri = sourceUri,
                    titleBlock = titleBlock,
                    location = location,
                )
            }.fold(
                onSuccess = { PhotoUiState.Saved(uri = it) },
                onFailure = { PhotoUiState.Error(hasLocation = true) },
            )
        }
    }

    /**
     * Sinaliza que o usuário tocou no botão de câmera sem haver um fix GPS — a captura não é
     * disparada e a UI mostra o aviso correspondente.
     */
    fun onCaptureWithoutLocation() {
        if (_photoState.value is PhotoUiState.Saving) return
        _photoState.value = PhotoUiState.Error(hasLocation = false)
    }

    /** Marca o resultado da última captura como já tratado pela UI (Toast/compartilhamento). */
    fun onPhotoResultConsumed() {
        if (_photoState.value is PhotoUiState.Saving) return
        _photoState.value = PhotoUiState.Idle
    }

    /** Remove a foto [uri] da lista de pré-visualização (o arquivo permanece na galeria). */
    fun onRemovePhoto(uri: String) {
        viewModelScope.launch {
            removePhotoUseCase(uri)
        }
    }
}

/**
 * Estado da funcionalidade "fotografar posição".
 *
 * - [Idle]: nada em andamento.
 * - [Saving]: compondo/gravando a imagem.
 * - [Saved]: imagem publicada; [Saved.uri] é o URI público (string) pronto para compartilhar.
 * - [Error]: falhou; [Error.hasLocation] indica se o motivo foi a ausência de um fix GPS.
 */
sealed interface PhotoUiState {
    data object Idle : PhotoUiState

    data object Saving : PhotoUiState

    data class Saved(val uri: String) : PhotoUiState

    data class Error(val hasLocation: Boolean) : PhotoUiState
}
