package ch.sebpiller.epg.scrapper.playtvfr;

import ch.sebpiller.epg.Audience;
import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrape information using https://www.ocs.fr/grille-tv.
 *
 * @deprecated PlayTvFr is very unstable and throws 503 errors very often.
 */
@Deprecated
public class PlayTvFrEpgScrapper implements EpgScrapper {
    private static final Logger LOG = LoggerFactory.getLogger(PlayTvFrEpgScrapper.class);

    private static final Map<Channel, String> MAP;
    /**
     * The format to insert day to fetch in the web url.
     */
    private static final DateTimeFormatter DAYSTR_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    // https://www.programme-tv.net/programme/chaine/2021-01-14/programme-mangas-82.html
    private static final String R_URL = "https://playtv.fr";
    private static final String ROOT_URL = R_URL + "/programmes-tv";

    private static final Pattern EPISODE_PATTERN = Pattern.compile("Saison (\\d+) - Épisode (\\d+)");

    private static final Pattern OG_URL_PATTERN = Pattern.compile("^https://playtv.fr/programmes-tv/([A-Za-z\\-]+)/(\\d{2}-\\d{2}-\\d{4})/.*$");

    static {
        Map<Channel, String> x = new EnumMap<>(Channel.class);
        x.put(Channel.ROUGETV, "rouge-tv");
        MAP = Collections.unmodifiableMap(x);
    }

