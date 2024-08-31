package pl.jborkows.bilion.runners.complex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MessageChannelTest {

    @Test
    void shouldReadSameOrderAsWrite(){
        var messageChannel = new MessageChannel<String>("test",5000);
        messageChannel.writeTo("A");
        Assertions.assertEquals("A", messageChannel.readFrom());
        messageChannel.writeTo("B");
        messageChannel.writeTo("C");
        Assertions.assertEquals("B", messageChannel.readFrom());
        Assertions.assertEquals("C", messageChannel.readFrom());
    }
}
