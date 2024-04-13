package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.navbar

import androidx.navigation.NavGraphBuilder
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.composableWithStayTransitions

const val AUTHENTICATOR_NAV_BAR_ROUTE: String = "AuthenticatorNavBarRoute"

fun NavGraphBuilder.authenticatorNavBarDestination(
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit,
    onNavigateToManualKeyEntry: () -> Unit,
    onNavigateToEditItem: (itemId: String) -> Unit,
) {
    composableWithStayTransitions(
        route = AUTHENTICATOR_NAV_BAR_ROUTE,
    ) {
        AuthenticatorNavBarScreen(
            onNavigateToQrCodeScanner = onNavigateToQrCodeScanner,
            onNavigateToManualKeyEntry = onNavigateToManualKeyEntry,
            onNavigateToEditItem = onNavigateToEditItem,
            onNavigateToSearch = onNavigateToSearch,
        )
    }
}
