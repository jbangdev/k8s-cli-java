//usr/bin/env jbang "$0" "$@" ; exit $?
// client-java for k8s
//DEPS io.kubernetes:client-java:5.0.0
// pico for cli
//DEPS info.picocli:picocli:4.1.4
// text output table
//DEPS com.massisframework:j-text-utils:0.3.4

import dnl.utils.text.table.TextTable;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.ProtoClient;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.*;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.Config;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static picocli.CommandLine.*;
import static picocli.CommandLine.Model.*;

@Command(
        name = "example",
        description = "An example kubectl plugin",
        subcommands = {
                PodCommand.class,
                ResourcesCommand.class
        },
        mixinStandardHelpOptions=true
)
public class KubeExample implements Callable<Integer> {

    @Spec
    CommandSpec cmd;

    public static void main(String[] args) {

        int exitCode = new CommandLine(new KubeExample()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        cmd.commandLine().usage(cmd.commandLine().getOut());
        return 0;
    }
}

@Command(
        name = "pod",
        description = "Programmatically accesses pods in a k8s with 'add', 'list' and 'list2'",
        subcommands = {
                PodAddCommand.class,
                PodListCommand.class,
                PodList2Command.class
        }
)
class PodCommand {
}


@Command(
        name = "add",
        description = "Adds a pod to a k8s cluster"
)
class PodAddCommand implements Runnable {

    @Parameters(index = "0")
    String name;

    @Option(names = {"-i", "--image"}, description = "image to use for pod")
    String image = "nginx";

    @Option(names = {"-n", "--namespace"}, description = "namespace to use for pod")
    String namespace = "default";

    @Override
    public void run() {
        ApiClient client;
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            System.out.println("Unable to get cluster configuration");
            System.err.println(e);
            return;
        }

        try {
            CoreV1Api api = new CoreV1Api(client);
            Map<String, String> labels = new HashMap<>();
            labels.put("app", "demo");
            V1Pod pod = new V1PodBuilder()
                    .withNewMetadata()
                    .withName(name)
                    .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(name)
                    .withImage(image)
                    .endContainer()
                    .endSpec()
                    .build();

            api.createNamespacedPod(namespace, pod, null, null, null);

        } catch (ApiException e) {
            System.out.println("unable to get pod list");
            System.err.println(e);
            return;
        }
    }
}

@Command(
        name = "list2",
        description = "lists pods in the cluster using the protoclient approach"
)
class PodList2Command implements Runnable {

    @Option(names = {"-n", "--namespace"}, description = "namespace to use for getting pods")
    String namespace = "default";

    @Override
    public void run() {

        ProtoClient pc;
        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            pc = new ProtoClient(client);
        } catch (IOException e) {
            System.out.println("Unable to get cluster configuration");
            System.err.println(e);
            return;
        }

        ProtoClient.ObjectOrStatus<V1.PodList> list;
        try {
            String path = String.format("/api/v1/namespaces/%s/pods", namespace);
            list = pc.list(V1.PodList.newBuilder(), path);

        } catch (ApiException | IOException e) {
            System.out.println("unable to get pod list");
            System.err.println(e);
            return;
        }

        if (list.object.getItemsList().size() < 1) {
            System.out.println("No Pods found");
        } else {
            printTable(list.object);
        }
    }

    private void printTable(V1.PodList podList) {
        Object[][] data = new Object[podList.getItemsList().size()][];
        int i = 0;
        for (V1.Pod item : podList.getItemsList()) {
            ArrayList<Object> cols = new ArrayList<>();
            cols.add(item.getMetadata().getName());
            cols.add(item.getMetadata().getNamespace());
            data[i++] = cols.toArray();
        }

        String[] columnNames = {"Pod Name", "namespace"};

        TextTable tt = new TextTable(columnNames, data);
        tt.printTable();
    }
}

/**
 * PodListCommand is a command for listing pods in a kubernetes cluster.  This class is an example and requires
 * standard kubeconfig setup to work.
 *
 */
@Command(
        name = "list",
        description = "lists pods in the cluster using a structured approach"
)
class PodListCommand implements Runnable {
    @Override
    public void run() {

        ApiClient client;
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            System.out.println("Unable to get cluster configuration");
            System.err.println(e);
            return;
        }

        V1PodList list;
        try {
            CoreV1Api api = new CoreV1Api(client);
            list = api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
        } catch (ApiException e) {
            System.out.println("unable to get pod list");
            System.err.println(e);
            return;
        }

        if (list.getItems().size() < 1) {
            System.out.println("No Pods found");
        } else {
            printTable(list);
        }
    }

    /**
     * Prints the table of pods discovered
     *
     * @param list PodList of pods
     */
    private static void printTable(V1PodList list) {
        Object[][] data = new Object[list.getItems().size()][];
        int i = 0;
        for (V1Pod item : list.getItems()) {
            ArrayList<Object> cols = new ArrayList<>();
            cols.add(item.getMetadata().getName());
            cols.add(item.getMetadata().getNamespace());
            data[i++] = cols.toArray();
        }

        String[] columnNames = {"Pod Name", "namespace"};

        TextTable tt = new TextTable(columnNames, data);
        tt.printTable();
    }
}

@Command(
        name = "resources",
        description = "lists resources in the k8s cluster"
)
class ResourcesCommand implements Runnable {

    @Override
    public void run() {

        ApiClient client;
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            System.out.println("Unable to get cluster configuration");
            System.err.println(e);
            return;
        }

        V1APIResourceList list;
        try {
            CoreV1Api api = new CoreV1Api(client);
            list = api.getAPIResources();

        } catch (ApiException e) {
            System.out.println("unable to get resource list");
            System.err.println(e);
            return;
        }


        if (list.getResources().size() < 1) {
            System.out.println("No resources found");
        } else {
            printTable(list.getResources());
        }
    }

    private void printTable(List<V1APIResource> resources) {
        Object[][] data = new Object[resources.size()][];
        int i = 0;
        for (V1APIResource item : resources) {
            ArrayList<Object> cols = new ArrayList<>();
            cols.add(item.getName());
            cols.add(item.isNamespaced());
            cols.add(item.getKind());
            data[i++] = cols.toArray();
        }

        String[] columnNames = {"Resource", "Namespaced", "Kind"};

        TextTable tt = new TextTable(columnNames, data);
        tt.printTable();
    }
}
