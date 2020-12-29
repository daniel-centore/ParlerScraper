package com.danielcentore.scraper.parler.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.danielcentore.scraper.parler.PUtils;
import com.danielcentore.scraper.parler.api.components.PagedParlerPosts;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

    public static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/87.0.4280.88 Safari/537.36";

    public static String MAIN_DOMAIN = "https://parler.com";
    public static String API_DOMAIN = "https://api.parler.com";

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

    public Response issueRequest(String referrer, String endpoint) {

        int attempt = 0;
        long waitTime = 2000 + random.nextInt(1000);

        while (true) {
            try {
                Response result = issueRequestNoRetry(referrer, endpoint);
                
                gui.println("> Pausing " + waitTime + "ms...");
                PUtils.sleep(waitTime);
                
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                attempt++;
                long retryTime = waitTime * (long) Math.pow(2, Math.min(attempt, 5));
                gui.println("> API REQUEST ATTEMPT " + attempt + " FAILED: " + e.getLocalizedMessage());
                gui.println("> Retrying in " + retryTime + "ms...");
                PUtils.sleep(retryTime);
            }
        }
    }

    private Response issueRequestNoRetry(String referrer, String endpoint) throws IOException {
        OkHttpClient client = new OkHttpClient();

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
        Response response = issueRequest(
                "profile/" + eUsername + "/posts",
                "v1/profile?username=" + eUsername);
        try {
            return mapper.readValue(response.body().byteStream(), ParlerUser.class)
                    .setFullyScanned();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PagedParlerPosts fetchPagedPosts(ParlerUser user) {
        return fetchPagedPosts(user, null);
    }

    public PagedParlerPosts fetchPagedPosts(ParlerUser user, ParlerTime start) {
        Response response = fetchPagedUserResponse("profile/" + user.getUrlEncodedUsername() + "/posts",
                "v1/post/creator", user, start);

        try {
            return mapper.readValue(response.body().byteStream(), PagedParlerPosts.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
        Response response = fetchPagedUserResponse(referrer, "v1/follow/" + endpointBase, user, start);

        try {
            return mapper.readValue(response.body().byteStream(), PagedParlerUsers.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Response fetchPagedUserResponse(String referrer, String endpointBase, ParlerUser user, ParlerTime start) {
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

        Response response = issueRequest("search?hashtag=" + hashtag, endpoint);

        try {
            return mapper.readValue(response.body().byteStream(), PagedParlerPosts.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
