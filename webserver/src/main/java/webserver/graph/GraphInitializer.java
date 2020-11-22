package webserver.graph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class GraphInitializer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private PlayerHeroGraph playerHeroGraph;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (contextRefreshedEvent.getApplicationContext().getParent() == null) {
            playerHeroGraph.buildGraph();
        }
    }
}
