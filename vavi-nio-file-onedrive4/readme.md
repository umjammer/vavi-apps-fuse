# vavi-nio-file-onedirve4

nio filsystem provider for onedrive

## feature

 * based on the official sdk
 * implements upload w/ monitor that *official sdk doesn't support*
 * OnedriveUploadOption for large file uploading
 * description as "user:attribute"
 * thumbnail as "user:attribute"

## issue

official graph sdk version currently (2022-03-02) is upper 5.15.0.
but i don't use those new versions for this project.
because after `com.microsoft.graph:microsoft-graph:2.0.2`,
the sdk deprecated `com.microsoft.graph.http.IConnection`
and start using `okhttp3.Response` instead of it.
this is not oop. (beginner should not use generics and method reference)
`IConnection` is designed for library independency.
why the official sdk doesn't implement okhttp3 library as `IConnection`?
for performance reason? i don't think so. because
network transportation is very slower than wrapping object.

i tried to adapt v5.x.x to this project. see branch `graph-sdk-5`.
but it was abandoned. i don't want to make any more effort to adapt
this project to the bad api's library.
(it's so hard to adapt lra monitor to official sdk.)

when the official sdk stops supporting v2,
it's a time to die this project.

there are two other onedrive providers.
so it's no problem!

 * [vavi-nio-file-onedrive](../vavi-nio-file-onedrive) (based on [onedrivejavasdk](https://github.com/umjammer/OneDriveJavaSDK))
 * [vavi-nio-file-onedrive3](../vavi-nio-file-onedrive3) (based on [onedrive-java-client](https://github.com/iterate-ch/onedrive-java-client))