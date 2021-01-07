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
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of a scrapper that gets the data from the RTS website.
 */
public class RtsEpgScrapper implements EpgScrapper {
    private static final Logger LOG = LoggerFactory.getLogger(RtsEpgScrapper.class);
    /**
     * The base url to scrape data from.
     */
    private static final String ROOT_URL = "https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/schedules";

    /**
     * The uri to scrape details from.
     */
    private static final String DETAILS_URL = "https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/medias";

    /**
     * The number of channels available on this endpoint.
     */
    private static final int CHANNELS_TO_FETCH = 48;

    /**
     * The site groups the data in pages containing at most this number of channels.
     */
    private static final int CHANNELS_PAGE_SIZE = 12;

    /**
     * The format to parse date with time from RTS.
     */
    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     * The format to insert day to fetch in the web url.
     */
    private static final DateTimeFormatter DAYSTR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * The pattern of the text identifying an episode in RTS. Eg. "Saison 7 (12/22)".
     */
    static final String EPISODE_PATTERN = "^Saison (\\d+) \\((\\d+)/\\d+\\)$";

    /**
     * Remember the last epg info for all channels, so we can determine the stop time of a show.
     */
    private final Map<Channel, EpgInfo> lastEpg = Collections.synchronizedMap(new HashMap<>());

    // count the number of timeout received during the scrape process. At each timeout, we wait 10s to give the remote
    // site a chance to recover. Once reached a threshold, we abort the process with the original exception.
    private int timeoutCount;

    @Override
    public void scrapeEpg(EpgInfoScrappedListener listener) {
        timeoutCount = 0;
        lastEpg.clear();

        LocalDate dayFetch = LocalDate.now();

        boolean hasMoreData = true;
        do { // loop through days until a 404 error is reported
            // get the data of all channels for the current day.
            for (int channelsPage = 0; (channelsPage * CHANNELS_PAGE_SIZE) < CHANNELS_TO_FETCH; channelsPage++) {
                Document doc = null;

                boolean ok = false;
                do {
                    try {
                        doc = Jsoup.connect(
                                ROOT_URL +
                                        "?dayStr=" + DAYSTR_FORMAT.format(dayFetch) +
                                        "&limit=" + CHANNELS_PAGE_SIZE +
                                        "&offset=" + (channelsPage * CHANNELS_PAGE_SIZE) +
                                        "&filter=none"
                        ).get();
                        ok = true;
                    } catch (HttpStatusException e) {
                        // only 404 are considered as end of stream, others are errors to be reported.
                        if (e.getStatusCode() != 404) {
                            throw new RuntimeException(e);
                        }

                        // no more data, bye bye
                        hasMoreData = false;
                        break;
                    } catch (SocketTimeoutException ste) {
                        handleTimeout(ste);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } while (!ok);

                LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));
                if (doc != null) {
                    scrapeDocument(doc, listener);
                }
            }

            dayFetch = dayFetch.plusDays(1); // tomorrow
        } while (hasMoreData);
    }

    /**
     * Scrape the given document and send results to the listener. The document is expected to contain an html dom
     * with a node "section" assigned classes "schedules" and "day".
     */
    void scrapeDocument(Document doc, EpgInfoScrappedListener listener) {
        Elements epg = doc.select("section.schedules.day");
        if (epg.size() != 1) {
            throw new IllegalArgumentException("invalid document: no unique 'section' with classes 'schedules' and 'day' !");
        }

        Pattern patt = Pattern.compile(EPISODE_PATTERN);

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
                String detailUriId = epgElement.attr("data-epg-element-id");
                String title = epgElement.getElementsByTag("h3").get(0).text();
                String time = epgElement.getElementsByTag("time").get(0).attr("datetime");

                String epNumber = null;
                String desc = null;

                for (Element p : epgElement.getElementsByTag("p")) {
                    if (desc == null) {
                        desc = p.text();
                    } else if (epNumber == null) {
                        epNumber = p.text();
                    }
                }

                LocalDateTime start = LocalDateTime.parse(time, TIME_PARSER);
                LOG.trace("{} {}:{} {}", channelName, start.getHour(), start.getMinute(), title);

                // if we have a previous epg info for this channel, determine duration and notify the previous
                EpgInfo lastInfo = lastEpg.get(channel);

                if (lastInfo != null) {
                    lastInfo.setTimeStop(start);
                    listener.epgInfoScrapped(lastInfo);
                }
                ////////////////////////////////////////////

                String episode = null;
                String subtitle = null;

                for (Element p : epgElement.getElementsByTag("p")) {
                    String text = p.text();
                    LOG.trace("    text {}: {}", p.siblingIndex(), text);

                    // If we are looking for an episode
                    if (episode == null) {
                        Matcher m = patt.matcher(text);

                        // episode with complex pattern
                        if (m.matches()) {
                            episode = String.format("S%02dE%02d",
                                    Integer.parseInt(m.group(1)),
                                    Integer.parseInt(m.group(2))
                            );
                        }

                        // episode with number only (eg. Les feux de l'amour)
                        else if (text.matches("\\d+")) {
                            episode = "E" + text;
                        } else {
                            subtitle = text;
                        }
                    } else {
                        subtitle = text;
                    }
                }

                // notify callback
                EpgInfo info = new EpgInfo();
                info.setChannel(channel);
                info.setTimeStart(start);
                info.setTitle(title);
                info.setSubtitle(subtitle);
                info.setEpisode(episode);
                scrapeDetails(detailUriId, info);

                // we can not notify the recipient yet, we need the next entry on the same channel to be able to determine the duration of this entry.
                lastEpg.put(channel, info);
            }
        }
    }

    void scrapeDetails(String detailUriId, EpgInfo info) {
        Document doc;
        try {
            doc = Jsoup.connect(DETAILS_URL + '/' + detailUriId).get();
        } catch (HttpStatusException e) {
            // only 404 are considered non fatal
            if (e.getStatusCode() != 404) {
                throw new RuntimeException(e);
            }

            return;
        } catch (SocketTimeoutException e) {
            handleTimeout(e);
            scrapeDetails(detailUriId, info);
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements elements = doc.getElementsByClass("description");
        if (!elements.isEmpty()) {
            Element e = elements.get(0);
            info.setDescription(e.text());
        }
    }

    private void handleTimeout(Exception re) {
        timeoutCount++;
        LOG.warn("*** timeout occured: {}", timeoutCount);

        if (timeoutCount < 10) {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new RuntimeException(re);
        }
    }
}
