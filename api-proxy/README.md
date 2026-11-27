# PlayEarn Arena API proxy

This Render Node service forwards API calls to the Oracle API origin over HTTPS. It allows only the bundled Android app's opaque local-file origin (`null`) and the official download website origin. It does not store credentials or request data.

Routes: `/api/v1/*` and `/healthz`.
