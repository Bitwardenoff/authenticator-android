package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.ItemListingExpandableFabAction
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledTonalButton
import com.x8bit.bitwarden.authenticator.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.authenticator.ui.platform.components.fab.ExpandableFabIcon
import com.x8bit.bitwarden.authenticator.ui.platform.components.fab.ExpandableFloatingActionButton
import com.x8bit.bitwarden.authenticator.ui.platform.components.icon.BitwardenIcon
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.authenticator.ui.platform.theme.Typography

/**
 * Displays the item listing screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListingScreen(
    viewModel: ItemListingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit,
    onNavigateToManualKeyEntry: () -> Unit,
    onNavigateToEditItemScreen: (id: String) -> Unit,
    onNavigateToSyncWithBitwardenScreen: () -> Unit,
    onNavigateToImportScreen: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ItemListingEvent.NavigateBack -> onNavigateBack()
            is ItemListingEvent.NavigateToSearch -> onNavigateToSearch()
            is ItemListingEvent.DismissPullToRefresh -> pullToRefreshState.endRefresh()
            is ItemListingEvent.NavigateToQrCodeScanner -> onNavigateToQrCodeScanner()
            is ItemListingEvent.NavigateToManualAddItem -> onNavigateToManualKeyEntry()
            is ItemListingEvent.ShowToast -> {
                Toast
                    .makeText(
                        context,
                        event.message(context.resources),
                        Toast.LENGTH_LONG
                    )
                    .show()
            }

            is ItemListingEvent.NavigateToEditItem -> onNavigateToEditItemScreen(event.id)
        }
    }

    ItemListingDialogs(
        dialog = state.dialog,
        onDismissRequest = remember(viewModel) {
            {
                viewModel.trySendAction(
                    ItemListingAction.DialogDismiss,
                )
            }
        },
        onConfirmDeleteClick = remember(viewModel) {
            { itemId ->
                viewModel.trySendAction(
                    ItemListingAction.ConfirmDeleteClick(itemId = itemId),
                )
            }
        }
    )

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.verification_codes),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
                actions = {
                    if (state.viewState !is ItemListingState.ViewState.NoItems) {
                        BitwardenIcon(
                            modifier = Modifier.clickable {
                                viewModel.trySendAction(ItemListingAction.SearchClick)
                            },
                            iconData = IconData.Local(R.drawable.ic_search_24px),
                            tint = MaterialTheme.colorScheme.surfaceTint
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExpandableFloatingActionButton(
                modifier = Modifier
                    .semantics { testTag = "AddItemButton" }
                    .padding(bottom = 16.dp),
                label = R.string.add_item.asText(),
                items = listOf(
                    ItemListingExpandableFabAction.ScanQrCode(
                        label = R.string.scan_a_qr_code.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = stringResource(id = R.string.scan_a_qr_code),
                            testTag = "ScanQRCodeButton",
                        ),
                        onScanQrCodeClick = {
                            viewModel.trySendAction(ItemListingAction.ScanQrCodeClick)
                        }
                    ),
                    ItemListingExpandableFabAction.EnterSetupKey(
                        label = R.string.enter_a_setup_key.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = R.drawable.ic_keyboard_24px),
                            contentDescription = stringResource(id = R.string.enter_a_setup_key),
                            testTag = "EnterSetupKeyButton",
                        ),
                        onEnterSetupKeyClick = {
                            viewModel.trySendAction(ItemListingAction.EnterSetupKeyClick)
                        }
                    )
                ),
                expandableFabIcon = ExpandableFabIcon(
                    iconData = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        testTag = "AddItemButton",
                    ),
                    iconRotation = 45f,
                ),
            )
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val currentState = state.viewState) {
                is ItemListingState.ViewState.Content -> {
                    LazyColumn {
                        items(currentState.itemList) {
                            VaultVerificationCodeItem(
                                authCode = it.authCode,
                                issuer = it.issuer,
                                periodSeconds = it.periodSeconds,
                                timeLeftSeconds = it.timeLeftSeconds,
                                alertThresholdSeconds = it.alertThresholdSeconds,
                                startIcon = it.startIcon,
                                onCopyClick = remember(viewModel) { { /*TODO*/ } },
                                onItemClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            ItemListingAction.ItemClick(it.id)
                                        )
                                    }
                                },
                                onEditItemClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            ItemListingAction.ItemClick(it.id)
                                        )
                                    }
                                },
                                onDeleteItemClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            ItemListingAction.DeleteItemClick(it.id)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                supportingLabel = it.supportingLabel,
                            )
                        }
                    }
                }

                is ItemListingState.ViewState.Error -> {
                    Text(
                        text = "Error! ${currentState.message}",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                ItemListingState.ViewState.NoItems,
                ItemListingState.ViewState.Loading,
                -> {
                    EmptyItemListingContent(
                        onAddCodeClick = onNavigateToQrCodeScanner,
                        onSyncWithBitwardenClick = onNavigateToSyncWithBitwardenScreen,
                        onImportItemsClick = onNavigateToImportScreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemListingDialogs(
    dialog: ItemListingState.DialogState?,
    onDismissRequest: () -> Unit,
    onConfirmDeleteClick: (itemId: String) -> Unit,
) {
    when (dialog) {
        ItemListingState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = R.string.syncing.asText(),
                ),
            )
        }

        is ItemListingState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialog.title,
                    message = dialog.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        is ItemListingState.DialogState.DeleteConfirmationPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.delete),
                message = dialog.message(),
                confirmButtonText = stringResource(id = R.string.ok),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = {
                    onConfirmDeleteClick(dialog.itemId)
                },
                onDismissClick = onDismissRequest,
                onDismissRequest = onDismissRequest
            )
        }

        null -> Unit
    }
}

/**
 * Displays the item listing screen with no existing items.
 */
@Composable
fun EmptyItemListingContent(
    modifier: Modifier = Modifier,
    onAddCodeClick: () -> Unit = {},
    onSyncWithBitwardenClick: () -> Unit = {},
    onImportItemsClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.ic_empty_vault),
            contentDescription = stringResource(
                id = R.string.empty_item_list,
            ),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.you_dont_have_items_to_display),
            style = Typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.empty_item_list_instruction),
        )

        Spacer(modifier = Modifier.height(16.dp))
        BitwardenFilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.add_code),
            onClick = onAddCodeClick,
        )

        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextButton(
            label = stringResource(id = R.string.sync_items_with_bitwarden),
            onClick = onSyncWithBitwardenClick,
        )

        Spacer(modifier = Modifier.height(16.dp))
        BitwardenTextButton(
            label = stringResource(id = R.string.import_items),
            onClick = onImportItemsClick,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun EmptyListingContentPreview() {
    EmptyItemListingContent(
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
