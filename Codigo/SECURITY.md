# Security Notes

## Secrets

- Keep real `.env`, `.env.local`, `local.properties`, `secrets.properties`, `google-services.json`, and generated `.next` files out of Git.
- Use `.env.example`, `.env.local.example`, and `local.properties.example` only as safe templates.
- Rotate any key that was previously committed or shared in screenshots/logs.

## Google Maps Keys

- Backend requests should use `GOOGLE_MAPS_API_KEY` from `backend/api/.env`.
- Android builds should receive `GOOGLE_MAPS_API_KEY` from `MobULite/local.properties` or CI environment variables.
- Restrict the Android Google key in Google Cloud by Android app package name and SHA-1 certificate fingerprint.
- Restrict server keys by allowed APIs and, in production, by server/IP where possible.

## Admin Dashboard

- `NEXT_PUBLIC_API_BASE_URL` is public browser configuration, not a secret.
- Keep admin JWT only in memory/session state. Do not store admin tokens in Git, local examples, or long-lived browser storage.

## Production Checklist

- Use HTTPS for backend and dashboard.
- Set a strong `JWT_SECRET` with at least 32 characters.
- Set `CORS_ORIGIN` to the exact production dashboard origin.
- Disable cleartext HTTP in Android release builds.
- Review dependency advisories before release.
