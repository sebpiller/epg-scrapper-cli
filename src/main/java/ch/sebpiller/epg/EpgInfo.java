package ch.sebpiller.epg;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Models information about an EPG entry. An EPG is constituted by a lot of objects of this type.
 */
// TODO remove Serializable
public class EpgInfo implements Serializable {
    private Channel channel;
    private String title;
    /**
     * Normalized episode id (if any). Follow the format S&lt;xx>E&lt;yy> (eg. S01E04) or a plain number prefixed
     * with E (eg. E1234).
     */
    private String episode;
    private String subtitle;
    private String description;
    private ZonedDateTime timeStart;
    private ZonedDateTime timeStop;
    private List<String> producers;
    private List<String> directors;
    private List<String> actors;
    private List<String> scenarists;
    private Audience audience;
    private String category;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ZonedDateTime getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(ZonedDateTime timeStart) {
        this.timeStart = timeStart;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEpisode() {
        return episode;
    }

    public void setEpisode(String episode) {
        this.episode = episode;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ZonedDateTime getTimeStop() {
        return timeStop;
    }

    public void setTimeStop(ZonedDateTime timeStop) {
        this.timeStop = timeStop;
    }

    @Override
    public String toString() {
        return "EpgInfo{" +
                "channel=" + channel +
                ", category=" + category +
                ", timeStart=" + timeStart +
                ", timeStop=" + timeStop +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", episode='" + episode + '\'' +
                ", audience='" + audience + '\'' +
                ", description='" + description + '\'' +
                ", producers='" + Arrays.toString(producers == null ? null : producers.toArray()) + '\'' +
                ", directors='" + Arrays.toString(directors == null ? null : directors.toArray()) + '\'' +
                ", scenarists='" + Arrays.toString(scenarists == null ? null : scenarists.toArray()) + '\'' +
                ", actors='" + Arrays.toString(actors == null ? null : actors.toArray()) + '\'' +
                '}';
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public List<String> getDirectors() {
        return directors;
    }

    public void setDirectors(List<String> directors) {
        this.directors = directors;
    }

    public List<String> getProducers() {
        return producers;
    }

    public void setProducers(List<String> producers) {
        this.producers = producers;
    }

    public List<String> getScenarists() {
        return scenarists;
    }

    public void setScenarists(List<String> scenarists) {
        this.scenarists = scenarists;
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
