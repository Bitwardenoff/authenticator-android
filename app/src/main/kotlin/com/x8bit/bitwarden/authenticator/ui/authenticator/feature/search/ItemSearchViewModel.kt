package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.search

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.concat
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.removeDiacritics
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ItemSearchViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
) :
    BaseViewModel<ItemSearchState, ItemSearchEvent, ItemSearchAction>(
        initialState = ItemSearchState(
            searchTerm = "",
            viewState = ItemSearchState.ViewState.Loading,
            dialogState = null,
        )
    ) {

    init {
        authenticatorRepository
            .getAuthCodesFlow()
            .map { ItemSearchAction.Internal.AuthenticatorDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ItemSearchAction) {
        when (action) {
            is ItemSearchAction.BackClick -> {
                sendEvent(ItemSearchEvent.NavigateBack)
            }

            is ItemSearchAction.DismissDialogClick -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
            }

            is ItemSearchAction.SearchTermChange -> {
                mutableStateFlow.update { it.copy(searchTerm = action.searchTerm) }
                recalculateViewState()
            }

            is ItemSearchAction.ItemClick -> {
                sendEvent(
                    event = ItemSearchEvent.ShowToast(
                        message = "Item ${action.itemId} copied to clipboard".asText(),
                    ),
                )
            }

            is ItemSearchAction.Internal.AuthenticatorDataReceive -> {
                handleAuthenticatorDataReceive(action)
            }
        }
    }

    private fun handleAuthenticatorDataReceive(action: ItemSearchAction.Internal.AuthenticatorDataReceive) {
        when (val data = action.dataState) {
            is DataState.Error -> authenticatorErrorReceive(authenticatorData = data)
            is DataState.Loaded -> authenticatorLoadedReceive(authenticatorData = data)
            DataState.Loading -> authenticatorLoadingReceive()
            is DataState.NoNetwork -> authenticatorNoNetworkReceive(authenticatorData = data)
            is DataState.Pending -> authenticatorDataPendingReceive(authenticatorData = data)
        }
    }

    private fun authenticatorErrorReceive(authenticatorData: DataState<List<VerificationCodeItem>>) {
        authenticatorData
            .data
            ?.let {
                updateStateWithAuthenticatorData(
                    authenticatorData = it,
                    clearDialogState = true,
                )
            }
            ?.run {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ItemSearchState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }
    }

    private fun authenticatorLoadedReceive(
        authenticatorData: DataState.Loaded<List<VerificationCodeItem>>,
    ) {
        updateStateWithAuthenticatorData(
            authenticatorData = authenticatorData.data,
            clearDialogState = true,
        )
    }

    private fun authenticatorLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = ItemSearchState.ViewState.Loading) }
    }

    private fun authenticatorNoNetworkReceive(
        authenticatorData: DataState<List<VerificationCodeItem>>,
    ) {
        authenticatorData
            .data
            ?.let {
                updateStateWithAuthenticatorData(
                    authenticatorData = it,
                    clearDialogState = true,
                )
            }
            ?.run {
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = ItemSearchState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(R.string.internet_connection_required_message.asText()),
                        ),
                        dialogState = null,
                    )
                }
            }
    }

    private fun authenticatorDataPendingReceive(
        authenticatorData: DataState.Pending<List<VerificationCodeItem>>,
    ) {
        updateStateWithAuthenticatorData(
            authenticatorData = authenticatorData.data,
            clearDialogState = false,
        )
    }

    //region Utility Functions
    private fun recalculateViewState() {
        authenticatorRepository.getAuthCodesFlow().value.data?.let { authenticatorData ->
            updateStateWithAuthenticatorData(
                authenticatorData = authenticatorData,
                clearDialogState = false
            )
        }
    }

    private fun updateStateWithAuthenticatorData(
        authenticatorData: List<VerificationCodeItem>,
        clearDialogState: Boolean,
    ) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                searchTerm = currentState.searchTerm,
                viewState = authenticatorData
                    .filterAndOrganize(state.searchTerm)
                    .toViewState(searchTerm = state.searchTerm),
                dialogState = currentState.dialogState.takeUnless { clearDialogState }
            )
        }
    }

    private fun List<VerificationCodeItem>.filterAndOrganize(searchTerm: String) =
        if (searchTerm.isBlank()) {
            emptyList()
        } else {
            this
                .groupBy { it.matchedSearch(searchTerm) }
                .flatMap { (priority, items) ->
                    when (priority) {
                        SortPriority.HIGH -> items.sortedBy { it.label }
                        SortPriority.LOW -> items.sortedBy { it.label }
                        null -> emptyList()
                    }
                }
        }

    private fun VerificationCodeItem.matchedSearch(searchTerm: String): SortPriority? {
        val term = searchTerm.removeDiacritics()
        val itemName = label.removeDiacritics()
        val itemId = id.takeIf { term.length > 8 }.orEmpty().removeDiacritics()
        val itemIssuer = issuer.orEmpty().removeDiacritics()
        return when {
            itemName.contains(other = term, ignoreCase = true) -> SortPriority.HIGH
            itemId.contains(other = term, ignoreCase = true) -> SortPriority.LOW
            itemIssuer.contains(other = term, ignoreCase = true) -> SortPriority.LOW
            else -> null
        }
    }

    private fun List<VerificationCodeItem>.toViewState(
        searchTerm: String,
    ): ItemSearchState.ViewState =
        when {
            searchTerm.isEmpty() -> {
                ItemSearchState.ViewState.Empty(message = null)
            }

            isNotEmpty() -> {
                ItemSearchState.ViewState.Content(
                    displayItems = toDisplayItemList(),
                )
            }

            else -> {
                ItemSearchState.ViewState.Empty(
                    message = R.string.there_are_no_items_that_match_the_search.asText()
                )
            }
        }

    private fun List<VerificationCodeItem>.toDisplayItemList(): List<ItemSearchState.DisplayItem> =
        this.map {
            it.toDisplayItem()
        }

    private fun VerificationCodeItem.toDisplayItem(): ItemSearchState.DisplayItem =
        ItemSearchState.DisplayItem(
            id = id,
            authCode = "",
            accountName = username ?: "",
            issuer = issuer,
            periodSeconds = periodSeconds,
            timeLeftSeconds = timeLeftSeconds,
            alertThresholdSeconds = 7,
            startIcon = IconData.Local(iconRes = R.drawable.ic_login_item),
            supportingLabel = label,
        )
    //endregion Utility Functions
}

