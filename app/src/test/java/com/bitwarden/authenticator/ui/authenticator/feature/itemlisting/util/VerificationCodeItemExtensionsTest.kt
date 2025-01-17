package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.manager.util.createMockVerificationCodeItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerificationCodeItemExtensionsTest {

    @Test
    fun `toViewState empty list should return NoItems state`() {
        assertEquals(
            ItemListingState.ViewState.NoItems,
            emptyList<VerificationCodeItem>().toViewState(alertThresholdSeconds = 7),
        )
    }

    @Test
    fun `toViewState non empty list should return Content state`() {
        val favoriteItem = createMockVerificationCodeItem(number = 1, favorite = true)
        val nonFavoriteItem = createMockVerificationCodeItem(number = 2)
        val items = listOf(favoriteItem, nonFavoriteItem)

        val expectedFavoriteItems = listOf(
            VerificationCodeDisplayItem(
                id = favoriteItem.id,
                issuer = favoriteItem.issuer,
                username = favoriteItem.username,
                timeLeftSeconds = favoriteItem.timeLeftSeconds,
                periodSeconds = favoriteItem.periodSeconds,
                alertThresholdSeconds = 7,
                authCode = favoriteItem.code,
                favorite = favoriteItem.favorite,
            ),
        )

        val expectedNonFavoriteItems = listOf(
            VerificationCodeDisplayItem(
                id = nonFavoriteItem.id,
                issuer = nonFavoriteItem.issuer,
                username = nonFavoriteItem.username,
                timeLeftSeconds = nonFavoriteItem.timeLeftSeconds,
                periodSeconds = nonFavoriteItem.periodSeconds,
                alertThresholdSeconds = 7,
                authCode = nonFavoriteItem.code,
                favorite = nonFavoriteItem.favorite,
            ),
        )

        assertEquals(
            ItemListingState.ViewState.Content(
                favoriteItems = expectedFavoriteItems,
                itemList = expectedNonFavoriteItems,
            ),
            items.toViewState(alertThresholdSeconds = 7),
        )
    }
}
