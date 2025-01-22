FROM redis:7.0.11

COPY redis.conf /data/redis.conf
EXPOSE 6379
CMD [ "redis-server", "/data/redis.conf" ]