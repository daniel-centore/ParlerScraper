package com.danielcentore.scraper.parler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.UIManager;

import com.danielcentore.scraper.parler.api.ICookiesListener;
import com.danielcentore.scraper.parler.api.ParlerClient;
import com.danielcentore.scraper.parler.api.ParlerTime;
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
    private volatile Future<?> currentTask;

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
        gui.println("Initializing local database...");

        scraperDb = new ScraperDb(gui);
        scraperDb.updateStatusArea();

        client = new ParlerClient(mst, jst, gui);
        client.addCookieListener(this);

        scraper = new ParlerScraping(scraperDb, client, gui);

        gui.setEnabled(true);

        gui.println("Ready!");
    }

    @Override
    public void cookiesUpdated(String mst, String jst) {
        gui.println("> Cookies updated from server");
        gui.setCookies(mst, jst);
        updateCookiesFile(mst, jst);
    }

    private void updateCookiesFile(String mst, String jst) {
        try {
            FileWriter fileWriter = new FileWriter(CREDENTIALS, false);
            fileWriter.write(mst + "\n" + jst);
            fileWriter.close();
        } catch (IOException e) {
            gui.println("> ERROR writing cookies to file: " + e.getLocalizedMessage());
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
            gui.println("> ERROR: Invalid date format");
            return;
        }

        if (startTime.compareTo(endTime) > 0) {
            ParlerTime temp = startTime;
            startTime = endTime;
            endTime = temp;
        }

        // This brings us to 23:59:59.999 of the same day
        Calendar endCal = endTime.toCalendar();
        endCal.add(Calendar.DAY_OF_MONTH, 1);
        endCal.add(Calendar.MILLISECOND, -1);
        endTime = ParlerTime.fromCalendar(endCal);

        if (endTime.compareTo(ParlerTime.now()) > 0) {
            endTime = ParlerTime.now();
        }

        scraperDb.updateStartEndTime(startTime, endTime);

        final ParlerTime finalStartTime = startTime;
        final ParlerTime finalEndTime = endTime;
        Runnable r = new Runnable() {
            public void run() {
                gui.println("== Scraping: STARTED ==");
                gui.setRunning(true);

                try {
                    scraper.scrape(finalStartTime, finalEndTime, getSeeds(seeds));
                } catch (InterruptedIOException e) {
                    // Stop button pressed
                } catch (Exception e) {
                    gui.println("ERROR: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                gui.println("== Scraping: STOPPED ==");
                gui.setRunning(false);
            }
        };
        currentTask = executor.submit(r);
    }

    private List<String> getSeeds(String seeds) {
        String[] split = seeds.split("\n");
        List<String> result = new ArrayList<>();
        for (String s : split) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public void stopPostScrapeBtn() {
        gui.println("> Finishing up, one moment...");
        currentTask.cancel(true);
    }

    public static void main(String[] args) throws FileNotFoundException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        new Main();
    }

}
