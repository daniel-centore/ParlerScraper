package com.danielcentore.scraper.parler.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.danielcentore.scraper.parler.PUtils;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerMaybeErrorResponse;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

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

    public static final int TIMEOUT_SEC = 20;
    public static final int MAX_ATTEMPTS = 2;

    private static ObjectMapper mapper = new ObjectMapper();
    private static Random random = new Random();

    private List<ICookiesListener> cookiesListeners = new ArrayList<>();

    private String mst;
    private String jst;

    private ParlerScraperGui gui;

    public ParlerClient(String mst, String jst, ParlerScraperGui gui) {
        this.mst = mst;
        this.jst = jst;

        this.gui = gui;
    }

    public String issueRequest(String referrer, String endpoint) {

        int attempt = 0;
        long waitTime = 1500 + random.nextInt(1500);

        // 0.5% of the time we take a coffee break 
        if (random.nextInt(1000) < 5) {
            gui.println("> Pausing extra long this time");
            waitTime = 30000 + random.nextInt(60000);
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
                e.printStackTrace();
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    gui.println("> Giving up :(");
                    return null;
                }
                long retryTime = waitTime * (long) Math.pow(2, attempt);
                gui.println("> API REQUEST ATTEMPT " + attempt + " FAILED: " + e.getLocalizedMessage());
                gui.println("> Endpoint: " + endpoint);
                gui.println("> Retrying in " + retryTime + "ms...");
                PUtils.sleep(retryTime);
                gui.println("> Retrying...");
            }
        }
    }

    private Response issueRequestNoRetry(String referrer, String endpoint) throws IOException {
        OkHttpClient client = new OkHttpClient();
        client.setReadTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);

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

    public ParlerUser fetchProfile(String username) {
        String eUsername = urlencode(username);
        String response = issueRequest(
                "profile/" + eUsername + "/posts",
                "v1/profile?username=" + eUsername);
        try {
            return mapper.readValue(response, ParlerUser.class)
                    .setFullyScanned();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public PagedParlerPosts fetchPagedPosts(ParlerUser user) {
        return fetchPagedPosts(user, null);
    }

    public PagedParlerPosts fetchPagedPosts(ParlerUser user, ParlerTime start) {
        String response = fetchPagedUserResponse("profile/" + user.getUrlEncodedUsername() + "/posts",
                "v1/post/creator", user, start);

        try {
            return mapper.readValue(response, PagedParlerPosts.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public PagedParlerUsers fetchFollowing(ParlerUser user) {
        return fetchFollowing(user, null);
    }

    public PagedParlerUsers fetchFollowing(ParlerUser user, ParlerTime start) {
        String eParlerId = user.getUrlEncodedParlerId();
        return fetchPagedUsers(String.format(
                "profile/%s/following", eParlerId),
                "following",
                user,
                start);
    }

    public PagedParlerUsers fetchFollowers(ParlerUser user) {
        return fetchFollowers(user, null);
    }

    public PagedParlerUsers fetchFollowers(ParlerUser user, ParlerTime start) {
        String eParlerId = user.getUrlEncodedParlerId();
        return fetchPagedUsers(String.format(
                "profile/%s/followers", eParlerId),
                "followers",
                user,
                start);
    }

    public PagedParlerUsers fetchPagedUsers(String referrer, String endpointBase, ParlerUser user, ParlerTime start) {
        String response = fetchPagedUserResponse(referrer, "v1/follow/" + endpointBase, user, start);

        try {
            return mapper.readValue(response, PagedParlerUsers.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String fetchPagedUserResponse(String referrer, String endpointBase, ParlerUser user, ParlerTime start) {
        String eParlerId = user.getUrlEncodedParlerId();

        // The limit seems to be ignored. The web version always sets it to 10 so we do too.
        String endpoint = String.format("%s?id=%s&limit=10", endpointBase, eParlerId);
        if (start != null) {
            endpoint += "&startkey=" + start.toParlerTimestamp();
        }

        return issueRequest(referrer, endpoint);
    }

    public PagedParlerPosts fetchPagedHashtag(String hashtag) {
        return fetchPagedHashtag(hashtag, null);
    }

    public PagedParlerPosts fetchPagedHashtag(String hashtag, ParlerTime start) {
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

        try {
            return mapper.readValue(response, PagedParlerPosts.class);
        } catch (Exception e) {
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

}
