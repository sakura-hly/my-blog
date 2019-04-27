package example.netty.codec;

import org.jboss.marshalling.*;

import java.io.IOException;

public class MarshallingCodecFactory {

    /**
     * 创建Jboss Marshaller
     * @return
     * @throws IOException
     */
    protected static Marshaller buildMarshalling() throws IOException {
        final MarshallerFactory factory = Marshalling.getProvidedMarshallerFactory("serial");
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);

        Marshaller marshaller = factory.createMarshaller(configuration);
        return marshaller;
    }

    /**
     * 创建Jboss Unmarshaller
     * @return
     */
    protected static Unmarshaller buildUnMarshalling() throws IOException {
        final MarshallerFactory factory = Marshalling.getProvidedMarshallerFactory("serial");
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);

        Unmarshaller unmarshaller = factory.createUnmarshaller(configuration);
        return unmarshaller;
    }
}
