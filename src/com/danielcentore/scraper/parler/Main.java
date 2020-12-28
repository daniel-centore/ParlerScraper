package com.danielcentore.scraper.parler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.UIManager;

import com.danielcentore.scraper.parler.api.ICookiesListener;
import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.ParlerTime;
import com.danielcentore.scraper.parler.api.components.PagedParlerUsers;
import com.danielcentore.scraper.parler.api.components.ParlerUser;
import com.danielcentore.scraper.parler.db.ScraperDb;
import com.danielcentore.scraper.parler.gui.ParlerScraperGui;

public class Main implements ICookiesListener {

    public static final File CREDENTIALS = new File("./credentials.txt");
    public static final String TAB = "    ";

    ExecutorService executor = Executors.newCachedThreadPool();
    ParlerScraperGui gui;
    ScraperDb scraperDb;
    ParlerClient client;
    ParlerScraping scraper;

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

        gui = new ParlerScraperGui(this, mst, jst);
        gui.setEnabled(false);
        gui.setVisible(true);
        gui.println("Initializing database...");

        scraperDb = new ScraperDb(gui);

        client = new ParlerClient(mst, jst);
        client.addCookieListener(this);

        scraper = new ParlerScraping(scraperDb, client, gui);

        gui.setEnabled(true);

        gui.println("Ready!");
    }

    @Override
    public void cookiesUpdated(String mst, String jst) {
        gui.println("Cookies updated from server");
        gui.setCookies(mst, jst);
        updateCookiesFile(mst, jst);
    }

    private void updateCookiesFile(String mst, String jst) {
        try {
            FileWriter fileWriter = new FileWriter(CREDENTIALS, false);
            fileWriter.write(mst + "\n" + jst);
            fileWriter.close();
        } catch (IOException e) {
            gui.println("ERROR writing cookies to file: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void resumePostScrapeBtn(String mst, String jst, String startDate, String endDate, String seeds) {
        updateCookiesFile(mst, jst);

        startDate = startDate.trim();
        endDate = endDate.trim();

        ParlerTime startTime = ParlerTime.fromYyyyMmDd(startDate);
        ParlerTime endTime = ParlerTime.fromYyyyMmDd(endDate);

        if (startTime == null || endTime == null) {
            gui.println("ERROR: Invalid date format");
            return;
        }

        if (startTime.compareTo(endTime) > 0) {
            ParlerTime temp = startTime;
            startTime = endTime;
            endTime = temp;
        }

        // Subtract a day from the start time to make this an inclusive bound
        Calendar startCal = startTime.toCalendar();
        startCal.add(Calendar.DAY_OF_MONTH, -1);
        startTime = ParlerTime.fromCalendar(startCal);

        final ParlerTime finalStartTime = startTime;
        final ParlerTime finalEndTime = endTime;
        Runnable r = new Runnable() {
            public void run() {
                gui.println("== Starting Scraping ==");
                gui.setRunning(true);

                scraper.scrape(finalStartTime, finalEndTime);

                gui.println("== Scraping Stopped ==");
                gui.setRunning(false);
            }
        };
        executor.submit(r);
    }

    public void stopPostScrapeBtn() {
        gui.println("Finishing up, one moment...");
        scraper.stop();
    }

    public static void main(String[] args) throws FileNotFoundException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        new Main();
    }

}