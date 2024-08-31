package pl.jborkows.bilion.runners;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

@EnabledIfEnvironmentVariable(named = "PERFORMANCE_TESTS", matches = ".+")
public class VectorIT {
    static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;
private Path outputPath;
    @BeforeEach
    void setUp() {
        try {
            outputPath = Files.createTempFile("aaa", "bb");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        outputPath.toFile().delete();
    }


@State(Scope.Thread)
    public static class Simd {

        @Param({"PREFERRED", "64", "128", "256", "512", "MAX"})
        private String inputSize;
        private VectorSpecies<Integer> vector;
        int[] arr1;
        int[] arr2;

        @Setup(Level.Trial)
        public  void setup() throws IOException {
            switch (inputSize){
                case "PREFERRED" -> vector = IntVector.SPECIES_PREFERRED;
                case "64" -> vector = IntVector.SPECIES_64;
                case "128" -> vector = IntVector.SPECIES_128;
                case "256" -> vector = IntVector.SPECIES_256;
                case "512" -> vector = IntVector.SPECIES_512;
                case "MAX" -> vector = IntVector.SPECIES_MAX;
                default -> throw new IllegalStateException("Unexpected value: " + inputSize);
            }
        }

        static Random random = new Random(new Date().getTime());
        @Setup(Level.Invocation)
        public void setupInvocation() {
            arr1 = new int[10_000];
            arr2 = new int[arr1.length + 1];
            for (int i = 0; i < arr1.length; i++) {
                arr1[i] = random.nextInt(10000);
                arr2[i] = random.nextInt(10000);
            }
            arr2[arr1.length] = -1;
        }

        @Benchmark
        public int[] addingTwoVectorSimd(){
            int[] finalResult = new int[arr1.length];
            int i = 0;
            for (; i < vector.loopBound(arr1.length); i += vector.length()) {
                var mask = vector.indexInRange(i, arr1.length);
                var v1 = IntVector.fromArray(vector, arr1, i, mask);
                var v2 = IntVector.fromArray(vector, arr2, i, mask);
                var result = v1.add(v2, mask);
                result.intoArray(finalResult, i, mask);
            }

            // tail cleanup loop
            for (; i < arr1.length; i++) {
                finalResult[i] = arr1[i] + arr2[i];
            }
            return finalResult;
        }
    }


    @State(Scope.Thread)
    public static class Plain {

        int[] arr1;
        int[] arr2;

        static Random random = new Random(new Date().getTime());

        @Setup(Level.Invocation)
        public void setupInvocation() {
            arr1 = new int[10_000];
            arr2 = new int[arr1.length + 1];
            for (int i = 0; i < arr1.length; i++) {
                arr1[i] = random.nextInt(10000);
                arr2[i] = random.nextInt(10000);
            }
            arr2[arr1.length] = -1;
        }

        @Benchmark
        public int[] addingTwoVectorPlain(){
            for (int i = 0; i < arr1.length; i++) {
                arr1[i] = i;
                arr2[i] = i * (-1 % 2);
            }
            arr2[arr1.length] = -1;
            int[] finalResult = new int[arr1.length];
            for (int i = 0; i < arr1.length; i++) {
                finalResult[i] = arr1[i] + arr2[i];
            }
            return finalResult;

        }
    }

    @Test
    void addingVectors() throws RunnerException {
        var opt = new OptionsBuilder()
                .include(VectorIT.Simd.class.getSimpleName())
                .include(VectorIT.Plain.class.getSimpleName())
                .forks(3)
                .warmupIterations(10)
                .build();
        new Runner(opt).run();
    }


    @Test
    void addTwoAndSaveVectors() {
        int[] arr1 = new int[50];
        int[] arr2 = new int[arr1.length + 1];
        for (int i = 0; i < arr1.length; i++) {
            arr1[i] = i;
            arr2[i] = i * (-1 % 2);
        }
        arr2[arr1.length] = -1;

        int i = 0;
        var start = LocalDateTime.now();

        start = LocalDateTime.now();
        int normalWay = 0;
        for (int j = 0; j < arr1.length; j++) {
            normalWay += arr1[j] + arr2[j];
        }

        System.out.println("Sum took " + Duration.between(start, LocalDateTime.now()));
        start = LocalDateTime.now();
        int result = 0;
        for (; i < SPECIES.loopBound(arr1.length); i += SPECIES.length()) {
            var mask = SPECIES.indexInRange(i, arr1.length);
            var v1 = IntVector.fromArray(SPECIES, arr1, i, mask);
            var v2 = IntVector.fromArray(SPECIES, arr2, i, mask);
            result += v1.add(v2, mask).reduceLanes(VectorOperators.ADD);
        }

        // tail cleanup loop
        for (; i < arr1.length; i++) {
            result += arr1[i] + arr2[i];
        }
        System.out.println("element size -> " + SPECIES.elementSize());
        System.out.println("length -> " + SPECIES.length());
        System.out.println("Vector took " + Duration.between(start, LocalDateTime.now()));

        Assertions.assertEquals(normalWay, result);
    }

}
