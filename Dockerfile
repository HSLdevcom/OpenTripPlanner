FROM adoptopenjdk/openjdk11:alpine-slim

RUN apk add --update curl bash ttf-dejavu && \
    rm -rf /var/cache/apk/*
VOLUME /opt/opentripplanner/graph

ENV OTP_ROOT="/opt/opentripplanner"

WORKDIR ${OTP_ROOT}
RUN touch logback.xml

ADD target/*-shaded.jar ${OTP_ROOT}/otp-shaded.jar
ADD entrypoint.sh .
RUN chmod +x entrypoint.sh

EXPOSE 8080
EXPOSE 8081

ENV JAVA_OPTS="-Xmx8G"

ENTRYPOINT ["./entrypoint.sh"]

