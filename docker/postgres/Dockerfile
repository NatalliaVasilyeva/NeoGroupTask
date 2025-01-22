FROM postgres:17.2-bullseye

COPY sql/* /docker-entrypoint-initdb.d/

ENV DEBUG="true"
ENV POSTGRES_PASSWORD="postgres"
ENV PG_TRUST_LOCALNET="true"
ENV PGDATA=/data
ENV POSTGIS_MAJOR=3
ENV POSTGIS_VERSION=3.5.1+dfsg-1.pgdg110+1
#ENV POSTGRES_DB=time-log

RUN apt-get update \
      && apt-cache showpkg postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
      && apt-get install -y --no-install-recommends \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR=$POSTGIS_VERSION \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR-scripts \
      && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=10s --timeout=4s --retries=150 \
  CMD pg_isready -U postgres && psql -lqt -U postgres | grep -qw healtcheck_db

ENTRYPOINT ["docker-entrypoint.sh"]

CMD ["postgres"]