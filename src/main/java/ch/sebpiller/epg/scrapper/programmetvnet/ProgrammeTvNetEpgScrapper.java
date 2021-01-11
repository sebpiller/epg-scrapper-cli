package ch.sebpiller.epg.scrapper.programmetvnet;

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
        x.put(Channel.THIRTEENTH_STREET, "programme-13eme-rue-26");
        // TODO
        x.put(Channel.TF1_SERIES_FILMS, "programme-tf1-series-films-201");
        x.put(Channel.SYFY, "programme-syfy-110");
        x.put(Channel.CANALPLUS, "programme-canalplus-2");
        x.put(Channel.AB3, "programme-ab-3-138");
        x.put(Channel.NATGEO, "programme-national-geographic-98");
        x.put(Channel.NATGEOWILD, "programme-nat-geo-wild-207");
        x.put(Channel.LCI, "programme-lci-la-chaine-info-78");
        x.put(Channel.DISNEY_CHANNEL, "programme-disney-channel-57");
        x.put(Channel.DISNEY_JUNIOR, "programme-disney-junior-166");
        x.put(Channel.VOYAGE, "programme-voyage-134");
        x.put(Channel.USHUAIA, "programme-ushuaia-tv-132");
        x.put(Channel.PARAMOUNT, "programme-paramount-channel-226");
        x.put(Channel.FRANCE24, "programme-france-24-142");
        x.put(Channel.EQUIDIA, "programme-equidia-59");
        x.put(Channel.CHASSE_PECHE, "programme-chasse-et-peche-42");
        //x.put(Channel.RMCDECOUVERTE, "programme-rmc-decouverte-205");

        MAP = Collections.unmodifiableMap(x);
    }

    private final Map<Channel, EpgInfo> lastEpgs = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void scrapeEpg(Predicate<Channel> filterChannel, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {
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
                    // fetch data for current channel
                    doc = Jsoup.connect(ROOT_URL + '/' + DAYSTR_FORMAT.format(dayFetch) + '/' + c.getValue() + ".html").get();
                    scrapeDocument(doc, scrapeDetails, listener);
                    dayFetch = dayFetch.plusDays(1); // tomorrow
                } catch (HttpStatusException e) {
                    if (e.getStatusCode() == 404) {
                        hasMoreData = false;
                    } else {
                        throw new RuntimeException(e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (LOG.isTraceEnabled())
                    LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch));
            } while (hasMoreData);
        }
    }

    void scrapeDocument(Document doc, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {
        // https://www.programme-tv.net/programme/chaine/2021-01-14/programme-mangas-82.html
        String uri = doc.getElementsByAttributeValue("rel", "canonical").attr("href");
        Matcher m = Pattern.compile("^.*/(\\d{4}-\\d{2}-\\d{2})/.*$").matcher(uri);

        if (!m.matches()) {
            throw new RuntimeException("can not find date of document");
        }

        LocalDate date = LocalDate.from(DAYSTR_FORMAT.parse(m.group(1)));

        LocalTime lastTime = null;
        LocalDate ld = date;

        String text = doc.select(".gridChannel-epgGrid .gridChannel-title").text();
        Channel c = Channel.valueOfAliases(text);
        if (c == null) {
            throw new RuntimeException("can not find channel " + text);
        }

        /*
            <div class="singleBroadcastCard" data-wide-target>
                            <div class="singleBroadcastCard-hour" data-startinghour="00">
                                00h40
                            </div>
                            <div class="singleBroadcastCard-infos">
                                <a class="singleBroadcastCard-title" href="https://www.programme-tv.net/programme/jeunesse/r206118-jayce-et-les-conquerants-de-la-lumiere/139055-lenlevement-dun-savant/" data-wide>
                                    Jayce et les conquérants de la lumière
                                </a>
                                <div class="singleBroadcastCard-subtitle">
                                    L&#039;enlèvement d&#039;un savant
                                </div>
                                <div class="singleBroadcastCard-genre">
                                    Dessin animé
                                </div>
                                <div class="singleBroadcastCard-duration">
            <span class="singleBroadcastCard-durationContent">
                                        25min
            </span>
                                    <div class="singleBroadcastCard-rebroadcast">Rediffusion</div>
                                </div>
                            </div>
                            <div class="singleBroadcastCard-imageWrapper">




                                <div class="pictureTagGenerator  pictureTagGenerator-ratio-5-7">


                                    <img src="https://tel.img.pmdstatic.net/fit/https.3A.2F.2Fprd2-tel-epg-img.2Es3-eu-west-1.2Eamazonaws.2Ecom.2Fprogram.2F61fff3819c85b5e4.2Ejpg/64x90/quality/80/jayce-et-les-conquerants-de-la-lumiere.jpg"
                                         sizes="(max-width: 750px) 64px, (max-width: 1023px) 64px, (min-width: 1024px) 64px"
                                         srcset="https://tel.img.pmdstatic.net/fit/https.3A.2F.2Fprd2-tel-epg-img.2Es3-eu-west-1.2Eamazonaws.2Ecom.2Fprogram.2F61fff3819c85b5e4.2Ejpg/64x90/quality/80/jayce-et-les-conquerants-de-la-lumiere.jpg 64w, https://tel.img.pmdstatic.net/fit/https.3A.2F.2Fprd2-tel-epg-img.2Es3-eu-west-1.2Eamazonaws.2Ecom.2Fprogram.2F61fff3819c85b5e4.2Ejpg/128x180/quality/80/jayce-et-les-conquerants-de-la-lumiere.jpg 128w"
                                         alt="Jayce et les conquérants de la lumière"
                                         class="  apply-ratio" />

                                    <div class="singleBroadcastCard-itemImageBlocHover"><span class="singleBroadcastCard-itemImageBlocHoverText">Lire <br /> le <br />résumé</span></div>

                                </div>


                                <span class="singleBroadcastCard-arrow">

    <svg class="icon icon-chevron-right"
    >
                <use href="#chevron-right"></use>
    </svg>

        </span>
                            </div>
                        </div>
         */

        for (Element a : doc.getElementsByClass("singleBroadcastCard")) {
            EpgInfo info = new EpgInfo();
            info.setChannel(c);

            String from = a.selectFirst(".singleBroadcastCard-hour").text();
            String[] split = from.split("h");
            LocalTime time = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));

            if (lastTime != null && time.isBefore(lastTime)) {
                ld = ld.plusDays(1);
            }
            lastTime = time;

            ZonedDateTime zdt = ZonedDateTime.of(ld, time, ZoneId.of("Europe/Zurich"));
            info.setTimeStart(zdt);
            Element title = a.selectFirst(".singleBroadcastCard-title");
            info.setTitle(title.text());
            info.setSubtitle(a.selectFirst(".singleBroadcastCard-subtitle").text());

            info.setCategory(resolveCategory(a.selectFirst(".singleBroadcastCard-genre").text()));

            // details are expensive to fetch. Some are filtered out by default.
            if (scrapeDetails.test(info)) {
                parseDetails(title.attr("href"), info);
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
