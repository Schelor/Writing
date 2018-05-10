package com.netty.meshagent.lb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import com.netty.meshagent.registry.EtcdRegistry;
import com.netty.meshagent.registry.EtcdRegistryFactory;
import com.netty.meshagent.registry.LightNode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by juntao.hjt on 2018-04-15 11:13.
 */
public class LoadBalanceFactory {

    private static final Logger logger = LogManager.getLogger(LoadBalanceFactory.class);

    private static final String URI_PATTERN = "sidecar://%s:%d";

    private static EtcdRegistry registry = EtcdRegistryFactory.registry();

    private static RandomLoadBalance random = new RandomLoadBalance();

    private static ConsistentHashLoadBalance consistent = new ConsistentHashLoadBalance();

    static {

    }

    public static Optional<URI> random() {
        List<LightNode> lookup = registry.lookup();
        if (lookup == null || lookup.isEmpty()) {
            return Optional.empty();
        }
        LightNode select = random.select(lookup);
        if (select == null) {
            return Optional.empty();
        }
        logger.info("random select server ip: {}, port: {}, weight: {}",
            select.getHost(), select.getPort(), select.getWeight());
        return toUri(select);
    }

    public static Optional<URI> toUri(LightNode node) {
        try {
            URI uri = new URI(String.format(URI_PATTERN, node.getHost(), node.getPort()));
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            logger.error("resolve the uri error.", e);
            return Optional.empty();
        }
    }

    public static Optional<URI> consistent(LoadBalance.Router router) {

        LightNode select = consistent.select(router);
        if (select == null) {
            return Optional.empty();
        }
        logger.info("consistent select server ip: {}, port: {}, weight: {}",
            select.getHost(), select.getPort(), select.getWeight());
        return toUri(select);
    }


}
