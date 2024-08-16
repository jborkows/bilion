# Use a base image with Maven and JDK installed
FROM maven:3.9.8-amazoncorretto-21-debian-bookworm
RUN apt-get update && apt-get install -y git
RUN git clone https://github.com/gunnarmorling/1brc.git /reference
WORKDIR /reference
RUN mvn verify
RUN ./create_measurements.sh 1000000000

# Copy your project files into the image
COPY . /app
COPY ./src/main/resources/weather_stations.csv /app/stations.csv
# Set the working directory
WORKDIR /app
RUN mvn package
RUN echo "DONE"
CMD ["java", "-jar", "/app/target/bilion-1.0-SNAPSHOT.jar", "/reference/measurements.txt"]

