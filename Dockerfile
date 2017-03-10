FROM openjdk:8-jre
MAINTAINER Antonis Kalou <kalouantonis@gmail.com>

ADD target/uberjar/clojurebot-0.1.0-SNAPSHOT-standalone.jar /usr/bin/clojurebot.jar
# Add security policy
ADD example.policy /root/.java.policy

CMD ["java", "-jar", "/usr/bin/clojurebot.jar"]
