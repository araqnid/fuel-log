FROM java-base:latest
ARG app
ARG version

COPY app /srv/app
RUN ln -s $app /srv/app/bin/run

VOLUME /srv/events

RUN mkdir /srv/snapshots

ENV JAVA_HOME=/usr/lib/jvm/zulu-9-amd64 \
    EVENT_SPOOL=/srv/events \
    SNAPSHOT_SPOOL=/srv/snapshots \
    GOOGLE_CLIENT_ID=xyzzy \
    GOOGLE_CLIENT_SECRET=xyzzy \
    FACEBOOK_APP_ID=xyzzy \
    FACEBOOK_APP_SECRET=xyzzy \
    DOCUMENT_ROOT=/srv/app/content/www \
    PORT=8000

STOPSIGNAL SIGTERM
EXPOSE 8000

CMD ["/srv/app/bin/run"]
