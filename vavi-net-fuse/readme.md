# vavi-net-fuse

integrated fuse filesystem mounter.

this is the api, implementation is provided as SPI.

## Providers

| fs                 | list | upload | download | copy | move | rm | mkdir | cache | watch |  library |
|--------------------|------|--------|----------|------|------|----|-------|-------|-------|---------|
| javafs      | ✅    | ✅   | ✅       | ✅  | ✅  | ✅  | ✅   | -    |        | [javafs](https://github.com/umjammer/javafs) |
| fuse-jna    | ✅    | ✅   | ✅       | ✅  | ✅  | ✅  | ✅   | -    |        | [fuse-jna](https://github.com/EtiennePerot/fuse-jna) |
| jnr-fuse    | ✅    | ✅   | ✅       | ✅  | ✅  | ✅  | ✅   | -    |        | [jnr-fuse](https://github.com/SerCeMan/jnr-fuse) |

## Workaround

 * if the test goes wrong, update macfuse and reboot the mac

## TODO

 * ~~https://github.com/cryptomator/fuse-nio-adapter~~
 * spotlight
   * https://stackoverflow.com/a/2335565
   * https://wiki.samba.org/index.php/Spotlight_with_Elasticsearch_Backend
   * https://gitlab.com/samba-team/samba/-/blob/master/source3/rpcclient/cmd_spotlight.c
 * `Path#toFile()` UnsupportedOperationException ... mount fs as fuse then `toFile`