package ch.sebpiller.epg.scrapper.ocs;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import org.apache.commons.lang3.StringUtils;
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
import java.util.*;
import java.util.function.Predicate;

/**
 * Scrape information using https://www.ocs.fr/grille-tv.
 */
public class OcsEpgScrapper implements EpgScrapper {
    private static final Logger LOG = LoggerFactory.getLogger(OcsEpgScrapper.class);

    /**
     * The format to insert day to fetch in the web url.
     */
    private static final DateTimeFormatter DAYSTR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 20210116
    private static final String ROOT_URL = "https://grille.ocs.fr/white?embed=&";

    private final Map<Channel, EpgInfo> lastEpgs = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void scrapeEpg(Predicate<Channel> filterChannel, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {
        lastEpgs.clear();
        LocalDate dayFetch = LocalDate.now();
        Document doc = null;
        boolean hasMoreData = true;
        do {
            try {
                doc = Jsoup.connect(ROOT_URL + "date=" + DAYSTR_FORMAT.format(dayFetch)).get();
                hasMoreData = StringUtils.isNotBlank(doc.getElementById("dateInitEpg").attr("value"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));
            if (hasMoreData) {
                scrapeDocument(doc, filterChannel, scrapeDetails, listener);
                dayFetch = dayFetch.plusDays(1); // tomorrow
            }
        } while (hasMoreData);
    }

    void scrapeDocument(Document doc, Predicate<Channel> filterChannel, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {
        // <input type="hidden" id="dateInitEpg" value="20210116"/>
        String attr = doc.getElementById("dateInitEpg").attr("value");
        LocalDate date = LocalDate.from(DAYSTR_FORMAT.parse(attr));

        Element div = doc.selectFirst(".grille-chaines");

        Channel[] channelsIds = new Channel[div.childrenSize()];
        int i = 0;
        for (Element child : div.children()) {
            channelsIds[i++] = Channel.valueOfAliases(child.selectFirst("h2.pageTitle").text());
        }

        Element grilleProgrammes = doc.getElementById("grille-programmes");

        for (int j = 0; j < channelsIds.length; j++) {
            Channel c = channelsIds[j];
            if (!filterChannel.test(c)) {
                continue;
            }

            Element li = grilleProgrammes.child(j);
            LocalTime lastTime = null;
            LocalDate ld = date;

            for (Element a : li.getElementsByClass("linkPageData")) {
                EpgInfo info = new EpgInfo();
                info.setChannel(c);

                String from = a.selectFirst(".program-horaire").text();
                String[] split = from.split("\\:");
                LocalTime time = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

                if (lastTime != null && time.isBefore(lastTime)) {
                    ld = ld.plusDays(1);
                }
                lastTime = time;

                ZonedDateTime zdt = ZonedDateTime.of(ld, time, ZoneId.of("Europe/Zurich"));
                info.setTimeStart(zdt);
                info.setTitle(a.selectFirst(".program-title").text());
                info.setCategory(resolveCategory(a.selectFirst(".program-subtitle span").text()));

                // details are expensive to fetch. Some are filtered out by default.
                String attr1 = null;
                if (scrapeDetails.test(info) && !"#".equals(attr1 = a.attr("data-detaillink"))) {
                    // data-detaillink="https://www.ocs.fr/programme/LAPROIEXXXXW0051426">

                    parseDetails(attr1, info);
                }

                lastEpgs.computeIfPresent(c, (channel, epgInfo) -> {
                    epgInfo.setTimeStop(zdt);
                    if (!listener.epgInfoScrapped(epgInfo)) {
                        // FIXME support 'abort the loop'
                    }
                    return epgInfo;
                });

                lastEpgs.put(c, info);
            }
        }
    }

    private void parseDetails(String uri, EpgInfo info) {
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
        //info.setSubtitle(subtitle);
        Element dis = doc.selectFirst(".presentation.display p");
        info.setDescription(dis == null ? null : dis.text());


        List<String> actors = new ArrayList<>();
        doc.select("div.infos div.casting ul li").forEach(element -> {
            String text = element.text();
            actors.add(text.substring(0, text.indexOf('(')).trim());
        });
        info.setActors(actors);

        //info.setEpisode(episode);
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
