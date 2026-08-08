# Blockstream App — a native Bitcoin wallet for Android

[Blockstream App](https://blockstream.com/app/) is a non-custodial Bitcoin
wallet that lets you safely store, send, and receive your Bitcoin, guiding you
from your first bitcoin purchase to full self-custody.

It is available for Android and [iOS](https://github.com/Blockstream/green_ios),
and is built on [GDK](https://github.com/blockstream/gdk), our cross-platform
wallet library.

## Features

* **Bitcoin full stack** — on-chain Bitcoin, Lightning, and Liquid assets in
  one unified view.
* **Hardware wallet support** — keep your keys in cold storage with
  [Blockstream Jade](https://blockstream.com/jade/), our open-source hardware
  wallet.
* **Privacy** — no mandatory documents or personal data; connect to your own
  node and route traffic through Tor with a single tap.
* **Advanced security** — spending limits, watch-only access, and our multisig
  security model, all described in the
  [Blockstream Help Center](https://help.blockstream.com/hc/en-us/categories/900000056183-Blockstream-Green).
* **Multilingual** — available in more than a dozen languages.

<a href="https://play.google.com/store/apps/details?id=com.greenaddress.greenbits_android_wallet">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="50"/></a>
<a href="https://f-droid.org/packages/com.greenaddress.greenbits_android_wallet/">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="50"/></a>

## Build

See [BUILD.md](BUILD.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Translations

Help translate the app on
[Transifex](https://www.transifex.com/blockstream/blockstream-green/).

## Support

Visit the [Blockstream Help Center](https://help.blockstream.com/).

## Security

To report a security vulnerability, see [SECURITY.md](SECURITY.md).

## License

Blockstream App is released under the terms of the
[GNU General Public License v3.0](LICENSE).

## Verifying release authenticity

Verifying the APK signing certificate is important for your own security.
Follow these steps to confirm the APK you downloaded is authentic.

Unzip the APK, extract `/META-INF/GREENADD.RSA`, and run:

```
keytool -printcert -file GREENADD.RSA
```

Verify the certificate fingerprints match:

```
Certificate fingerprints:
	 SHA1: 7F:05:E3:DC:29:CB:E6:76:F5:0A:56:A2:80:1A:FD:37:91:96:8F:7A
	 SHA256: 32:F9:CC:00:B1:3F:BE:AC:E5:1E:2F:B5:1D:F4:82:04:4E:42:AD:34:A9:BD:91:2F:17:9F:ED:B1:6A:42:97:0E
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3
```

Download `SHA256SUMS.asc` from the
[release page](https://github.com/Blockstream/green_android/releases) and
verify that the checksum of your release file is listed:

```
shasum -a 256 --check SHA256SUMS.asc
```

The output must list `OK` after the name of the file you downloaded.

Import our GPG key:

```
gpg --keyserver keyserver.ubuntu.com --recv-keys 04BEBF2E35A2AF2FFDF1FA5DE7F054AA2E76E792
```

Verify that the checksums file is signed by our key:

```
gpg --verify SHA256SUMS.asc
```

The output must contain a line starting with `gpg: Good signature` and the
line `Primary key fingerprint: 04BE BF2E 35A2 AF2F FDF1  FA5D E7F0 54AA 2E76 E792`.
