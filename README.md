# EPG-Scrapper

Tool to build an XMLTV file containing EPG (Electronic Program Guide) with infos found in various sources:

- https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/schedules
- https://grille.ocs.fr
- https://playtv.fr/programmes-tv
- https://www.programme-tv.net/programme

Actually, the scrapping process scrapes all the available information from all sources for ~70 FR-CH channels (about 32'000 entries) in a bit less than 45 minutes (with a RPi4).

Designed to work with TVHeadend (PVR) and Kodi clients, but other compliant tools may be used too.

A cron POD for Kubernetes can be defined as: 

````yaml
---
### Download EPG guide periodically
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: cron-scrape-epg
spec:
  # twice a week, at 04:00 AM
  schedule: "0 4   * * 1,4"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: cron-epg-scrapper
              image: sebpiller/epg-scrapper:latest
              imagePullPolicy: Always
              volumeMounts:
                # will produce a file named 'epg.xml' in the path mapped on '/data'
                - mountPath: /data
                  subPath: config/data
                  name: epg-data
          volumes:
            - name: epg-data
              persistentVolumeClaim:
                claimName: tvheadend-pvc
          restartPolicy: OnFailure
````