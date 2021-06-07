FROM debian:buster
LABEL arch="armv7|arm64"
ENV DEBIAN_FRONTEND=noninteractive

WORKDIR /tmp

#RUN \
#    { printf "deb http://nexus.home/repository/debian_buster/ buster main\n"; printf "deb http://nexus.home/repository/debian-security_buster-updates/ buster/updates main\n"; printf "deb http://nexus.home/repository/debian_buster-updates/ buster-updates main\n\n"; } > /etc/apt/sources.list

RUN \
    apt-get update -y && \
    apt-get install -y --no-install-recommends --no-install-suggests \
      openjdk-11-jre-headless && \

    rm -rf /var/lib/apt/lists/*

COPY target/epg-scrapper-*-sb-executable.jar /epg-scrapper-latest.jar

CMD [ "/bin/sh", "-c", "java -jar /epg-scrapper-latest.jar -o /data/epg.xml" ]