[![Release](https://jitpack.io/v/umjammer/vavi-apps-fuse.svg)](https://jitpack.io/#umjammer/vavi-apps-fuse)

# vavi-apps-fuse

fuse for java and many file systems.

# Status

| fs                 | list | upload | download | copy | move | rm | mkdir | cache | watch | library |
|--------------------|------|--------|----------|------|------|----|-------|-------|-------|---------|
| google drive       | ✅    | ✅      | ✅        | ✅    | ✅    | ✅  | ✅     | ✅     |       | [google-api-services-drive](https://developers.google.com/api-client-library/java/) |
| one drive (v1)     | ✅    | ✅      | ✅        | ✅    | ✅    | ✅  | ✅     | ✅     |       | [OneDriveJavaSDK](https://github.com/umjammer/OneDriveJavaSDK) |
| one drive (graph)  | ✅    | ✅      | ✅        | ✅    | ✅    | ✅  | ✅     | ✅     |       | [msgraph-sdk-java](https://github.com/microsoftgraph/msgraph-sdk-java) |
| dropbox            | ✅    | ✅      | ✅        | ✅    | ✅    | ✅  | ✅     | ✅     |       | [java7-fs-dropbox](https://github.com/umjammer/java7-fs-dropbox) |
| box                | ✅    | ✅      | ✅        | ✅    | ✅    | ✅  | ✅     | ✅     |       | [java7-fs-box](https://github.com/umjammer/java7-fs-box) |
| vfs ssh            | ✅    |        |          |      |      |    |       |       |       | [commons-vfs2]() |
| vfs webdav         | ✅    |        |          |      |      |    |       |       |       | [commons-vfs2-sandbox]() |
| archive            | ✅    |        |          |      |      |    |       |       |       | [vavi-util-archive]() |
| hfs                | ✅    |        |          |      |      |    |       |       |       | [hfsexplorer](https://github.com/umjammer/hfsexplorer) |


# TODO

 * ~~amazon~~ (only ios and android are supported now)
 * ~~adrive~~ (i was banned)
 * ~~flickr~~ (quit 1T service)

 * ~~apache-commons-vfs~~ (wip)
 * ~~vavi-util-archive~~ (wip)

 * shutdownHook

 * ~~https://github.com/unsound/hfsexplorer~~ (wip)

 * virtualbox vdi

 * mincraft nbt

# Libraries

## onedrive

### SDK

  * [OneDriveJavaSDK](https://github.com/tawalaya/OneDriveJavaSDK) (v1)
  * [onedrive-java-client](https://github.com/iterate-ch/onedrive-java-client) (cyberduck version, v2 graph)
  * [msgraph-sdk-java](https://github.com/microsoftgraph/msgraph-sdk-java) (v2 graph) 🎯

## googledrive

### SDK

  * [google-api-java-client](https://developers.google.com/api-client-library/java/)

## box

### SDK

  * [box-java-sdk](https://github.com/box/box-java-sdk)

## dropbox

### SDK

  * [dropbox-core-sdk](https://github.com/dropbox/dropbox-sdk-java)

## Amazon Data Cloud

### SDK

 * [amazon](https://github.com/yetisno/ACD-JAPI)

## fuse

 * [javafs](https://github.com/puniverse/javafs) 🎯
   * [patch for javafs](https://github.com/umjammer/javafs)
 * [jnr-fuse](https://github.com/SerCeMan/jnr-fuse)
 * [fuse-jna](https://github.com/EtiennePerot/fuse-jna)


### fuse-jna

 * https://github.com/smacke/gdrivefs (v2)
 * https://github.com/tbutter/gyingpan (v2)
 * https://github.com/stepank/jdbox (v2)

 * https://github.com/bonifaido/zkfuse-jna (ZooKeeper)
 * https://github.com/Aypz/bcfusefs (BitCasa)
 * https://github.com/centic9/JGitFS

 * http://yy.hatenablog.jp/entry/2014/06/01/000000

### jnr-fuse

 * https://github.com/Alluxio/alluxio/tree/master/integration/fuse (memory)

## java.nio.file.spi.FileSystemProvider

 * https://github.com/elek/jfs (google drive)
 * https://github.com/usrflo/encfs4j (enc)
 * https://github.com/platformlayer/openstack-fileprovider (OpenStack Swift)
 * https://github.com/google/jimfs (mem)
 * https://github.com/marschall/zipfilesystem-standalone (zip)
 * https://github.com/lucastheisen/jsch-nio (ssh, sftp)
 * https://github.com/heikkipora/Amazon-S3-FileSystem-NIO2 (aws s3)
 * https://github.com/fge/java7-fs-base (java7 nio file base) 🎯

### java7-fs

 * https://github.com/fge/java7-fs-ftp (ftp)
 * https://github.com/fge/java7-fs-dropbox (dropbox) 🎯
 * https://github.com/fge/java7-fs-box (box) 🎯

## vfs

 * [jbossvfs](https://github.com/jbossas/jboss-vfs)
 * [truevfs](http://truevfs.net/)