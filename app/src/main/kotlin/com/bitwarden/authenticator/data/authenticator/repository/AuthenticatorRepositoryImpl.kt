package com.bitwarden.authenticator.data.authenticator.repository

import android.net.Uri
import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.FileManager
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.manager.model.ExportJsonData
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorData
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.data.authenticator.repository.util.sortAlphabetically
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManager
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.data.platform.repository.util.combineDataStates
import com.bitwarden.authenticator.data.platform.repository.util.map
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * A "stop timeout delay" in milliseconds used to let a shared coroutine continue to run for the
 * specified period of time after it no longer has subscribers.
 */
private const val STOP_TIMEOUT_DELAY_MS: Long = 5_000L

/**
 * Default implementation of [AuthenticatorRepository].
 */
@Suppress("TooManyFunctions")
class AuthenticatorRepositoryImpl @Inject constructor(
    private val authenticatorDiskSource: AuthenticatorDiskSource,
    private val totpCodeManager: TotpCodeManager,
    private val fileManager: FileManager,
    private val importManager: ImportManager,
    dispatcherManager: DispatcherManager,
) : AuthenticatorRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val mutableCiphersStateFlow =
        MutableStateFlow<DataState<List<AuthenticatorItemEntity>>>(DataState.Loading)

    private val mutableTotpCodeResultFlow =
        bufferedMutableSharedFlow<TotpCodeResult>()

    override val totpCodeFlow: Flow<TotpCodeResult>
        get() = mutableTotpCodeResultFlow.asSharedFlow()

    override val authenticatorDataFlow: StateFlow<DataState<AuthenticatorData>> =
        ciphersStateFlow.map { cipherDataState ->
            when (cipherDataState) {
                is DataState.Error -> {
                    DataState.Error(
                        cipherDataState.error,
                        AuthenticatorData(cipherDataState.data.orEmpty()),
                    )
                }

                is DataState.Loaded -> {
                    DataState.Loaded(AuthenticatorData(items = cipherDataState.data))
                }

                DataState.Loading -> {
                    DataState.Loading
                }

                is DataState.NoNetwork -> {
                    DataState.NoNetwork(AuthenticatorData(items = cipherDataState.data.orEmpty()))
                }

                is DataState.Pending -> {
                    DataState.Pending(AuthenticatorData(items = cipherDataState.data))
                }
            }
        }.stateIn(
            scope = unconfinedScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_DELAY_MS),
            initialValue = DataState.Loading,
        )

    override val ciphersStateFlow: StateFlow<DataState<List<AuthenticatorItemEntity>>>
        get() = mutableCiphersStateFlow.asStateFlow()

    init {
        authenticatorDiskSource
            .getItems()
            .onStart {
                mutableCiphersStateFlow.value = DataState.Loading
            }
            .onEach {
                mutableCiphersStateFlow.value = DataState.Loaded(it.sortAlphabetically())
            }
            .launchIn(unconfinedScope)
    }

    override fun getItemStateFlow(itemId: String): StateFlow<DataState<AuthenticatorItemEntity?>> =
        authenticatorDataFlow
            .map { dataState ->
                dataState.map { authenticatorData ->
                    authenticatorData
                        .items
                        .find { it.id == itemId }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>> {
        return authenticatorDataFlow
            .map { dataState ->
                dataState.map { authenticatorData -> authenticatorData.items }
            }
            .flatMapLatest { cipherDataState ->
                val cipherList = cipherDataState.data ?: emptyList()
                totpCodeManager.getTotpCodesStateFlow(itemList = cipherList)
                    .map { verificationCodeDataStates ->
                        combineDataStates(
                            verificationCodeDataStates,
                            cipherDataState,
                        ) { verificationCodeItems, _ ->
                            // Just return the verification items; we are only combining the
                            // DataStates to know the overall state.
                            verificationCodeItems
                        }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )
    }

    override fun emitTotpCodeResult(totpCodeResult: TotpCodeResult) {
        mutableTotpCodeResultFlow.tryEmit(totpCodeResult)
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createItem(item: AuthenticatorItemEntity): CreateItemResult {
        return try {
            authenticatorDiskSource.saveItem(item)
            CreateItemResult.Success
        } catch (e: Exception) {
            CreateItemResult.Error
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun addItems(vararg items: AuthenticatorItemEntity): CreateItemResult {
        return try {
            authenticatorDiskSource.saveItem(*items)
            CreateItemResult.Success
        } catch (e: Exception) {
            CreateItemResult.Error
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun hardDeleteItem(itemId: String): DeleteItemResult {
        return try {
            authenticatorDiskSource.deleteItem(itemId)
            DeleteItemResult.Success
        } catch (e: Exception) {
            DeleteItemResult.Error
        }
    }

    override suspend fun exportVaultData(
        format: ExportVaultFormat,
        fileUri: Uri,
    ): ExportDataResult {
        return when (format) {
            ExportVaultFormat.JSON -> encodeVaultDataToJson(fileUri)
            ExportVaultFormat.CSV -> encodeVaultDataToCsv(fileUri)
        }
    }

    override suspend fun importVaultData(
        format: ImportFileFormat,
        fileData: IntentManager.FileData,
    ): ImportDataResult = fileManager.uriToByteArray(fileData.uri)
        .map {
            importManager
                .import(
                    importFileFormat = format,
                    byteArray = it,
                )
        }
        .fold(
            onSuccess = { it },
            onFailure = { ImportDataResult.Error() },
        )

    private suspend fun encodeVaultDataToCsv(fileUri: Uri): ExportDataResult {
        val headerLine =
            "folder,favorite,type,name,login_uri,login_totp"
        val dataLines = authenticatorDiskSource
            .getItems()
            .firstOrNull()
            .orEmpty()
            .joinToString("\n") { it.toCsvFormat() }

        val csvString = "$headerLine\n$dataLines"

        return if (fileManager.stringToUri(fileUri = fileUri, dataString = csvString)) {
            ExportDataResult.Success
        } else {
            ExportDataResult.Error
        }
    }

    private fun AuthenticatorItemEntity.toCsvFormat() =
        ",,1,$issuer,,${toOtpAuthUriString()},$issuer,$period,$digits"

    private suspend fun encodeVaultDataToJson(fileUri: Uri): ExportDataResult {
        val dataString: String = Json.encodeToString(
            ExportJsonData(
                encrypted = false,
                items = authenticatorDiskSource
                    .getItems()
                    .firstOrNull()
                    .orEmpty()
                    .map { it.toExportJsonItem() },
            ),
        )

        return if (
            fileManager.stringToUri(
                fileUri = fileUri,
                dataString = dataString,
            )
        ) {
            ExportDataResult.Success
        } else {
            ExportDataResult.Error
        }
    }

    private fun AuthenticatorItemEntity.toExportJsonItem() = ExportJsonData.ExportItem(
        id = id,
        folderId = null,
        organizationId = null,
        collectionIds = null,
        name = issuer,
        notes = null,
        type = 1,
        login = ExportJsonData.ExportItem.ItemLoginData(
            totp = toOtpAuthUriString(),
        ),
        favorite = false,
    )
}
