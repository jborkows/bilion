FROM maven:3.9.8-amazoncorretto-21-debian-bookworm as data_preparer
RUN apt-get update && apt-get install -y git
RUN git clone https://github.com/gunnarmorling/1brc.git /reference
WORKDIR /reference
RUN mvn verify
RUN ./create_measurements.sh 1000000000

FROM maven:3.9.8-amazoncorretto-21-debian-bookworm as maven
COPY . /app
WORKDIR /app
RUN mvn package
RUN echo "DONE"

FROM  ghcr.io/graalvm/native-image-community:22 as graalvm
COPY --from=maven /app/target/*.jar /home/app/application.jar
WORKDIR /home/app
RUN native-image --no-fallback -jar application.jar

FROM maven:3.9.8-amazoncorretto-21-debian-bookworm as graalRunner
COPY --from=data_preparer /reference/measurements.txt /app/data.txt
COPY --from=graalvm /home/app/application /app/application
WORKDIR /app
ENTRYPOINT  ./application data.txt graal

FROM  ghcr.io/graalvm/native-image-community:22 as jvmRunner
COPY --from=data_preparer /reference/measurements.txt /home/app/data.txt
COPY --from=maven /app/target/*.jar /home/app/application.jar
WORKDIR /home/app
ENTRYPOINT java -jar application.jar data.txt jvm

