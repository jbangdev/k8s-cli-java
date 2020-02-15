//usr/bin/env jbang "$0" "$@" ; exit $?
// client-java for k8s
//DEPS io.kubernetes:client-java:5.0.0
// pico for cli
//DEPS info.picocli:picocli:4.1.4
// text output table
//DEPS com.massisframework:j-text-utils:0.3.4

package com.d2iq.kubectl.jbang;

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

import static picocli.CommandLine.*;

@Command(
        name = "example",
        description = "An example kubectl plugin"
)
public class KubeExample {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested = false;

    @Command(
            name = "pod-list",
            description = "lists pods in the cluster using a structured approach"
    )
    public void list() {

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

    @Command(
            name = "list2",
            description = "lists pods in the cluster using the protoclient approach"
    )
    public void list2(@Option(names = {"-n", "--namespace"}, defaultValue = "default", description = "namespace to use for getting pods")
                              String namespace
    ) {

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

    @Command(
            description = "lists resources in the k8s cluster"
    )
    public void resources() {

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

    @Command(
            name = "pod-add",
            description = "Adds a pod to a k8s cluster"
    )
    public void add(@Parameters(index = "0") String name,
                    @Option(names = {"-i", "--image"}, defaultValue = "nginx", description = "image to use for pod")
                            String image,

                    @Option(names = {"-n", "--namespace"}, defaultValue = "default", description = "namespace to use for pod")
                            String namespace
    ) {
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

    public static void main(String[] args) {

        CommandLine cmd = new CommandLine(new KubeExample());

        if (cmd.isUsageHelpRequested()) {
            cmd.usage(cmd.getOut());
            return;
        }

        cmd.execute(args);
    }
}


