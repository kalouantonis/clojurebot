FROM openjdk:8-jre
MAINTAINER Antonis Kalou <kalouantonis@gmail.com>

ADD target/uberjar/clojurebot-0.1.0-SNAPSHOT-standalone.jar /srv/clojurebot.jar
# Add security policy
ADD example.policy /root/.java.policy

CMD ["java", "-jar", "/srv/clojurebot.jar"]
