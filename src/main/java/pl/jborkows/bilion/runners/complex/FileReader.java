package pl.jborkows.bilion.runners.complex;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

class FileReader implements Runnable {

    private final Path path;
    private final WriteChannel<ByteChunkMessage> writeChannel;

    FileReader(Path path, WriteChannel<ByteChunkMessage> writeChannel) {
        this.path = path;
        this.writeChannel = writeChannel;
    }

    @Override
    public void run() {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()));) {
            while (true){
                byte[] buffer = BytePool.INSTANCE.chunk();
                int bytesRead = bis.read(buffer, 0 , BytePool.FILE_BUFFER_SIZE);
                if(bytesRead == -1){
                    break;
                }
                writeChannel.writeTo(new ByteChunkMessage(buffer, bytesRead));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
