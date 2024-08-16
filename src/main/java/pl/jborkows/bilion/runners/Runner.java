package pl.jborkows.bilion.runners;

import java.nio.file.Path;

public interface Runner {
    void process(Path path) throws Exception;
    default String name(){
        return this.getClass().getSimpleName();
    };
}
