package ch.sebpiller.epg.scrapper.programmetvnet;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import ch.sebpiller.epg.scrapper.ScrappingException;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
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
 * Scrape information using https://www.ocs.fr/grille-tv.
 */
public class ProgrammeTvNetEpgScrapper implements EpgScrapper {
    private static final Logger LOG = LoggerFactory.getLogger(ProgrammeTvNetEpgScrapper.class);

    private static final Map<Channel, String> MAP;
    /**
     * The format to insert day to fetch in the web url.
     */
    private static final DateTimeFormatter DAYSTR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // https://www.programme-tv.net/programme/chaine/2021-01-14/programme-mangas-82.html
    private static final String ROOT_URL = "https://www.programme-tv.net/programme/chaine";

    private static final Pattern EPISODE_PATTERN = Pattern.compile("Saison (\\d+) - Épisode (\\d+)");

    static {
        Map<Channel, String> x = new EnumMap<>(Channel.class);
        x.put(Channel.MANGAS, "programme-mangas-82");
        //x.put(Channel.THIRTEENTH_STREET, "programme-13eme-rue-26");
        // TODO
        x.put(Channel.TF1_SERIES_FILMS, "programme-tf1-series-films-201");
        x.put(Channel.SYFY, "programme-syfy-110");
        //x.put(Channel.CANALPLUS, "programme-canalplus-2");
        x.put(Channel.AB3, "programme-ab-3-138");
        x.put(Channel.NATGEO, "programme-national-geographic-98");
        x.put(Channel.NATGEOWILD, "programme-nat-geo-wild-207");
        //x.put(Channel.LCI, "programme-lci-la-chaine-info-78");
        x.put(Channel.DISNEY_CHANNEL, "programme-disney-channel-57");
        x.put(Channel.DISNEY_JUNIOR, "programme-disney-junior-166");
        //x.put(Channel.VOYAGE, "programme-voyage-134");
        //x.put(Channel.USHUAIA, "programme-ushuaia-tv-132");
        x.put(Channel.PARAMOUNT, "programme-paramount-channel-226");
        //x.put(Channel.FRANCE24, "programme-france-24-142");
        //x.put(Channel.EQUIDIA, "programme-equidia-59");
        //x.put(Channel.CHASSE_PECHE, "programme-chasse-et-peche-42");
        //x.put(Channel.RMCDECOUVERTE, "programme-rmc-decouverte-205");

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
                try {
                    // fetch data for current channeil
                    String url = ROOT_URL + '/' + DAYSTR_FORMAT.format(dayFetch) + '/' + c.getValue() + ".html";
                    doc = Jsoup.connect(url).get();

                    scrapeDocument(doc, c.getKey(), listener);
                    dayFetch = dayFetch.plusDays(1); // tomorrow
                } catch (HttpStatusException e) {
                    if (e.getStatusCode() == 404) {
                        hasMoreData = false;
                    } else {
                        throw new ScrappingException(e);
                    }
                } catch (IOException e) {
                    throw new ScrappingException(e);
                }

                if (LOG.isTraceEnabled())
                    LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));
            } while (hasMoreData);
        }

        LOG.info("scrapper {} has completed", this);
    }

    void scrapeDocument(Document doc, Channel c, EpgInfoScrappedListener listener) {
        // https://www.programme-tv.net/programme/chaine/2021-01-14/programme-mangas-82.html
        String uri = doc.getElementsByAttributeValue("rel", "canonical").attr("href");
        Matcher m = Pattern.compile("^.*/(\\d{4}-\\d{2}-\\d{2})/.*$").matcher(uri);

        if (!m.matches()) {
            return;
            //throw new ScrappingException("can not find date of document");
        }

        LocalDate date = LocalDate.from(DAYSTR_FORMAT.parse(m.group(1)));

        LocalTime lastTime = null;
        LocalDate ld = date;

       /* String text = doc.select(".gridChannel .gridChannel-title").text();
        Channel c = Channel.valueOfAliases(text);
        if (c == null) {
            throw new ScrappingException("can not find channel " + text);
        }*/

        for (Element a : doc.getElementsByClass("mainBroadcastCard")) {
            EpgInfo info = new EpgInfo(c);

            String from = a.selectFirst(".mainBroadcastCard-startingHour").text();
            String[] split = from.split("h");
            LocalTime time = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

            if (lastTime != null && time.isBefore(lastTime)) {
                ld = ld.plusDays(1);
            }
            lastTime = time;

            ZonedDateTime zdt = ZonedDateTime.of(ld, time, ZoneId.of("Europe/Zurich"));
            info.setTimeStart(zdt);
            Element title = a.selectFirst(".mainBroadcastCard-title a");
            info.setTitle(title.text());
            Element element = a.selectFirst(".mainBroadcastCard-subtitle");
            if (element == null)
                LOG.warn("no subtitle found for {}", title.text());
            else {
                info.setSubtitle(element.text());
            }
            info.setCategory(resolveCategory(a.selectFirst(".mainBroadcastCard-format").text()));

            String href = title.attr("href");
            if (StringUtils.isBlank(href))
                LOG.warn("no details uri found for {}", title.text());
            else {
                parseDetails(href, info);
            }
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

            throw new ScrappingException(e);
        } catch(SocketTimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new ScrappingException(e);
        } catch (IllegalArgumentException iae) {
            System.out.println("URI = " + uri);
        }
    }

    void parseDetails(Document doc, EpgInfo info) {
        info.setDescription(doc.select(".synopsis-text").text());

        //programOverviewHeader-title
        Element element = doc.selectFirst(".programOverviewHeader-title");
        if (element == null) {
//            System.out.println("======================");
//            System.out.println(doc);
//            System.out.println("======================");
        } else {
            String s = element.text();
            Matcher m = EPISODE_PATTERN.matcher(s);
            if (m.matches()) {
                String episode = String.format("S%02dE%02d",
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2))
                );

                info.setEpisode(episode);
            }
        }

    }

    private String resolveCategory(String text) {
        // FIXME improve mapping of category between rts and xmltv
        switch (text.toLowerCase().trim()) {
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
            case "jeunesse":
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
