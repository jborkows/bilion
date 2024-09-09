package pl.jborkows.bilion.runners.complex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class Store {
    private static final ThreadLocal<Map<Integer,Data>> data = ThreadLocal.withInitial(HashMap::new);
    static List<Map<Integer,Data>> results = new ArrayList<>();

    synchronized private static void addResult(Map<Integer,Data> result) {
        results.add(result);
    }

    void gather(){
        addResult(data.get());
    }

    static final class Data {
        private final String name;

        int sum = 0;
        int count = 0;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;


        Data(String name){
            this.name = name;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "name='" + name + '\'' +
                    ", avg=" + sum*1l/count +
                    ", max=" + max +
                    ", min=" + min +
                    '}';
        }
    }


     Collection<Data> getData() {
        return data.get().values();
    }

     void register(byte[] chunk, int beginName, int offsetName, int value){
        var code = hashCode(chunk, beginName, offsetName);

        data.get().compute(code, (c,data)->{
            if(data == null){
                data = getData(chunk, beginName, offsetName);
            };
            data.count++;
            data.max = Math.max(value,data.max);
            data.min = Math.min(value,data.min);
            data.sum += value;
            return data;
        });
    }

    private static Data getData(byte[] chunk, int beginName, int offsetName) {
        byte[] bytes = Arrays.copyOfRange(chunk, beginName, beginName + offsetName);
        String name = new String(bytes);
        return new Data(name);
    }

    private static int hashCode(byte[] name, int begin, int offsetName){
        int hash = 0;
        for(int i = begin;i<begin+offsetName;i++) {
            hash = 31*hash + name[i];
        }
        return hash;
    }
}
