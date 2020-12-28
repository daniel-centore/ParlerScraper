package com.danielcentore.scraper.parler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.UIManager;

import com.danielcentore.scraper.parler.api.ICookiesListener;
import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.db.ScraperDb;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;

public class Main implements ICookiesListener {

    public static final File CREDENTIALS = new File("./credentials.txt");
    public static final String TAB = "    ";

    ParlerScraperGui gui;
    ScraperDb scraperDb;
    ParlerClient client;

    public Main() {
        String mst = "";
        String jst = "";
        try {
            Scanner scan = new Scanner(CREDENTIALS);
            mst = scan.nextLine();
            jst = scan.nextLine();
            scan.close();
        } catch (FileNotFoundException e1) {
        }

        scraperDb = new ScraperDb();

        client = new ParlerClient(mst, jst);
        client.addCookieListener(this);
        
        gui = new ParlerScraperGui(this, mst, jst);
        gui.setVisible(true);
    }

    @Override
    public void cookiesUpdated(String mst, String jst) {
        gui.println("Cookies updated");
        gui.println(TAB + "mst: " + mst);
        gui.println(TAB + "jst: " + jst);

        gui.setCookies(mst, jst);

        try {
            FileWriter fileWriter = new FileWriter(CREDENTIALS, false);
            fileWriter.write(mst + "\n" + jst);
            fileWriter.close();
        } catch (IOException e) {
            gui.println("Error writing cookies to file: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void resumePostScrapeBtn(String mst, String jst, String startDate, String endDate, String seeds) {
        cookiesUpdated(mst, jst);
        gui.setRunning(true);
    }

    public void stopPostScrapeBtn() {
        gui.setRunning(false);
    }

    public void idkBtn() {
        //        for (int i = 0;; i++) {
        //            gui.println("Text " + i);
        //            try {
        //                Thread.sleep(100);
        //            } catch (InterruptedException e) {
        //                // TODO Auto-generated catch block
        //                e.printStackTrace();
        //            }
        //        }

        gui.println("Fetching Tuck");
        ParlerUser profile = client.fetchProfile("TuckerCarlson");
        gui.println("Storing Tuck");
        scraperDb.storeUser(profile);
        gui.println("Done with Tuck");

        //  System.out.println(profile);
        //  PagedParlerUsers following = client.fetchFollowing(profile);
        //  System.out.println(following);
        //  PagedParlerUsers followers = client.fetchFollowers(profile);
        //  System.out.println(followers);
        //  
        //  scraperDb.storePagedUsers(following);
        //  scraperDb.storePagedUsers(followers);
        //  
        //  System.out.println(scraperDb.getAllNotWorthlessUsers());

        //  PagedParlerPosts posts = client.fetchPagedPosts(profile);
        //  scraperDb.storePagedPosts(posts);
        //  System.out.println(posts);
        //        PagedParlerPosts hastagPosts = client.fetchPagedHashtag("maga");
        //        scraperDb.storePagedPosts(hastagPosts);
        //  System.out.println(hastagPosts);
    }

    public static void main(String[] args) throws FileNotFoundException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        new Main();
    }

}
