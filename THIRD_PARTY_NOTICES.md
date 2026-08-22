# Third-party components

SHADOW_NET distributes the following native networking cores in its APKs:

- **Xray-core / AndroidLibXrayLite** — Mozilla Public License 2.0 and the licenses of its bundled dependencies. Sources: https://github.com/XTLS/Xray-core and https://github.com/2dust/AndroidLibXrayLite
- **sing-box** — GNU General Public License version 3 with the project's additional naming provision. Source: https://github.com/SagerNet/sing-box

The build pins Xray in `app/build.gradle.kts` and sing-box in `scripts/fetch-vpn-cores.sh`. These binaries are downloaded during the build and are not committed to this repository.
