package dev.ftb.app.structs.accounts;

import dev.ftb.app.services.accounts.LocalLoopback;

/// An in-progress browser login.
///
/// @param verifier    the PKCE code verifier. Secret, only ever sent to the token endpoint, never put in a URL or logged
/// @param redirectUrl the plain (not URL-encoded) redirect URI, sent again unchanged in the code exchange
public record LoopbackHolder(LocalLoopback loopback, String state, String verifier, String redirectUrl) {
}
