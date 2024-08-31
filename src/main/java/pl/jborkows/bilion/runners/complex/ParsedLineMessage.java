package pl.jborkows.bilion.runners.complex;

import java.nio.charset.StandardCharsets;

record ParsedLineMessage(byte[] name, int lentgh,  int integerPart, int decimalPart){
    String stationName(){
        return new String(name, 0, lentgh, StandardCharsets.UTF_8);
    }

}
