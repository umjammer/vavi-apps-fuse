vavi-apps-fuse
----

# Depends

 * [patch for OneDriveJavaSDK](https://gist.github.com/umjammer/1f7bd3b1cf10516b135258407d2091be)
 * [patch for javafs](https://gist.github.com/umjammer/156326deb769c62b11a834aef6f69e81)
 * [patch for java7-fs-dropbox](https://gist.github.com/umjammer/4bc15c64cc06ceb3366c098f6ec84e11)
 * [patch for java7-fs-box](https://gist.github.com/umjammer/ccc88380d38660a39a2c4181637d685c)

# Licenses

# Status

| fs                 | authentication | autologin | list | upload | download | copy | move | rm | mkdir | cache | watch |
|--------------------|----------------|-----------|------|--------|----------|------|------|----|-------|-------|-------|
| google drive       | ✔              |           | ✔    | ✔      | ✔        |      | ✔    | ✔  | ✔     |       |       |
| one drive          | ✔              | ✔         | ✔    | ✔      | ✔        | ✔    | ✔    | ✔  | ✔     |       |       |
| dropbox            | ✔              | ✔         | ✔    | ✔      | ✔        |      |      | ✔  | ✔     |       |       |
| box                |                | ✔         | ✔    | ✔      | ✔        |      |      | ✔  | ✔     |       |       |
| vfs                |                |           |      |        |          |      |      |    |       |       |       |
| amazon cloud drive |                |           |      |        |          |      |      |    |       |       |       |
| flickr             |                |           |      |        |          |      |      |    |       |       |       |

# Libraries

  * fuse-jna
  * javafs <- !!!
  * jnr-fuse

  * google-api-java-client
  * OneDriveJavaSDK
  * dropbox-core-sdk
  * [box-java-sdk](https://github.com/box/box-java-sdk)
  * [amazon](https://github.com/yetisno/ACD-JAPI)

# TODO

 * amazon
 * adrive
 * flickr
 
 * apache-commons-vfs
 * vavi-util-archive
 
 * shutdownHook
 
 * google authentication automation
 
# MISC
 
## onedrive
  
  
## googledrive
 
### fuse-jna
  
 * https://github.com/smacke/gdrivefs (v2)
 * https://github.com/tbutter/gyingpan (v2)
 * https://github.com/stepank/jdbox (v2)

### java.nio.file.spi.FileSystemProvider

 * https://github.com/elek/jfs

## fuse-jna

 * https://github.com/bonifaido/zkfuse-jna (ZooKeeper)
 * https://github.com/Aypz/bcfusefs (BitCasa)
 * https://github.com/centic9/JGitFS
 
 * http://yy.hatenablog.jp/entry/2014/06/01/000000

## jnr-fuse

 * https://github.com/Alluxio/alluxio/tree/master/integration/fuse (memory)

## java.nio.file.spi.FileSystemProvider

 * https://github.com/usrflo/encfs4j (enc)
 * https://github.com/platformlayer/openstack-fileprovider (OpenStack Swift)
 * https://github.com/google/jimfs (mem)
 * https://github.com/marschall/zipfilesystem-standalone (zip)
 * https://github.com/lucastheisen/jsch-nio (ssh, sftp)
 * https://github.com/heikkipora/Amazon-S3-FileSystem-NIO2 (aws s3)

### javafs

 * https://github.com/fge/java7-fs-base (base?)
 
## javafs

 * https://github.com/fge/java7-fs-ftp (ftp)
 * https://github.com/fge/java7-fs-dropbox (dropbox) <- !!!
 * https://github.com/fge/java7-fs-box
