# SHADOW_NET

Android VPN client with real `VpnService` integration and two selectable cores:

- Xray (`AndroidLibXrayLite`)
- sing-box (official Android CLI core behind the Xray TUN bridge)

## Build

Native binaries are intentionally not stored in Git. Gradle downloads the pinned Xray AAR and runs `scripts/fetch-vpn-cores.sh` to fetch sing-box automatically during `preBuild`.

Build with Android Studio or Gradle:

```bash
gradle testDebugUnitTest assembleDebug
```

Set `SHADOW_NET_ABIS` to fetch only specific ABIs during local development, for example:

```bash
SHADOW_NET_ABIS="arm64-v8a" bash scripts/fetch-vpn-cores.sh
```

CI fetches all four supported Android ABIs and produces ABI-split APKs.

## Connection behavior

The home-screen button requests Android VPN consent, starts a foreground `VpnService`, creates a real TUN interface, launches the selected core, and verifies the route before showing Connected. `AUTO` chooses the best compatible core first and attempts the other core when possible if startup or route verification fails.

No demo server is seeded. A real server configuration must be added and selected before connecting.

## License

GPL-3.0. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
