package webserver.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Component;

// @Component
@Deprecated
public class KafkaManager {

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private ConsumerFactory consumerFactory;

    @Bean
    public ConcurrentKafkaListenerContainerFactory delayContainerFactory() {
        ConcurrentKafkaListenerContainerFactory container = new ConcurrentKafkaListenerContainerFactory();
        container.setConsumerFactory(consumerFactory);
        // 禁止自动启动
        container.setAutoStartup(false);
        // 手动提交offset
        container.getContainerProperties().setAckMode((ContainerProperties.AckMode.MANUAL));
        return container;
    }

    public void startListener(String listenerId) {
        if (!registry.getListenerContainer(listenerId).isRunning()) {
            registry.getListenerContainer(listenerId).start();
        }
        registry.getListenerContainer(listenerId).resume();

    }

    public void stopListener(String listenerId) {
        if (registry.getListenerContainer(listenerId).isRunning()) {
            registry.getListenerContainer(listenerId).stop();
        }
    }
}