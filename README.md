[![Release](https://jitpack.io/v/umjammer/vavi-apps-fuse.svg)](https://jitpack.io/#umjammer/vavi-apps-fuse)
[![Java CI](https://github.com/umjammer/vavi-apps-fuse/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-fuse/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-fuse/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-apps-fuse/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-apps-fuse

🌏 mount the world!

## Status

| fs                 | list | upload | download | copy | move | rm | mkdir | cache | watch | project                                                                                    | library                                                                                                                                     |
|--------------------|------|--------|----------|------|------|----|-------|-------|-------|--------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| google drive (v3)  | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | ✅     | 🚧    | [sub module](vavi-nio-file-googledrive)                                                    | [google-api-services-drive](https://developers.google.com/api-client-library/java/)                                                         |
| one drive (v1)     | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | ✅     | 🚧    | [sub module](vavi-nio-file-onedrive)                                                       | [OneDriveJavaSDK](https://github.com/umjammer/OneDriveJavaSDK)                                                                              |
| one drive (graph)  | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | ✅     | 🚧    | [sub module](vavi-nio-file-onedrive3)                                                      | [onedrive-java-client](https://github.com/iterate-ch/onedrive-java-client)                                                                      |
| one drive (graph)  | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | ✅     | 🚧    | [sub module](vavi-nio-file-onedrive4)                                                      | [msgraph-sdk-java](https://github.com/microsoftgraph/msgraph-sdk-java)                                                                  |
| dropbox (v2)       | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | ✅     | 🚧    | [vavi-nio-file-dropbox](https://github.com/umjammer/vavi-nio-file-dropbox)                 |                                                                                                                                             |
| box (v2)           | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | ✅     | 🚧    | [vavi-nio-file-box](https://github.com/umjammer/vavi-nio-file-box)                         |                                                                                                                                             |
| vfs (sftp)         | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | -     |       | [sub module](vavi-nio-file-vfs)                                                            | [commons-vfs2](https://commons.apache.org/proper/commons-vfs/), [jsch](http://www.jcraft.com/jsch/)                                         |
| vfs (smb:cifs-ng)  | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | -     |       | [sub module](vavi-nio-file-vfs)                                                            | [commons-vfs2-cifs](https://github.com/vbauer/commons-vfs2-cifs), [jcifs-ng](https://github.com/AgNO3/jcifs-ng)                             |
| vfs (smb:smbj)     | ✅    | ✅   | ✅       | ✅    | ✅   | ✅ | ✅    | -     |       | [sub module](vavi-nio-file-vfs)                                                            | [commons-vfs2-smb](https://github.com/umjammer/commons-vfs2-smb)                                                                            |
| vfs (webdav ssh)   | 🚧    |      |          |      |      |     |       |       |       | [sub module](vavi-nio-file-vfs)                                                            | [commons-vfs2-jackrabbit2](https://commons.apache.org/proper/commons-vfs/commons-vfs2-jackrabbit2/)                                         |
| vfs (smb:cifs)     | ✅    | ✅   | ✅       | ✅   | ✅    | ✅ | ✅    | -     |       | [sub module](vavi-nio-file-vfs)                                                            | [commons-vfs2-sandbox](https://commons.apache.org/proper/commons-vfs/commons-vfs2-sandbox/), [jcifs-ng](https://github.com/AgNO3/jcifs-ng/) |
| archive            | ✅    | -    | ✅       | -    | -    | -   | -     | -     |       | [sub module](vavi-nio-file-archive)                                                        | [vavi-util-archive](https://github.com/umjammer/vavi-util-archive)                                                                          |
| hfs+ (dmg)         | ✅    |      | ✅       |      |      |     |       |       |       | [sub module](vavi-nio-file-hfs)                                                            | [hfsexplorer](https://github.com/umjammer/hfsexplorer)                                                                                      |
| [gathered](https://github.com/umjammer/vavi-apps-fuse/wiki/GatheredFileSystem) | ✅ | - | ✅ | -    | -    | - | - | -     | | [sub module](vavi-nio-file-gathered)                                                       | -                                                                                                                                           |
| cyberduck (webdav ssh) | ✅ | ✅  | ✅       | ✅    | ✅    | ✅ | ✅   | ✅     |       | [vavi-nio-file-cyberduck](https://github.com/umjammer/vavi-nio-file-cyberduck)             | [cyberduck.webdav](https://github.com/iterate-ch/cyberduck/)                                                                                |
| cyberduck (sftp)   | ✅    | ✅   | ✅       | ✅    | ✅    | ✅ | ✅   | ✅     |       | [vavi-nio-file-cyberduck](https://github.com/umjammer/vavi-nio-file-cyberduck)             | [cyberduck.ssh](https://github.com/iterate-ch/cyberduck/)                                                                                   |
| discutils (vdi/ntfs) | ✅  |      | ✅       |      |      |     |      |       |       | [vavi-nio-file-discutils](https://github.com/umjammer/vavi-nio-file-discutils)             |                                                                                                                                             |
| ~~google play music~~ | ✅ | -    | ✅       | -    | -    | -   | -    | -     |       | [vavi-nio-file-googleplaymusic](https://github.com/umjammer/vavi-nio-file-googleplaymusic) | [gplaymusic](https://github.com/umjammer/gplaymusic)                                                                                        |
||||||
| fuse (javafs)      | ✅    | ✅   | ✅       | ✅    | ✅    | ✅  | ✅   | ✅     |       | [vavi-nio-file-fuse](https://github.com/umjammer/vavi-nio-file-fuse)                                                        | [javafs](https://github.com/umjammer/javafs)                                                                                                |
| fuse (fuse-jna)    | ✅    | ✅   | ✅       | ✅    | ✅    | ✅  | ✅   | ✅     |       | [vavi-nio-file-fuse](https://github.com/umjammer/vavi-nio-file-fuse)                                                                | [fuse-jna](https://github.com/EtiennePerot/fuse-jna)                                                                                        |
| fuse (jnr-fuse)    | ✅    | ✅   | ✅       | ✅    | ✅    | ✅  | ✅   | ✅     |       | [vavi-nio-file-fuse](https://github.com/umjammer/vavi-nio-file-fuse)                                                                | [jnr-fuse](https://github.com/SerCeMan/jnr-fuse)                                                                                            |

## Usage

```java
    URI uri = URI.create("googledrive:///?id=you@gmail.com");
    FileSystems fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
    Fuse.getFuse().mount(fs, "/your/mout/point", Collections.emptyMap());
```

## Installation

 * [maven](https://jitpack.io/#umjammer/vavi-apps-fuse)

### jars

 * https://jitpack.io/#umjammer/vavi-apps-fuse

### ~~selenium chrome driver~~ (obsolete, use os default browser)

 * download the [chromedriver executable](https://chromedriver.chromium.org/downloads) and locate it into some directory.
   * don't forget to run jvm with the jvm argument `-Dwebdriver.chrome.driver=/usr/local/bin/chromedriver`.

### each fs installation

 * [instruction wiki](https://github.com/umjammer/vavi-apps-fuse/wiki/Home#installation)

### fuse

 * install [macFUSE](https://osxfuse.github.io/)
 * jvmarg (fuse-jna)
   * `-Djna.library.path=/usr/local/lib`

## How To

 * [how to replace authenticator](https://github.com/umjammer/vavi-apps-fuse/wiki/HowToReplaceAuthenticator)
 * [OCR using google drive](https://github.com/umjammer/vavi-apps-fuse/blob/ade22cec00d1ca9a3ade45cf4061228a032e4a32/vavi-nio-file-sandbox/src/test/java/GoogleOCR.java)
 * [remove older revisions on google drive](https://github.com/umjammer/vavi-apps-fuse/blob/9608a560f014d515ad95b45de0264dbe3f7c1d62/vavi-nio-file-googledrive/src/test/java/vavi/nio/file/googledrive/Main7.java)
 * [write a description to a file on google drive or onedrive](https://github.com/umjammer/vavi-apps-fuse/blob/9608a560f014d515ad95b45de0264dbe3f7c1d62/vavi-nio-file-sandbox/src/test/java/Descriptor.java)

## References

 * https://github.com/umjammer/vavi-apps-fuse/wiki/Libraries
 * https://github.com/googleapis/java-storage-nio (official !!!)
   * retry handler
   * option
   * SeekableByteChannel
   * how about upload?
 * https://github.com/unsound/jfuse (jni)
 * https://github.com/uw-dims/fuse4j

## TODO

 * ~~amazon~~ (~~only ios and android are supported now~~ to be closed)
 * ~~adrive~~ (i was banned)
 * ~~flickr~~ (quit 1T service)
 * ~~apache-commons-vfs~~ (done)
 * ~~vavi-util-archive~~ (done)
 * ~~shutdownHook~~
 * ~~https://github.com/unsound/hfsexplorer~~ (done)
 * ~~virtualbox vdi~~ ([done](https://github.com/umjammer/vavi-nio-file-discutils))
 * ~~mincraft nbt~~ (deal in [tree view](https://github.com/umjammer/vavi-apps-treeview))
 * ~~credential from uri~~
 * ~~https://github.com/cryptomator/fuse-nio-adapter~~
 * watch
   * [base](https://github.com/umjammer/vavi-nio-file-base) 
   * [webhook](https://github.com/umjammer/vavi-apps-webhook)  
 * https://github.com/mucommander/mucommander
 * trash,recent
 * ~~separate vavi-nio-file-commons and merge into vavi-nio-file-base~~
 * ~~separate fuse part (use obsoleted vavi-nio-file -> vavi-nio-file-fuse)~~
   * rename project like vavi-nio-file-collection
