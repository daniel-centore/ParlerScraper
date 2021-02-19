package com.danielcentore.scraper.parler.api;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.danielcentore.scraper.parler.PUtils;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerMaybeErrorResponse;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Handles communication with the Parler API
 *
 * @author Daniel Centore
 */
public class ParlerClient {

    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/87.0.4280.88 Safari/537.36";

    public static final String MAIN_DOMAIN = "https://parler.com";
    public static final String API_DOMAIN = "https://api.parler.com";

    public static final int TIMEOUT_SEC = 35;
    public static final int MAX_ATTEMPTS = 4;

    private static ObjectMapper mapper = new ObjectMapper();
    private static Random random = new Random();

    private List<ICookiesListener> cookiesListeners = new ArrayList<>();

    private String mst;
    private String jst;

    private ParlerScraperGui gui;

    private long lastRequest = 0;

    private volatile boolean stopRequested = false;

    public ParlerClient(String mst, String jst, ParlerScraperGui gui) {
        this.mst = mst;
        this.jst = jst;

        this.gui = gui;
    }

    public String issueRequest(String referrer, String endpoint) throws InterruptedIOException {
        stopRequested = false;

        int attempt = 0;
        long waitTime = 500 + random.nextInt(1500);
        if (lastRequest > 0) {
            waitTime -= System.currentTimeMillis() - lastRequest;
            lastRequest = System.currentTimeMillis();
            waitTime = Math.max(waitTime, 1);
        }

        while (true) {
            try {
                Response result = issueRequestNoRetry(referrer, endpoint);

                String json = result.body().string();

                String message = mapper.readValue(json, ParlerMaybeErrorResponse.class).getMessage();
                if (message != null) {
                    gui.println("> API MESSAGE: " + message);
                    gui.println("> Full json: " + json);
                    throw new RuntimeException("API Message");
                }

                gui.println("> Pausing " + waitTime + "ms...");
                PUtils.sleep(waitTime);

                return json;
            } catch (Exception e) {
                if (stopRequested) {
                    gui.println("> Stop button pushed; terminating network operation");
                    throw new InterruptedIOException();
                }
                if (!(e instanceof SocketTimeoutException)) {
                    e.printStackTrace();
                }
                attempt++;
                gui.println("> API REQUEST ATTEMPT " + attempt + " FAILED: " + e.getLocalizedMessage());
                gui.println("> Endpoint: " + endpoint);
                if (attempt >= MAX_ATTEMPTS) {
                    gui.println("> Giving up :(");
                    return null;
                }
                long retryTime = waitTime * (long) Math.pow(2, attempt);
                gui.println("> Retrying in " + retryTime + "ms...");
                PUtils.sleep(retryTime);
                gui.println("> Retrying...");
            }
        }
    }

    private Response issueRequestNoRetry(String referrer, String endpoint) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", MAIN_DOMAIN)
                .addHeader("Cookie", String.format("jst=%s; mst=%s", jst, mst))
                .addHeader("Accept-Language", "en-us")
                .addHeader("Referrer", MAIN_DOMAIN + "/" + referrer)
                .url(API_DOMAIN + "/" + endpoint)
                .build();

        Response response = client.newCall(request).execute();

        boolean updated = false;
        for (String header : response.headers("set-cookie")) {
            String[] components = header.split(";");
            String mainComponent = components[0];

            String[] split = mainComponent.split("=");
            String name = split[0].trim();
            String value = split[1].trim();

            if (name.equals("jst") && !this.jst.equals(value)) {
                this.jst = value;
                updated = true;
            } else if (name.equals("mst") && !this.mst.equals(value)) {
                this.mst = value;
                updated = true;
            }
        }

        // Rewrite credentials if necessary
        if (updated) {
            for (ICookiesListener listener : this.cookiesListeners) {
                listener.cookiesUpdated(mst, jst);
            }
        }

