package ch.sebpiller.epg.scrapper.telecablesatfr;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import org.jsoup.HttpStatusException;
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
 * Scrape information from https://tv-programme.telecablesat.fr.
 *
 * @deprecated They are protected against scrapping process with something like CloudFlare I guess...
 */
// https://tv-programme.telecablesat.fr/chaine/822/rouge-tv.html?date=2021-02-02&period=afternoon
@Deprecated
public class TelecableSatFrScrapper implements EpgScrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TelecableSatFrScrapper.class);

    private static final String ROOT_URL = "https://tv-programme.telecablesat.fr/chaine";

    private static final String PERIOD_1 = "morning", PERIOD_2 = "noon", PERIOD_3 = "afternoon";

    private static final String[] PERIODS = {PERIOD_1, PERIOD_2, PERIOD_3};

    /**
     * The format to insert day to fetch in the web url.
     */
    private static final DateTimeFormatter DAYSTR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<Channel, String> MAP;


    static {
        Map<Channel, String> x = new EnumMap<>(Channel.class);
        x.put(Channel.ROUGETV, "822/rouge-tv");
        MAP = Collections.unmodifiableMap(x);
    }

    private final Map<Channel, EpgInfo> lastEpgs = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void scrapeEpg(Predicate<Channel> filterChannel, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {
        this.lastEpgs.clear();
        Document doc;

        int i = 0;
        for (Map.Entry<Channel, String> c : MAP.entrySet()) {
            boolean hasMoreData = true;
            LocalDate dayFetch = LocalDate.now();
            if (!filterChannel.test(c.getKey())) {
                continue;
            }

            do {
                for (String period : PERIODS) {
                    String url = ROOT_URL + '/' + c.getValue() + ".html?date=" + DAYSTR_FORMAT.format(dayFetch) + "&period=" + period;
                    try {
                        // fetch data for current channel
                        doc = Jsoup.connect(url).get();

                        scrapeDocument(doc, scrapeDetails, listener);
                    } catch (HttpStatusException e) {
                        if (e.getStatusCode() == 404) {
                            // epg for this day is not available yet
                            hasMoreData = false;
                        } else {
                            // fatal
                            throw new RuntimeException(e);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                dayFetch = dayFetch.plusDays(1); // tomorrow

                if (LOG.isTraceEnabled())
                    LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));

                // prevent 503 from the site when query comes too quickly
                if (++i % 1 == 0) {
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } while (hasMoreData);
        }

        if (LOG.isInfoEnabled())
            LOG.info("scrapper {} has completed", this);
    }

    void scrapeDocument(Document doc, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {
        Matcher m;
        // <link rel="canonical" href="https://tv-programme.telecablesat.fr/chaine/822/rouge-tv.html?date=2021-02-03&period=morning">
        String uri = doc.getElementsByAttributeValue("rel", "canonical").attr("href");

        ////
        // extract LocalDate rel canonical
        String DATE_EXTRACTOR = "^.*=(\\d{4}-\\d{2}-\\d{2})&.*$";
        m = Pattern.compile(DATE_EXTRACTOR).matcher(uri);
        if (!m.matches()) {
            // fallback to parse from ul.nav > li:not(.active) tab in case it doesn't exists in the canonical link

            Element li = doc.selectFirst("div.tabbable ul.nav li:not(.active)");

            // FIXME it seems they have implemented some sort of scrappers-protection...
            if (li == null) {
                LOG.warn("skipped bad document. Cloudflare protected?");
                return;
            }

            String href = li.select("a").attr("href");
            m = Pattern.compile(DATE_EXTRACTOR).matcher(href);
            if (!m.matches()) {
                throw new RuntimeException("can not find date of document");
            }
        }

        LOG.info("OK dude!");

        LocalDate date = LocalDate.from(DAYSTR_FORMAT.parse(m.group(1)));


        ////
        m = Pattern.compile("^.*/(.*)\\.html.*$").matcher(uri);
        if (!m.matches()) {
            throw new RuntimeException("can not find channel of document");
        }
        String x = m.group(1);
        Channel c = Channel.valueOfAliases(x);
        if (c == null) {
            throw new RuntimeException("can not find channel " + x);
        }


        LocalTime lastTime = null;
        LocalDate ld = date;

        // div class="news"
        for (Element news : doc.select("div.news")) {
            /*
            <div class="news">
                <!-- Begin .col-md-1 -->
                <div class="col-xs-12 col-sm-1 col-md-1">
                    <div class="schedule-hour">10:20</div>
                </div>
                <!-- Begin .col-md-11 -->
                <div class="col-xs-12 col-sm-11 col-md-11">
                    <div class="item">
                        <div class="item-image-2">
                            <div data-diffusion="965129901"
                                 class="img-link"
                                 href="/emission/au-rythme-de-la-vie-134512958.html">
                                <img class="img-responsive img-full"
                                     src="//tv.cdnartwhere.eu/cache/i/eyJiYXNlIjoiaHR0cHM6XC9cL21wLXBob3Rvcy1jZG4uYXp1cmVlZGdlLm5ldFwvY29udGFpbmVyNWY4YzExY2FkZjljNDEwOThlN2YxMTFiNzc4YWIzMzlcL2IwMjRkYTAzYjliYjRiYjlkMzA3M2Y3ZDRkZWFkODc4NDQ3MzZkMTMuanBnIiwibWFzayI6IjY2N3gzODUifQ.jpg"
                                     alt="Au rythme de la vie">
                            </div>
                        </div>
                        <div class="item-content">
                            <div class="title-left title-style04 underline04">
                                <h3>
                                    <a data-diffusion="965129901"
                                       href="/emission/au-rythme-de-la-vie-134512958.html"><strong>Au
                                        rythme de la
                                        vie</strong></a></h3>
                            </div>
                            <p>Les vies des habitants d'un
                                quartier de Berlin connaissent
                                des hauts et des bas, des drames
                                et des bonheurs, des espoirs
                                fous et des déceptions
                                violentes.</p>
                        </div>
                    </div>
                </div>
            </div>
            */
            String hour = news.selectFirst("div.schedule-hour").text();
            String title = news.select(".item-content h3").text();
            String subTitle = news.select(".item-content p").text();

            String[] split = hour.split("\\:");

            LocalTime time = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

            if (lastTime != null && time.isBefore(lastTime)) {
                ld = ld.plusDays(1);
            }
            lastTime = time;

            ZonedDateTime zdt = ZonedDateTime.of(ld, time, ZoneId.of("Europe/Zurich"));


            EpgInfo info = new EpgInfo();
            info.setTimeStart(zdt);
            info.setChannel(c);
            info.setTitle(title);
            info.setSubtitle(subTitle);

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
