package ch.sebpiller.epgscrapper.tsr;

import ch.sebpiller.epgscrapper.Channel;
import ch.sebpiller.epgscrapper.EpgInfo;
import ch.sebpiller.epgscrapper.EpgInfoScrappedListener;
import ch.sebpiller.epgscrapper.EpgScrapper;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Implementation of a scrapper that gets the data from the RTS website.
 */
public class TsrEpgScrapper implements EpgScrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TsrEpgScrapper.class);
    /**
     * The base url to scrape data from.
     */
    private static final String ROOT_URL = "https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/schedules";

    /**
     * The number of channels available on this endpoint.
     */
    private static final int CHANNELS_TO_FETCH = 48;

    /**
     * The site groups the data in pages containing at most this number of channels.
     */
    private static final int CHANNELS_PAGE_SIZE = 12;

    @Override
    public void scrapeEpg(EpgInfoScrappedListener listener) {
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final DateTimeFormatter timeParser = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        LocalDate dayFetch = LocalDate.now();
        LOG.trace(dayFetch.format(fmt));

        boolean hasMoreData = true;
        do { // loop through days until a 404 error is reported

            // get the data of all channels for the current day.
            for (int channelsPage = 0; (channelsPage * CHANNELS_PAGE_SIZE) < CHANNELS_TO_FETCH; channelsPage++) {
                Document doc;

                try {
                    doc = Jsoup.connect(
                            ROOT_URL +
                                    "?dayStr=" + fmt.format(dayFetch) +
                                    "&limit=" + CHANNELS_PAGE_SIZE +
                                    "&offset=" + (channelsPage * CHANNELS_PAGE_SIZE) +
                                    "&filter=none"
                    ).get();
                } catch (HttpStatusException e) {
                    // only 404 are considered as end of stream, others are errors to be reported.
                    if (e.getStatusCode() != 404) {
                        throw new RuntimeException(e);
                    }

                    hasMoreData = false;
                    break; // no more data, bye bye
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                LOG.trace("============================= {}", fmt.format(dayFetch));

                Elements epg = doc.select("section.schedules.day");
                if (epg.size() != 1) {
                    throw new IllegalArgumentException("invalid document fetched: no unique 'section' with classes 'schedules' and 'day' !");
                }

                for (Element elChannel : epg.get(0).children()) {
                    LOG.trace("-------");
                    Set<String> cn = elChannel.classNames();
                    cn.remove("ch");

                    String channelName = cn.iterator().next();
                    Channel channel = Channel.valueOfAliases(channelName);
                    if (channel == null) {
                        throw new IllegalArgumentException("unknown channel: " + channelName);
                    }

                    for (Element epgElement : elChannel.getElementsByAttribute("data-epg-element-id")) {
                        String title = epgElement.getElementsByTag("h3").get(0).text();
                        String time = epgElement.getElementsByTag("time").get(0).attr("datetime");

                        String epNumber = null, desc = null;
                        for (Element p : epgElement.getElementsByTag("p")) {
                            if (desc == null) {
                                desc = p.text();
                            } else if (epNumber == null) {
                                epNumber = p.text();
                            }
                        }

                        LocalDateTime ldt = LocalDateTime.parse(time, timeParser);
                        LOG.trace("{} {}:{} {}", channelName, ldt.getHour(), ldt.getMinute(), title);

                        for (Element p : epgElement.getElementsByTag("p")) {
                            LOG.trace("    text {}: {}", p.siblingIndex(), p.text());
                        }

                        // notify callback
                        EpgInfo info = new EpgInfo();
                        info.setChannel(channel);
                        info.setTimeStart(ldt);
                        info.setTitle(title);

                        listener.epgInfoScrapped(info);

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
        } while (hasMoreData);
    }
}
