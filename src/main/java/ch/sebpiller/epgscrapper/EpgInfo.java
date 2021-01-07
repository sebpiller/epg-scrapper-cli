package ch.sebpiller.epgscrapper;

import java.time.LocalDateTime;

/**
 * Models information about an EPG entry. An EPG is constituted by a lot of objects of this kind.
 */
public class EpgInfo {
    private Channel channel;
    private LocalDateTime timeStart;
    private String title;

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

    @Override
    public String toString() {
        return "EpgInfo{" +
                "channel=" + channel +
                ", timeStart=" + timeStart +
                ", title='" + title + '\'' +
                '}';
    }
}
