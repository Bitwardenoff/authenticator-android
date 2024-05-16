package com.bitwarden.authenticator.data.authenticator.datasource.disk.entity

/**
 * Enum representing the main type options for authenticator items.
 */
enum class AuthenticatorItemType {

    /**
     * A time-based one time password.
     */
    TOTP,

    /**
     * Steam's implementation of a one time password.
     */
    STEAM,
    ;

    companion object {
        fun fromStringOrNull(value: String): AuthenticatorItemType? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
