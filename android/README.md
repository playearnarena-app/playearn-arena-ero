# PlayEarn Arena Android beta

This project packages the existing User App HTML prototype inside an installable Android APK. The app loads the bundled local asset at `app/src/main/assets/app/index.html`; it does not open the public website as its app screen.

## Build

From this directory with JDK 17 and Android SDK Platform 35 installed:

```sh
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important beta limitations

- This is a UI preview build, not a production release.
- Login, wallet, spin, ad validation, referrals and payout flows in the source UI are not yet fully integrated with the production API.
- Rewards shown in this preview are simulations. No real earnings or cash payouts are provided.
- Cash withdrawals must remain disabled until legal, provider-policy and payment reviews are completed.
- Debug APK is for testing only. A production release requires a securely managed release keystore, end-to-end API/auth testing and security review.
