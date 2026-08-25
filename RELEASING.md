# Releasing

Create a GitHub Release from a `v<version>` tag that points to a commit with the
same non-SNAPSHOT Maven version. The release workflow publishes the signed
artifacts to Maven Central.

Configure the `maven-central` GitHub environment with these secrets:

- `CENTRAL_USERNAME` and `CENTRAL_PASSWORD`: Maven Central Portal token credentials
- `GPG_PRIVATE_KEY`: ASCII-armored private signing key
- `GPG_PASSPHRASE`: signing key passphrase
