FROM hub.kuangheng.com/jdk/mini-jdk8:1
#VOLUME /tmp
ADD cloud.calculate.jar app.jar
ENV LANG C.UTF-8
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]