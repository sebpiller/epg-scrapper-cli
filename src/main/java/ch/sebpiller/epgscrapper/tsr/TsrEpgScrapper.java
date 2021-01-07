package ch.sebpiller.epgscrapper.tsr;

import ch.sebpiller.epgscrapper.EpgScrapper;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class TsrEpgScrapper implements EpgScrapper {
    //private static final Logger LOG = LoggerF
    /**
     * The number of channels available on this endpoint.
     */
    private static final int CHANNELS_TO_FETCH = 50;

    @Override
    public void scrapeEpg() {
        int channelsPageSize = 12;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate dayFetch = LocalDate.now();
        System.out.println(dayFetch.format(fmt));

        /*
        System.out.println(doc);
        if (true) {
            return;
        }
        */

        boolean errorFetchingData = false;
        do {
            for (int channelsPageIndex = 0; (channelsPageIndex * channelsPageSize) < CHANNELS_TO_FETCH; channelsPageIndex++) {
                Document doc = null;

                try {
                    doc = Jsoup.connect("https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/schedules?dayStr=" + fmt.format(dayFetch) + "&limit=" + channelsPageSize + "&offset=" + (channelsPageIndex * channelsPageSize) + "&filter=none").get();
                } catch (HttpStatusException e) {
                    // only 404 are considered as end of stream
                    if (e.getStatusCode() != 404) {
                        throw new RuntimeException(e);
                    }

                    errorFetchingData = true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("============================= " + fmt.format(dayFetch));

                Elements epg = doc.select("section.schedules.day");
                if (epg.size() != 1) {
                    throw new IllegalArgumentException("invalid document: no unique section with classes 'schedules' and 'day'!");
                }

                for (Element channel : epg.get(0).children()) {
                    System.out.println("-------");
                    Set<String> cn = channel.classNames();
                    cn.remove("ch");
                    String channelName = cn.iterator().next();

                    for (Element epgElement : channel.getElementsByAttribute("data-epg-element-id")) {
                        String title = epgElement.getElementsByTag("h3").get(0).text();

                        //<time datetime="2021-01-06T05:00:00.000+0000">06:00</time>
                        String time = epgElement.getElementsByTag("time").get(0).attr("datetime");

                        String epNumber = null, desc = null;
                        for (Element p : epgElement.getElementsByTag("p")) {
                            if (desc == null) {
                                desc = p.text();
                            } else if (epNumber == null) {
                                epNumber = p.text();
                            }
                        }

                        LocalDateTime ldt = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                        System.out.println(channelName + " " + ldt.getHour() + ":" + ldt.getMinute() + " " + title);

                        for (Element p : epgElement.getElementsByTag("p")) {
                            System.out.println("    text " + p.siblingIndex() + ": " + p.text());
                        }
                        /*
                        if (desc != null) {
                            System.out.println("    title: " + desc);
                        }
                        if (epNumber != null) {
                            System.out.println("    episode: " + epNumber);
                        }*/
                    }
                }
            }

            dayFetch = dayFetch.plusDays(1); // tomorrow
        } while (!errorFetchingData);
    }
}
