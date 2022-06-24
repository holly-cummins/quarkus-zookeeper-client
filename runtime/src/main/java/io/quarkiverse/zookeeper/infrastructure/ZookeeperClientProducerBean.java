package io.quarkiverse.zookeeper.infrastructure;

import java.io.IOException;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.wildfly.common.annotation.Nullable;

import io.quarkiverse.zookeeper.config.SessionConfig;

@ApplicationScoped
public class ZookeeperClientProducerBean {

    private static final Logger LOG = Logger.getLogger(ZookeeperClientProducerBean.class);

    public static final String EXTENSION_NAME = "zookeeper";

    private static final Set<States> CLOSEABLE_STATES = Set.of(
            States.ASSOCIATING,
            States.CONNECTED,
            States.CONNECTEDREADONLY,
            States.CONNECTING);

    @ConfigProperty(name = SessionConfig.CONNECTION_STRING)
    String cnString;

    @ConfigProperty(name = SessionConfig.TIMEOUT)
    int cnTimeout;

    @ConfigProperty(name = SessionConfig.CAN_BE_READ_ONLY)
    boolean canBeReadOnly;

    @Inject
    ClientStatusWatcher watcher;

    private ZooKeeper client = null;

    @PostConstruct
    public void init() {
        try {
            client = new ZooKeeper(cnString, cnTimeout, watcher, canBeReadOnly);
            LOG.infof("Zookeeper client has been initialized for [%s].", cnString);
        } catch (IOException e) {
            LOG.error("Cannot initialize the zookeeper client.", e);
        }
    }

    @PreDestroy
    public void tearDown() {
        if (client != null && CLOSEABLE_STATES.contains(client.getState())) {
            try {
                client.close();
                LOG.info("Zookeeper client has been closed.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while tearing down the zookeeper client");
            }
        }
        client = null;
    }

    @Produces
    @ApplicationScoped
    public @Nullable synchronized ZooKeeper createZKClient() {
        if (client == null || !CLOSEABLE_STATES.contains(client.getState())) {
            init();
        }
        return client;
    }
}
