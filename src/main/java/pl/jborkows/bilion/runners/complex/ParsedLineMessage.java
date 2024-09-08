package pl.jborkows.bilion.runners.complex;

import java.nio.charset.StandardCharsets;
import java.util.List;

record ParsedLineMessage(List<ParsedLineItem> parsedLineItems){
}

record ParsedLineItem (byte[] name, int begin, int offsetName,  int integerPart, int decimalPart){
    String stationName(){
        return new String(name, begin,  offsetName, StandardCharsets.UTF_8);
    }
}