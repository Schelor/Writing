package junit.test;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;

/**
 * @author xiele
 * @date 2018/04/09
 */
public class Functional {

    // "269402143497859", "abe8c3c82f67cf7f32b0187ac28276c7",

    @Test
    public void testFormatString() {
        String urlTemplate = "https://graph.facebook.com/v2.11/%s?fields=id,name&access_token=%s";
        String url = String.format(urlTemplate, "990549324453065", "EAAJyoYNLmicBAFHciUrAZBAbGldYb4eShbwKVnfEhHnMOIgK3Adfc3D146CdUGI2CgqxHr6RG1gZBpflNCsCafL0WFcS3fkuhau70C09RgD9l0Pes2Ba98m6jawUNrTpSOXn3Cgy25Ky11Rde7VuwnXu0ysZAkwCRAnqZAl0GBmhgbQlZCxCWtJqWw8ZALRePRPLqRj0dASAxbTUEoHZCZAv6wHDnrT2g0UZD");
        System.out.println("formatted url: " + url);
    }

    @Test
    public void testUri() throws URISyntaxException {
        URI uri = new URI(String.format("sidecar://%s:%d", "127.0.0.1", 90));
        System.out.println(uri);
    }

    @Test
    public void testFuture() {
        Futures.addCallback(null, new FutureCallback<String>() {
            // we want this handler to run immediately after we push the big red button!
            public void onSuccess(String explosion) {
                //walkAwayFrom(explosion);
            }
            public void onFailure(Throwable thrown) {
                //battleArchNemesis(); // escaped the explosion!
            }
        });
    }
}
