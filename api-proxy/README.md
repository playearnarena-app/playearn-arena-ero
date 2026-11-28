# PlayEarn Arena API proxy

This Render Node service forwards API calls to the Oracle API origin over HTTPS. It allows only the bundled Android app's opaque local-file origin (`null`) and the official download website origin. It does not store credentials or request data.

Routes: `/api/v1/*` and `/healthz`.


Security notes: CORS does not allow credentialed cookies; the Android client must send its bearer session token explicitly. Responses include `no-store`, `nosniff`, `no-referrer`, and `DENY` frame headers. The opaque `null` origin remains allowed for the bundled Android WebView and must never be treated as authentication; API authorization is enforced by the Oracle backend.
