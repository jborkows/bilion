package pl.jborkows.bilion.runners.complex;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

class FileReader implements Runnable {

    private final Path path;
    private final WriteChannel<ByteChunkMessage> writeChannel;

    FileReader(Path path, WriteChannel<ByteChunkMessage> writeChannel) {
        this.path = path;
        this.writeChannel = writeChannel;
    }

    @Override
    public void run() {
        final int bufferSize = 32 * 1024;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()));) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = bis.read(buffer, 0, bufferSize)) != -1) {
               writeChannel.writeTo(new ByteChunkMessage(Arrays.copyOf(buffer,bytesRead) ));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
