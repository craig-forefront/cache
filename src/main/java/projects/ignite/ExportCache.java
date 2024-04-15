package projects.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.discovery.zk.ZookeeperDiscoverySpi;

/**
 * Export ignite cache
 */
public class ExportCache 
{
    public static void main(String[] args) throws IgniteException {
        // Preparing IgniteConfiguration using Java APIs
        IgniteConfiguration cfg = new IgniteConfiguration();

        // The node will be started as a client node.
        cfg.setClientMode(true);

        // Classes of custom Java logic will be transferred over the wire from this app.
        cfg.setPeerClassLoadingEnabled(true);

        ZookeeperDiscoverySpi zkDiscoverySpi = new ZookeeperDiscoverySpi();

        zkDiscoverySpi.setZkConnectionString("127.0.0.1:34076,127.0.0.1:43310,127.0.0.1:36745");
        zkDiscoverySpi.setSessionTimeout(30_000);

        zkDiscoverySpi.setZkRootPath("/ignite");
        zkDiscoverySpi.setJoinTimeout(10_000);

        //Override default discovery SPI.
        cfg.setDiscoverySpi(zkDiscoverySpi);

        // Starting the node
        Ignite ignite = Ignition.start(cfg);

        // Create an IgniteCache and put some values in it.
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache("myCache");
        cache.put(1, "Hello");
        cache.put(2, "World!");
        
        Affinity<Object> cacheAffinity = ignite.affinity("myCache");
        final Integer partitions = cacheAffinity.partitions();

        System.out.println(">> Created the cache and add the values.");
        System.out.println("Total number of partitions is: " + partitions);

        // Executing custom Java compute task on server nodes.
        ignite.compute(ignite.cluster().forServers()).broadcast(new RemoteTask());

        System.out.println(">> Compute task is executed, check for output on the server nodes.");

        // Disconnect from the cluster.
        ignite.close();
    }

    /**
     * A compute tasks that prints out a node ID and some details about its OS and JRE.
     * Plus, the code shows how to access data stored in a cache from the compute task.
     */
    private static class RemoteTask implements IgniteRunnable {
        @IgniteInstanceResource
        Ignite ignite;

        @Override public void run() {
            System.out.println(">> Executing the compute task");

            System.out.println(
                "   Node ID: " + ignite.cluster().localNode().id() + "\n" +
                "   OS: " + System.getProperty("os.name") +
                "   JRE: " + System.getProperty("java.runtime.name"));

            IgniteCache<Integer, String> cache = ignite.cache("myCache");

            System.out.println(">> " + cache.get(1) + " " + cache.get(2));
        }
    }
}