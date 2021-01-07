package ch.sebpiller.epgscrapper;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Models information about an EPG entry. An EPG is constituted by a lot of objects of this type.
 */
// TODO remove Serializable
public class EpgInfo implements Serializable {
    private Channel channel;
    private LocalDateTime timeStart;
    private String title;
    /**
     * Normalized episode id (if any). Follow the format S&lt;xx>E&lt;yy> (eg. S01E04) or a plain number prefixed
     * with E (eg. E1234).
     */
    // TODO support more patterns (where ne season is defined)
    private String episode;
    private String subtitle;
    private String description;
    private LocalDateTime timeStop;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public LocalDateTime getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(LocalDateTime timeStart) {
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

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setTimeStop(LocalDateTime timeStop) {
        this.timeStop = timeStop;
    }

    public LocalDateTime getTimeStop() {
        return timeStop;
    }

    @Override
    public String toString() {
        return "EpgInfo{" +
                "channel=" + channel +
                ", timeStart=" + timeStart +
                ", timeStop=" + timeStop +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", episode='" + episode + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
