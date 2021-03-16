package ch.sebpiller.epg.scrapper.tvsearchch;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a scrapper that fetches its data from the site tv.search.ch.
 */
public class TvSearchCh implements EpgScrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TvSearchCh.class);
    private static final String ROOT_URL = "https://tv.search.ch/%s?pages=9";

    private static final Map<Channel, String> CHANNELS;

    static {
        EnumMap<Channel, String> c = new EnumMap<>(Channel.class);
        c.put(Channel.ROUGETV, "rougetv");
        CHANNELS = Collections.unmodifiableMap(c);
    }

    private final Map<Channel, EpgInfo> lastEpgs = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void scrapeEpg(Predicate<Channel> filterChannel, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {
        this.lastEpgs.clear();
        Document doc;

        int i = 0;
        for (Map.Entry<Channel, String> c : CHANNELS.entrySet()) {
            boolean hasMoreData = true;

            LocalDate dayFetch = LocalDate.now();
            if (!filterChannel.test(c.getKey())) {
                continue;
            }

            do {
                String url = String.format(ROOT_URL, c.getValue());
                hasMoreData = false;

                try {
                    doc = Jsoup.connect(url).get();
                    scrapeDocument(doc, scrapeDetails, listener, c.getKey());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                dayFetch = dayFetch.plusDays(1); // tomorrow

//                if (LOG.isTraceEnabled())
//                    LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));
            } while (hasMoreData);
        }

        if (LOG.isInfoEnabled())
            LOG.info("scrapper {} has completed", this);
    }

    void scrapeDocument(Document doc, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener, Channel c) {
        Pattern extractDate = Pattern.compile("^.*(\\d{4}-\\d{2}-\\d{2})T.*$");

        for (Element li : doc.select("ol.tv-shows.tv-search-results li")) {
            Element a = li.selectFirst("div.tv-show a.tv-show-link");
            String href = a.attr("href");

            Matcher matcher = extractDate.matcher(href);
            if (!matcher.matches()) {
                throw new RuntimeException("unable to extract the date");
            }

            String date = matcher.group(1);
            LocalDate d = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            String s = li.selectFirst("span.tv-show-time").text();
            String[] split = s.split("\\:");
            LocalTime time = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            ZonedDateTime zdt = ZonedDateTime.of(d, time, ZoneId.of("Europe/Zurich"));

            String title = li.selectFirst("span.tv-show-title").text();

            // notice the css query syntax: "... + dd" which means "take the next tag following the selector"
            String category = li.selectFirst("div.tv-tooltip dt.tv-detail-catname + dd").text();
            Element descriptionEl = li.selectFirst("div.tv-tooltip dt.tv-detail-description + dd");
            String description = descriptionEl == null ? null : descriptionEl.text();

            EpgInfo info = new EpgInfo();
            info.setChannel(c);
            info.setTimeStart(zdt);
            info.setTitle(title);
            info.setCategory(category);
            info.setDescription(description);

            this.lastEpgs.computeIfPresent(c, (channel, epgInfo) -> {
                epgInfo.setTimeStop(zdt);
                if (!listener.epgInfoScrapped(epgInfo)) {
                    // FIXME support 'abort the loop'
                }
                return epgInfo;
            });

            this.lastEpgs.put(c, info);
        }
    }

}
