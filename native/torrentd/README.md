### torrentd

This git repo is part of the open source project Archos Video Player Community Edition, 
a video player software for Android.

You can find the parent application, 
[Archos Video Player](https://play.google.com/store/apps/details?id=com.archos.mediacenter.video) 
on Google Play. 

This repo contains a "torrent-streaming" client. 

Please see the entry point repo: https://github.com/archos-sa/aos-AVP

The Android build uses libtorrent 2.1, C++17, and OpenSSL. WebTorrent is disabled
in both the libtorrent builder and torrentd; their feature definitions and archive
paths must stay aligned. Build from the Nova workspace with
`make native_build_native/torrentd`.

The command-line input is a local `.torrent` file or a magnet URI. Nova downloads
remote `.torrent` files before launching torrentd.