        return response;
    }

    public ParlerUser fetchProfile(String username) throws InterruptedIOException {
        String eUsername = urlencode(username);
        String response = issueRequest(
                "profile/" + eUsername + "/posts",
                "v1/profile?username=" + eUsername);
        if (response == null) {
            return null;
        }
        try {
            return mapper.readValue(response, ParlerUser.class)
                    .setFullyScanned();
        } catch (Exception e) {
            gui.println("> Failed to deserialize: " + e.getLocalizedMessage());
            gui.println("> Response: " + response);
            e.printStackTrace();
        }
        return null;
    }

    public PagedParlerPosts fetchPagedPosts(ParlerUser user) throws InterruptedIOException {
        return fetchPagedPosts(user, null);
    }

    public PagedParlerPosts fetchPagedPosts(ParlerUser user, ParlerTime start) throws InterruptedIOException {
        String response = fetchPagedUserResponse("profile/" + user.getUrlEncodedUsername() + "/posts",
                "v1/post/creator", user, start);
        if (response == null) {
            return null;
        }

        try {
            return mapper.readValue(response, PagedParlerPosts.class);
        } catch (Exception e) {
            gui.println("> Failed to deserialize: " + e.getLocalizedMessage());
            gui.println("> Response: " + response);
            e.printStackTrace();
        }

        return null;
    }

    public PagedParlerPosts fetchPagedLikes(ParlerUser user) throws InterruptedIOException {
        return fetchPagedLikes(user, null);
    }

    public PagedParlerPosts fetchPagedLikes(ParlerUser user, ParlerTime start) throws InterruptedIOException {
        String response = fetchPagedUserResponse("profile/" + user.getUrlEncodedUsername() + "/posts",
                "v1/post/creator/liked", user, start);
        if (response == null) {
            return null;
        }

        try {
            return mapper.readValue(response, PagedParlerPosts.class);
        } catch (Exception e) {
            gui.println("> Failed to deserialize: " + e.getLocalizedMessage());
            gui.println("> Response: " + response);
            e.printStackTrace();
        }

        return null;
    }

    public PagedParlerUsers fetchFollowing(ParlerUser user) throws InterruptedIOException {
        return fetchFollowing(user, null);
    }

    public PagedParlerUsers fetchFollowing(ParlerUser user, ParlerTime start) throws InterruptedIOException {
        String eParlerId = user.getUrlEncodedParlerId();
        return fetchPagedUsers(String.format(
                "profile/%s/following", eParlerId),
                "following",
                user,
                start);
    }

    public PagedParlerUsers fetchFollowers(ParlerUser user) throws InterruptedIOException {
        return fetchFollowers(user, null);
    }

    public PagedParlerUsers fetchFollowers(ParlerUser user, ParlerTime start) throws InterruptedIOException {
        String eParlerId = user.getUrlEncodedParlerId();
        return fetchPagedUsers(String.format(
                "profile/%s/followers", eParlerId),
                "followers",
                user,
                start);
    }

    public PagedParlerUsers fetchPagedUsers(String referrer, String endpointBase, ParlerUser user, ParlerTime start)
            throws InterruptedIOException {
        String response = fetchPagedUserResponse(referrer, "v1/follow/" + endpointBase, user, start);
        if (response == null) {
            return null;
        }

        try {
            return mapper.readValue(response, PagedParlerUsers.class);
        } catch (Exception e) {
            gui.println("> Failed to deserialize: " + e.getLocalizedMessage());
            gui.println("> Response: " + response);
            e.printStackTrace();
        }

        return null;
    }

    public String fetchPagedUserResponse(String referrer, String endpointBase, ParlerUser user, ParlerTime start)
            throws InterruptedIOException {
        String eParlerId = user.getUrlEncodedParlerId();

        // The limit seems to be ignored. The web version always sets it to 10 so we do too.
        String endpoint = String.format("%s?id=%s&limit=10", endpointBase, eParlerId);
        if (start != null) {
            endpoint += "&startkey=" + start.toParlerTimestamp();
        }

        return issueRequest(referrer, endpoint);
    }

    public PagedParlerPosts fetchPagedHashtag(String hashtag) throws InterruptedIOException {
        return fetchPagedHashtag(hashtag, null);
    }

    public PagedParlerPosts fetchPagedHashtag(String hashtag, ParlerTime start) throws InterruptedIOException {
        if (hashtag.startsWith("#")) {
            hashtag = hashtag.substring(1);
        }
        String eHashtag = urlencode(hashtag);

        // The limit seems to be ignored. The web version always sets it to 10 so we do too.
        String endpoint = String.format("v1/post/hashtag?tag=%s&limit=10", eHashtag);
        if (start != null) {
            endpoint += "&startkey=" + start.toParlerTimestamp();
        }

        String response = issueRequest("search?hashtag=" + hashtag, endpoint);
        if (response == null) {
            return null;
        }

        try {
            return mapper.readValue(response, PagedParlerPosts.class);
        } catch (Exception e) {
            gui.println("> Failed to deserialize: " + e.getLocalizedMessage());
            gui.println("> Response: " + response);
            e.printStackTrace();
        }
        return null;
    }

    public static String urlencode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void addCookieListener(ICookiesListener listener) {
        cookiesListeners.add(listener);
    }

    public void stop() {
        this.stopRequested = true;
    }

}
