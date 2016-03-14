
package vavi.net.fuse.dropbox;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.DbxWebAuthNoRedirect;

import vavi.net.fuse.Getter;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * An example command-line application that runs through the web-based OAuth
 * flow (using {@link DbxWebAuth}).
 */
@PropsEntity(url = "classpath:dropbox.properties")
public class Main2 {

    @Property(name = "dropbox.clientId")
    private String clientId;
    @Property(name = "dropbox.clientSecret")
    private String clientSecret;

    /**
     * @param args 0: email
     */
    public static void main(String[] args) throws Exception {
        String email = args[0];

        // Only display important log messages.
        Logger.getLogger(Main2.class.getName()).setLevel(Level.WARNING);
        
        Main2 app = new Main2();
        PropsEntity.Util.bind(app);

        // Read app info file (contains app key and app secret)
        DbxAppInfo appInfo = new DbxAppInfo(app.clientId, app.clientSecret);

        // Run through Dropbox API authorization process
        String userLocale = Locale.getDefault().toString();
        DbxRequestConfig requestConfig = new DbxRequestConfig("vavi-apps-fuse", userLocale);
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(requestConfig, appInfo);

        String authorizeUrl = webAuth.start();
        
        Getter getter = new DropBoxFxGetter(email);
        String code = getter.get(authorizeUrl);

        DbxAuthFinish authFinish = webAuth.finish(code);

        System.out.println("Authorization complete.");
        System.out.println("- User ID: " + authFinish.getUserId());
        System.out.println("- Access Token: " + authFinish.getAccessToken());

        // Save auth information to output file.
        DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.getAccessToken(), appInfo.getHost());
        DbxAuthInfo.Writer.writeToStream(authInfo, System.err, true);
    }
}