data class ItemSearchState(
    val searchTerm: String,
    val viewState: ViewState,
    val dialogState: DialogState?,
) {
    sealed class ViewState : Parcelable {
        @Parcelize
        data class Content(
            val displayItems: List<DisplayItem>,
        ) : ViewState()

        @Parcelize
        data class Empty(val message: Text?) : ViewState()

        @Parcelize
        data class Error(val message: Text) : ViewState()

        @Parcelize
        data object Loading : ViewState()
    }

    sealed class DialogState : Parcelable {
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
        ) : DialogState()

        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }

    @Parcelize
    data class DisplayItem(
        val id: String,
        val authCode: String,
        val accountName: String,
        val issuer: String?,
        val periodSeconds: Int,
        val timeLeftSeconds: Int,
        val alertThresholdSeconds: Int,
        val startIcon: IconData,
        val supportingLabel: String? = null,
    ) : Parcelable
}

sealed class ItemSearchAction {
    data object BackClick : ItemSearchAction()

    data object DismissDialogClick : ItemSearchAction()

    data class SearchTermChange(val searchTerm: String) : ItemSearchAction()

    data class ItemClick(val itemId: String) : ItemSearchAction()

    sealed class Internal : ItemSearchAction() {
        data class AuthenticatorDataReceive(
            val dataState: DataState<List<VerificationCodeItem>>,
        ) : Internal()
    }
}

sealed class ItemSearchEvent {
    data object NavigateBack : ItemSearchEvent()

    data class ShowToast(val message: Text) : ItemSearchEvent()
}

enum class SortPriority {
    HIGH,
    LOW
}