    private final Map<Channel, EpgInfo> lastEpgs = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void scrapeEpg(Predicate<Channel> filterChannel, EpgInfoScrappedListener listener) {
        this.lastEpgs.clear();
        Document doc;

        for (Map.Entry<Channel, String> c : MAP.entrySet()) {
            boolean hasMoreData = true;
            LocalDate dayFetch = LocalDate.now();
            if (!filterChannel.test(c.getKey())) {
                continue;
            }

            do {
                String url = ROOT_URL + '/' + c.getValue() + '/' + DAYSTR_FORMAT.format(dayFetch) + '/';
                try {
                    // fetch data for current channel
                    doc = Jsoup.connect(url).get();
                    //System.out.println(doc);

                    scrapeDocument(doc, listener);
                    dayFetch = dayFetch.plusDays(1); // tomorrow
                } catch (HttpStatusException e) {
                    if (e.getStatusCode() == 404) {
                        // epg for this day is not available yet
                        hasMoreData = false;
                    } else if (e.getStatusCode() == 503) {
                        // sometimes throws 503 - Service unavailable
                        LOG.warn("URL '{}' resulted in a '503 - Service unavailable error'. " +
                                "Skipping the whole scrapper", url);
                        hasMoreData = false;
                    } else {
                        // fatal
                        throw new RuntimeException(e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (LOG.isTraceEnabled())
                    LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));
            } while (hasMoreData);
        }

        if (LOG.isInfoEnabled())
            LOG.info("scrapper {} has completed", this);
    }

    void scrapeDocument(Document doc, EpgInfoScrappedListener listener) {
        // <meta property="og:url" content="https://playtv.fr/programmes-tv/rouge-tv/11-01-2021/">
        Elements ogUrl = doc.getElementsByAttributeValue("property", "og:url");
        String uri = ogUrl.attr("content");
        Matcher m = OG_URL_PATTERN.matcher(uri);

        if (!m.matches()) {
            throw new RuntimeException("can not find date of document");
        }

        LocalDate date = LocalDate.from(DAYSTR_FORMAT.parse(m.group(2)));

        LocalTime lastTime = null;
        LocalDate ld = date;

        String text = m.group(1);
        Channel c = MAP.entrySet().stream().filter(e -> e.getValue().equals(text)).findFirst().get().getKey();

        boolean ignoreThis = true;
        for (Element a : doc.select("#programs-next-list .program")) {
            String from = a.selectFirst(".program-start").text();
            String[] split = from.split(":");
            LocalTime time = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

            if (lastTime != null && time.isBefore(lastTime)) {
                ignoreThis = false;
            }
            lastTime = time;
            if (ignoreThis) {
                continue;
            }

            EpgInfo info = new EpgInfo();
            info.setChannel(c);

            ZonedDateTime zdt = ZonedDateTime.of(ld, time, ZoneId.of("Europe/Zurich"));
            info.setTimeStart(zdt);
            Element title = a.selectFirst(".program-title a");
            String title1 = title.attr("title");

            String subtitle = "";
            for (Element e : title.nextElementSiblings()) {
                if (!"-".equals(e.text())) {
                    subtitle = subtitle + e.text() + "\n";
                }
            }

            if (StringUtils.isNotBlank(subtitle)) {
                info.setSubtitle(subtitle);
            }

            Pattern p = Pattern.compile("^(.*) épisode (\\d+)$");
            Matcher mm = p.matcher(title1);
            if (mm.matches()) {
                info.setTitle(mm.group(1));
                info.setEpisode('E' + mm.group(2));
            } else {
                info.setTitle(title1);
            }

            // <meta itemprop="description"
            info.setDescription(a.getElementsByAttributeValue("itemprop", "description").attr("content"));


            info.setCategory(resolveCategory(a.select(".program-gender").text()));

            // info.setSubtitle(a.selectFirst(".singleBroadcastCard-subtitle").text());
            // info.setCategory(resolveCategory(a.selectFirst(".singleBroadcastCard-genre").text()));

            parseDetails(R_URL + title.attr("href"), info);

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


    void parseDetails(String uri, EpgInfo info) {
        try {
            parseDetails(Jsoup.connect(uri).get(), info);
        } catch (HttpStatusException e) {
            // ignore 403 errors
            if (e.getStatusCode() == 403) {
                return;
            } else if (e.getStatusCode() == 404) {
                LOG.warn("not found {}", uri, e);
                return;
            }

            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void parseDetails(Document doc, EpgInfo info) {
        //info.setDescription(doc.select(".synopsis-text").text());
        //programOverviewHeader-title

        for (Element e : doc.select("ul.program-casting li")) {
            Set<String> strings = new HashSet<>();
            for (Element items : e.select("ul.program-casting-casts li a")) {
                strings.add(items.attr("title"));
            }

            String text = e.select(".program-casting-status").text();
            if ("Acteurs".equals(text)) {
                info.setActors(new ArrayList<>(strings));
            } else if ("Réalisateur".equals(text)) {
                info.setDirectors(new ArrayList<>(strings));
            } else if ("Créateur".equals(text)) {
                info.setProducers(new ArrayList<>(strings));
            }
        }

        Element csa = doc.selectFirst("p.pmd-ProgramCsa");
        if (csa != null) {
            if (!csa.getElementsByClass("pmd-Svg--minus18").isEmpty()) {
                info.setAudience(Audience.EIGHTEEN);
            } else if (!csa.getElementsByClass("pmd-Svg--minus16").isEmpty()) {
                info.setAudience(Audience.SIXTEEN);
            } else if (!csa.getElementsByClass("pmd-Svg--minus12").isEmpty()) {
                info.setAudience(Audience.TWELVE);
            } else {
                LOG.warn("can not understand csa\n{}", csa);
            }
        }
    }

    private String resolveCategory(String text) {
        // FIXME improve mapping of category between rts and xmltv
        switch (text.toLowerCase()) {
            case "film":
            case "téléfilm":
            case "film tv":
                return "Movie / Drama";
            case "x":
                return "News / Current affairs";
            case "série":
            case "feuilleton":
            case "court-métrage":
                return "Show / Games";
            case "sport":
            case "sports":
                return "Sports";
            case "xxxxx":
                return "Children's / Youth";
            case "xxx":
                return "Music";
            case "yyx":
                return "Art / Culture";
            case "h":
                return "Social / Political issues / Economics";
            case "documentaire":
                return "Education / Science / Factual topics";
            case "magazine":
                return "Leisure hobbies";
            case "k":
                return "Special characteristics";
            default:
                return text;
        }
    }
}
