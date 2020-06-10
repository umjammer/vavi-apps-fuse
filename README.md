[![Release](https://jitpack.io/v/umjammer/vavi-apps-fuse.svg)](https://jitpack.io/#umjammer/vavi-apps-fuse) [![Java CI with Maven](https://github.com/umjammer/vavi-apps-fuse/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/umjammer/vavi-apps-fuse/actions)

# vavi-apps-fuse

fuse for java and many file systems.

## Status

| fs                 | list | upload | download | copy | move | rm | mkdir | cache | watch | project | library |
|--------------------|------|--------|----------|------|------|----|-------|-------|-------|---------|---------|
| google drive (v3)  | ✅    | ✅   | ✅       | ✅  | ✅  | ✅ | ✅    | ✅   |       | [sub module](vavi-nio-file-googledrive) | [google-api-services-drive](https://developers.google.com/api-client-library/java/) |
| one drive (v1)     | ✅    | ✅   | ✅       | ✅  | ✅  | ✅ | ✅    | ✅   |       | [sub module](vavi-nio-file-onedrive) | [OneDriveJavaSDK](https://github.com/umjammer/OneDriveJavaSDK) |
| one drive (graph)  | ✅    | ✅   | ✅       | ✅  | ✅  | ✅ | ✅    | ✅   |       | [sub module](vavi-nio-file-onedrive3) | [msgraph-sdk-java](https://github.com/microsoftgraph/msgraph-sdk-java) |
| one drive (graph)  | ✅    | ✅   | ✅       | ✅  | ✅  | ✅ | ✅    | ✅   |       | [sub module](vavi-nio-file-onedrive4) | [onedrive-java-client](https://github.com/iterate-ch/onedrive-java-client) |
| dropbox (v2)       | ✅    | ✅   | ✅       | ✅  | ✅  | ✅ | ✅    | ✅   |       | [java7-fs-dropbox](https://github.com/umjammer/java7-fs-dropbox) | |
| box (v2)           | ✅    | ✅   | ✅       | ✅  | ✅  | ✅ | ✅    | ✅   |       | [java7-fs-box](https://github.com/umjammer/java7-fs-box) | |
| vfs (sftp)         | ✅    | ✅   | ✅       | ✅  | ✅  | ✅ | ✅    | ✅   |       | [sub module](vavi-nio-file-vfs) | [commons-vfs2](https://commons.apache.org/proper/commons-vfs/), [jcifs](https://www.jcifs.org/), [jsch](http://www.jcraft.com/jsch/) |
| vfs (webdav ssh)   | 🚧    |      |           |     |      |     |       |      |        | [sub module](vavi-nio-file-vfs) | [commons-vfs2-jackrabbit2](https://commons.apache.org/proper/commons-vfs/commons-vfs2-jackrabbit2/) |
| archive            | ✅    | -    | ✅       | -   | -    | -   | -     | -    |        | [sub module](vavi-nio-file-archive) | [vavi-util-archive](https://github.com/umjammer/vavi-util-archive) |
| hfs+ (dmg)         | ✅    |      |           |     |      |     |       |      |        | [sub module](vavi-nio-file-hfs) | [hfsexplorer](https://github.com/umjammer/hfsexplorer) |
| [gathered](https://github.com/umjammer/vavi-apps-fuse/wiki/GatheredFileSystem) | ✅    | -    | ✅       | -    | -    | -   | -     | -    |       | [sub module](vavi-nio-file-gathered) | - |
| cyberduck (webdav ssh) | ✅ | ✅  | ✅       | ✅  | ✅   | ✅ | ✅   | ✅   |       | [vavi-nio-file-cyberduck](https://github.com/umjammer/vavi-nio-file-cyberduck) | [cyberduck.webdav](https://github.com/iterate-ch/cyberduck/) |
| cyberduck (sftp)   | ✅    | ✅   | ✅       | ✅  | ✅   | ✅ | ✅   | ✅   |       | [vavi-nio-file-cyberduck](https://github.com/umjammer/vavi-nio-file-cyberduck) | [cyberduck.ssh](https://github.com/iterate-ch/cyberduck/) |
| discutils (vdi/ntfs) | ✅  |      | ✅       |      |      |     |       |      |       | [vavi-nio-file-discutils](https://github.com/umjammer/vavi-nio-file-discutils) | |
| google play music  | ✅    | -    | ✅       | -    | -    | -   | -     | -    |       | [vavi-nio-file-googleplaymusic](https://github.com/umjammer/vavi-nio-file-googleplaymusic) | [gplaymusic](https://github.com/umjammer/gplaymusic) |
| fuse (javafs)      | ✅    | ✅   | ✅       | ?    | ?    | ✅ | ✅   | -    |       | [sub module](vavi-net-fuse) | [javafs](https://github.com/umjammer/javafs) |
| fuse (fuse-jna)    | ✅    | ✅   | ✅       | ?    | ?    | ✅ | ✅   | -    |       | [sub module](vavi-net-fuse) | [fuse-jna](https://github.com/EtiennePerot/fuse-jna) |
| fuse (jnr-fuse)    | 🚧    | ✅   | ✅       | ?    | ?    | ✅ | ✅   | -    |       | [sub module](vavi-net-fuse) | [jnr-fuse](https://github.com/SerCeMan/jnr-fuse) |

## Installation

### jars

 * https://jitpack.io/#umjammer/vavi-apps-fuse

### selenium chrome driver

 * download the [chromedriver executable](https://chromedriver.chromium.org/downloads) and locate it into some directory.
   * don't forget to run jvm with the jvm argument `-Dwebdriver.chrome.driver=/usr/local/bin/chromedriver`.

### each fs installation

 * [instruction wiki](https://github.com/umjammer/vavi-apps-fuse/wiki/Home#installation)

## How To

 * [how to replace authenticator](https://github.com/umjammer/vavi-apps-fuse/wiki/HowToReplaceAuthenticator)

## References

https://github.com/umjammer/vavi-apps-fuse/wiki/Libraries

## TODO

 * ~~amazon~~ (only ios and android are supported now)
 * ~~adrive~~ (i was banned)
 * ~~flickr~~ (quit 1T service)
 * ~~apache-commons-vfs~~ (wip)
 * ~~vavi-util-archive~~ (wip)
 * ~~shutdownHook~~
 * ~~https://github.com/unsound/hfsexplorer~~ (wip)
 * ~~virtualbox vdi~~ ([wip](https://github.com/umjammer/vavi-nio-file-discutils))
 * mincraft nbt
 * ~~credential from uri~~
 * ~~https://github.com/cryptomator/fuse-nio-adapter~~

