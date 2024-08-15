# Use a base image with Maven and JDK installed
FROM maven:3.9.8-amazoncorretto-21-debian-bookworm
# Copy your project files into the image
COPY . /app
COPY ./src/main/resources/weather_stations.csv /app/stations.csv
# Set the working directory
WORKDIR /app
# Compile and package the application
RUN mvn package
# Run the packaged application
CMD ["java", "-jar", "/app/target/bilion-1.0-SNAPSHOT.jar", "stations.csv"]

