package pl.jborkows.bilion.runners;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class ReadBytesSync implements Runner {

    private static class Station {
        long max = 0;
        long min = 0;
        long all = 0;
        int count = 0;
    }

    private final Map<NamedStation, Station> stations = new HashMap<>();
    private final BlockingDeque<DataSend<byte[]>> bytesQueue = new LinkedBlockingDeque<>(100000);
    private final BlockingDeque<DataSend<List<NamedStation>>> stationQueue = new LinkedBlockingDeque<>(100000);
    private final int bufforSize;

    private sealed interface DataSend<T>{
        final  class Data<T> implements DataSend<T>{
           final T data;

            public Data(T data) {
                this.data = data;
            }
        }
        final  class Finish<T> implements DataSend<T>{}
    }

    public ReadBytesSync(int bufforKBSize) {
        this.bufforSize = bufforKBSize * 1024;
    }

    @Override
    public void process(Path path) throws Exception {

        var bytesWorker = new Thread(this::processBytes, "bytes processor");
        var stationProcessor = new Thread(this::processStations, "station processor");
        bytesWorker.start();
        stationProcessor.start();

        final int bufferSize = 4096; // Define the buffer size. 4096 bytes is a common choice.
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()));) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                byte[] offered = Arrays.copyOf(buffer, bytesRead);
                var message = new DataSend.Data<>(offered);
                while(!bytesQueue.offerLast(message, 1,TimeUnit.SECONDS)){};
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(!bytesQueue.offerLast(new DataSend.Finish<>(), 1,TimeUnit.SECONDS)){};
        System.out.println("Reading finished");
        bytesWorker.join();
        stationProcessor.join();
        stations.forEach((k, v) -> {
            System.out.printf("%s;%s;%s;%s;\n", new String(k.name, StandardCharsets.UTF_8), (1.0 * v.all / v.count) / 100, v.max / 100.0, v.min / 100.0);
        });
    }

    private void processStations() {

        try {
            while (true) {
                var message = stationQueue.poll(100, TimeUnit.MICROSECONDS);
//                System.out.println("Station Queue size " + bytesQueue.size());
                if(message == null){
                    continue;
                }
                if(message instanceof DataSend.Finish){
                    return;
                }
                var data = (DataSend.Data<List<NamedStation>>) message;
                var read = data.data;
                if (read == null || read.isEmpty()) {
                    continue;
                }

                for (NamedStation namedStation : read) {
                    var value = namedStation.value;
                    stations.compute(namedStation, (name, station) -> {
                        if (station == null) {
                            station = new Station();
                        }
                        station.all += value;
                        station.count += 1;
                        station.max = Math.max(station.max, value);
                        station.min = Math.min(station.min, value);
                        return station;
                    });
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    private List<NamedStation> namedStations;

    private void sendNamedStations() {
        var offered= new DataSend.Data<>(List.copyOf(namedStations));

        while (true){
            try {
                if (stationQueue.offer(offered, 10, TimeUnit.MICROSECONDS)) break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        namedStations = null;
    }
    private void addNamedStation( NamedStation namedStation) {
        int initialCapacity = 100;
        if(namedStations == null) {
           namedStations = new ArrayList<>(initialCapacity);
       }
       namedStations.add(namedStation);
       if(namedStations.size() == initialCapacity){
           sendNamedStations();
       }
    }

    private final byte[] temporalNameBuffer = new byte[1000];
    private int nameLength = 0;
    private long tempValue = 0;
    private int valueLength = 0;
    private enum State {
        PROCESSING_NAME,
        PROCESSING_UNKNOWN_SIGNED_VALUE,
        PROCESSING_NEGATIVE,
        PROCESSING_WHOLE_NUMBER,
        PROCESSING_DECIMAL_NUMBER,
    }

    private State state = State.PROCESSING_NAME;
    private long multiplier = 1;
    private void processBytes() {
        try {
            while (true) {
                var message = bytesQueue.pollFirst(100, TimeUnit.MICROSECONDS);
//                System.out.println("Bytes Queue size " + bytesQueue.size());
                if(message == null){
                    continue;
                }
                if(message instanceof DataSend.Finish) {
                    sendNamedStations();
                    while(!stationQueue.offerLast(new DataSend.Finish<>(), 1,TimeUnit.SECONDS)){};
                    return;
                }
                var data = (DataSend.Data<byte[]>) message;
                var read = data.data;
                if (read == null) {
                    continue;
                }
                for (int i = 0; i < read.length;) {
                    var aByte = read[i];

                    if(aByte == '\0' || aByte == '\n'){
                        i++;
                        continue;
                    }
                    switch (state){
                        case PROCESSING_NAME -> {
                            i = reactOnComma(aByte, i, read);
                        }
                        case PROCESSING_NEGATIVE -> {
                            i = processNegative(aByte, i);
                        }
                        case PROCESSING_UNKNOWN_SIGNED_VALUE -> {
                            i = processUnsigned(read, i);
                        }
                        case PROCESSING_WHOLE_NUMBER -> {
                            i = getI(aByte, i);
                        }
                        case PROCESSING_DECIMAL_NUMBER -> {
                            if(aByte < '0' || aByte > '9'){
                                state = State.PROCESSING_NAME;
                                addNamedStation(new NamedStation(Arrays.copyOf(temporalNameBuffer, nameLength),multiplier*tempValue));
                                for (int j = 0; j < nameLength; j++) {
                                    temporalNameBuffer[j] = 0;
                                }
                                nameLength=0;
                                valueLength = 0;
                                tempValue= 0;
                            }else {
                                tempValue = parse(aByte) + tempValue*10;
                                i++;
                            }
                        }
                    }
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private int getI(byte aByte, int i) {
        if(aByte == '.'){
            state = State.PROCESSING_DECIMAL_NUMBER;
            i++;
            valueLength = 0;
            multiplier=1;
        }else {
            var parsed = parse(aByte);
            tempValue = parsed + tempValue * ((valueLength++) > 0 ? 10 : 1);
            i++;
        }
        return i;
    }

    private int processUnsigned(byte[] read, int i) {
        if(read[i]=='-'){
            state = State.PROCESSING_NEGATIVE;
            i++;
        }else{
            state = State.PROCESSING_WHOLE_NUMBER;
        }
        return i;
    }

    private int processNegative(byte aByte, int i) {
        if(aByte == '.'){
            state = State.PROCESSING_DECIMAL_NUMBER;
            i++;
            multiplier=-1;
            valueLength = 0;
        }else {

            var parsed = parse(aByte);
            tempValue = parsed + tempValue * ((valueLength++) > 0 ? 10 : 1);
            i++;
        }
        return i;
    }

    private int reactOnComma(byte aByte, int i, byte[] read) {
        if(';' == aByte) {
            if(i +1 == read.length) {
                state = State.PROCESSING_UNKNOWN_SIGNED_VALUE;
                i++;
            }else{
                if(read[i +1]=='-'){
                    state = State.PROCESSING_NEGATIVE;
                    i +=2;
                }else{
                    state = State.PROCESSING_WHOLE_NUMBER;
                    i++;
                }
            }
        }else {
            temporalNameBuffer[nameLength++] = aByte;
            i++;
        }
        return i;
    }


    private static long parse(byte c) {
        return c - '0';
    }

    @Override
    public String name() {
        return this.getClass().getName() +  " with buffer " + bufforSize;
    }

    private static final class NamedStation {
        final byte[] name;
        final Long value;
        private final int hash;

        private NamedStation(byte[] name, Long value ) {
            this.name = name;
            this.value = value;
            this.hash = Arrays.hashCode(name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NamedStation that = (NamedStation) o;
            return Objects.equals(hash, that.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(hash);
        }

        @Override
        public String toString() {
            return "NamedStation{" +
                    "name=" + new String(name, StandardCharsets.UTF_8)+
                    ", value=" + value +
                    ", hash=" + hash +
                    '}';
        }
    }
}
