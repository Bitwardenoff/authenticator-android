package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.authenticator.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.authenticator.ui.platform.components.field.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.authenticator.ui.platform.components.icon.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.authenticator.ui.platform.components.indicator.BitwardenCircularCountdownIndicator
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold

private const val AUTH_CODE_SPACING_INTERVAL = 3

/**
 * Displays the authenticator item screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(
    viewModel: ItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = { },
    onNavigateToEditItem: (id: String) -> Unit = { },
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    EventsEffect(viewModel = viewModel) {
        when (it) {
            ItemEvent.NavigateBack -> onNavigateBack()
            is ItemEvent.NavigateToEdit -> onNavigateToEditItem(state.itemId)
        }
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.view_item),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ItemAction.CloseClick) }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ExtendedFloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = remember(viewModel) { { onNavigateToEditItem(state.itemId) } },
                    modifier = Modifier
                        .semantics { testTag = "EditItemButton" }
                        .padding(bottom = 16.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = stringResource(id = R.string.edit_item),
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay
    ) { innerPadding ->
        when (val viewState = state.viewState) {
            is ItemState.ViewState.Content -> ItemContent(
                modifier = Modifier
                    .imePadding()
                    .fillMaxSize()
                    .padding(innerPadding),
                viewState = viewState,
                onCopyTotpClick = { }
            )

            is ItemState.ViewState.Error -> {
                /*ItemErrorContent(state)*/
            }

            ItemState.ViewState.Loading -> ItemState.ViewState.Loading
        }
    }
}

@Composable
fun ItemContent(
    modifier: Modifier = Modifier,
    viewState: ItemState.ViewState.Content,
    onCopyTotpClick: () -> Unit,
) {
    LazyColumn(modifier = modifier) {

        item {
            BitwardenTextField(
                modifier = Modifier.padding(horizontal = 16.dp),
                label = stringResource(id = R.string.name),
                value = viewState.itemData.issuer,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextFieldWithActions(
                label = stringResource(id = R.string.verification_code_totp),
                value = viewState.itemData.totpCodeItemData?.verificationCode.orEmpty()
                    .chunked(AUTH_CODE_SPACING_INTERVAL)
                    .joinToString(" "),
                onValueChange = { },
                readOnly = true,
                singleLine = true,
                actions = {
                    BitwardenCircularCountdownIndicator(
                        timeLeftSeconds = viewState.itemData.totpCodeItemData?.timeLeftSeconds ?: 0,
                        periodSeconds = viewState.itemData.totpCodeItemData?.periodSeconds ?: 0,
                        alertThresholdSeconds = viewState.itemData.alertThresholdSeconds
                    )
                    BitwardenIconButtonWithResource(
                        iconRes = IconResource(
                            iconPainter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = stringResource(id = R.string.copy_totp),
                        ),
                        onClick = onCopyTotpClick,
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}
