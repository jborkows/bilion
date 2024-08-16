package pl.jborkows.bilion.runners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OwnSplitDoubleActiveParser implements Runner {
    private static class Station {
        long max = 0;
        long min = 0;
        long all = 0;
        int count = 0;
    }

    private final Map<String, Station> stations = new HashMap<>();

    @Override
    public void process(Path path) throws Exception {
        try (var stream = Files.lines(path, StandardCharsets.UTF_8)) {
            stream.filter(line -> !line.startsWith("#")).forEach(this::processLine);
        }
        stations.forEach((k, v) -> {
            System.out.printf("%s;%s;%s;%s;\n", k, (1.0 * v.all / v.count) / 1000, v.max / 1000, v.min / 1000);
        });
    }
    long number = 0;
    boolean parseNumber=false;
    boolean minus = false;
    long scaler = 1;
    boolean dot = false;

    /*
    TODO
    check if can use table before dot long[2] filled in same order
    and long[4] filled in "reverse" order and have single big calculation
     */
    private void processLine(String line) {
        var stationName = "";
        var index = 0;
        number = 0;
        parseNumber=false;
        minus = false;
        scaler = 1;
        dot = false;

        char[] charArray = line.toCharArray();
        int runningIndex=0;
        for (char c : charArray) {
            if (c == ';') {
                stationName = line.substring(0, runningIndex);
                parseNumber=true;
                continue;
            }
            if(!parseNumber){
                runningIndex++;
                continue;
            }
            if(dot){
                afterDot(c);
            }else{
                beforeDot(c);
            }

        }


        var value = minus ? -1 * number : number;
        var station = stations.computeIfAbsent(stationName, (_a) -> new Station());
        station.all += value;
        station.count += 1;
        station.max = Math.max(station.max, value);
        station.min = Math.min(station.min, value);
    }

    private void beforeDot(char c) {
        switch (c) {
            case '-':{
                minus=true;
                break;
            }
            case '.':{
                number*=1000;
                scaler = 10000;
                dot=true;
            }
            case '0':{
                number=number*10;
                break;
            }
            case '1':{
                number=number*10+1;
                break;
            }

            case '2':{
                number=number*10+2;
                break;
            }
            case '3':{
                number=number*10+3;
                break;
            }
            case '4':{
                number=number*10+4;
                break;
            }

            case '5':{
                number=number*10+5;
                break;
            }
            case '6':{
                number=number*10+6;
                break;

            }
            case '7':{
                number=number*10+7;
                break;
            }

            case '8':{
                number=number*10+8;
                break;
            }
            case '9':{
                number=number*10+9;
                break;
            }
        }
    }

    private void afterDot(char c) {
        scaler/=10;
        switch (c) {
            case '0':{
                break;
            }
            case '1':{
                number=number+1*scaler;
                break;
            }

            case '2':{
                number=number+2*scaler;
                break;
            }
            case '3':{
                number=number+3*scaler;
                break;
            }
            case '4':{
                number=number+4*scaler;
                break;
            }

            case '5':{
                number=number+5*scaler;
                break;
            }
            case '6':{
                number=number+6*scaler;
                break;

            }
            case '7':{
                number=number+7*scaler;
                break;
            }

            case '8':{
                number=number+8*scaler;
                break;
            }
            case '9':{
                number=number+9*scaler;
                break;
            }
        }
    }

}
