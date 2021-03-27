package ch.sebpiller.epg.scrapper.tsr;

import ch.sebpiller.epg.Audience;
import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import ch.sebpiller.epg.scrapper.ScrappingException;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of a scrapper that gets the data from the RTS website.
 */
/*
 * Implements a basic 'retry-on-timeout' algorithm: each now and then, RTS can not be reached and the Jsoup API dies
 * with a SocketTimeoutException. In such cases, we wait 10 seconds (grace period where we do not send more
 * requests to RTS) and repeat the operation. If we catch more than 10 timeouts during a scrape process, we die with the
 * last timeout exception caught.
 */
public class RtsEpgScrapper implements EpgScrapper {
    /**
     * The pattern of the text identifying an episode in RTS. Eg. "Saison 7 (12/22)".
     */
    static final String EPISODE_PATTERN_STR = "^Saison (\\d+) \\((\\d+)/\\d+\\)$";
    private static final Pattern EPISODE_PATTERN = Pattern.compile(EPISODE_PATTERN_STR);
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
     * Number of timeouts to reach to abort the process.
     */
    private static final int MAX_TIMEOUT = 10;
    /**
     * The format to parse date with time from RTS.
     */
    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    /**
     * The format to insert day to fetch in the web url.
     */
    private static final DateTimeFormatter DAYSTR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * Remember the last epg info for all channels, so we can determine the stop time of that show.
     */
    private final Map<Channel, EpgInfo> lastEpg = Collections.synchronizedMap(new HashMap<>());

    private int timeoutCount;

    @Override
    public void scrapeEpg(Predicate<Channel> filterChannel, EpgInfoScrappedListener listener) {
        this.timeoutCount = 0;
        this.lastEpg.clear();

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
                            throw new ScrappingException(e);
                        }

                        // no more data, bye bye
                        hasMoreData = false;
                        break;
                    } catch (SocketTimeoutException ste) {
                        handleTimeout(ste);
                    } catch (IOException e) {
                        throw new ScrappingException(e);
                    }
                } while (!ok);

                if (LOG.isTraceEnabled())
                    LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));
                if (doc != null) {
                    scrapeDocument(doc, filterChannel, listener);
                }
            }

            dayFetch = dayFetch.plusDays(1); // tomorrow
        } while (hasMoreData);

        LOG.info("scrapper {} has completed", this);
    }

    /**
     * Scrape the given document and send results to the listener. The document is expected to contain an html dom
     * with a node "section" assigned classes "schedules" and "day".
     */
    void scrapeDocument(Document doc, Predicate<Channel> filterChannel, EpgInfoScrappedListener listener) {
        Elements epg = doc.select("section.schedules.day");
        if (epg.size() != 1) {
            throw new IllegalArgumentException("invalid document: no unique 'section' with classes " +
                    "'schedules' and 'day' !");
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
            if (!filterChannel.test(channel)) {
                continue;
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

                ZonedDateTime start = ZonedDateTime.parse(time, TIME_PARSER);
                LOG.trace("{} {}:{} {}", channelName, start.getHour(), start.getMinute(), title);

                // if we have a previous epg info for this channel, determine duration and notify the previous
                EpgInfo lastInfo = this.lastEpg.get(channel);

                if (lastInfo != null) {
                    lastInfo.setTimeStop(start);
                    if (!listener.epgInfoScrapped(lastInfo)) {
                        // asked to stop, we kill the process
                        return;
                    }
                }
                ////////////////////////////////////////////

                String episode = null;
                String subtitle = null;

                for (Element p : epgElement.getElementsByTag("p")) {
                    String text = p.text();
                    LOG.trace("    text {}: {}", p.siblingIndex(), text);

                    // If we are looking for an episode
                    if (episode == null) {
                        Matcher m = EPISODE_PATTERN.matcher(text);

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
                EpgInfo info = new EpgInfo(channel);
                info.setTimeStart(start);
                info.setTitle(title);
                info.setSubtitle(subtitle);
                info.setEpisode(episode);

                parseDetails(detailUriId, info);

                // we can not notify the recipient yet, we need the next entry on the same channel
                // to be able to determine the duration of this entry.
                this.lastEpg.put(channel, info);
            }
        }
    }

    void parseDetails(String detailUriId, EpgInfo info) {
        Document doc;
        try {
            doc = Jsoup.connect(DETAILS_URL + '/' + detailUriId).get();
        } catch (HttpStatusException e) {
            // only 404 are considered non fatal
            if (e.getStatusCode() != 404) {
                throw new ScrappingException(e);
            }

            return;
        } catch (SocketTimeoutException e) {
            handleTimeout(e);
            parseDetails(detailUriId, info);
            return;
        } catch (IOException e) {
            throw new ScrappingException(e);
        }

        parseDetails(info, doc);
    }

    void parseDetails(EpgInfo info, Document details) {
        Elements elements = details.getElementsByClass("description");
        if (!elements.isEmpty()) {
            Element e = elements.get(0);
            info.setDescription(e.text());
        }

        Element cat = details.selectFirst("section.specifications h3 span");
        if (cat != null) {
            String text = resolveCategory(cat.text());
            info.setCategory(text);
        }

        for (Element credit : details.select("section.credits div.credit")) {
            String h3 = credit.getElementsByTag("h3").get(0).text();

            if ("Réalisateur".equalsIgnoreCase(h3)) {
                List<String> directors = new ArrayList<>();
                credit.getElementsByTag("li").forEach(person -> directors.add(person.child(0).text()));
                info.setDirectors(directors);
            } else if ("Créateur".equalsIgnoreCase(h3)) {
                List<String> producers = new ArrayList<>();
                credit.getElementsByTag("li").forEach(person -> producers.add(person.child(0).text()));
                info.setProducers(producers);
            } else if ("Scénario".equalsIgnoreCase(h3)) {
                List<String> scenarists = new ArrayList<>();
                credit.getElementsByTag("li").forEach(person -> scenarists.add(person.child(0).text()));
                info.setScenarists(scenarists);
            } else if ("Acteur".equalsIgnoreCase(h3)) {
                List<String> actors = new ArrayList<>();
                credit.getElementsByTag("li").forEach(person -> actors.add(person.child(0).text()));
                info.setActors(actors);
            }
        }

        if (!details.select("section.glossary span.logo-jaune").isEmpty()) {
            info.setAudience(Audience.TWELVE);
        } else if (!details.select("section.glossary span.logo-rouge").isEmpty()) {
            info.setAudience(Audience.SIXTEEN);
        }
    }

    private String resolveCategory(String text) {
        // FIXME improve mapping of category between rts and xmltv
        switch (text.toLowerCase()) {
            case "fiction":
                return "Movie / Drama";
            case "actualité":
            case "actualite":
                return "News / Current affairs";
            case "divertissement":
                return "Show / Games";
            case "sport":
                return "Sports";
            case "jeunesse":
                return "Children's / Youth";
            case "musique, théâtre, danse":
                return "Music";
            case "magazine":
                return "Art / Culture";
            case "h":
                return "Social / Political issues / Economics";
            case "documentaire":
                return "Education / Science / Factual topics";
            case "x":
                return "Leisure hobbies";
            case "k":
                return "Special characteristics";
            default:
                return text;
        }
    }

    private void handleTimeout(Exception re) {
        this.timeoutCount++;
        LOG.warn("*** timeout occurred: {}", this.timeoutCount);

        if (this.timeoutCount < MAX_TIMEOUT) {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new ScrappingException(re);
        }
    }

}