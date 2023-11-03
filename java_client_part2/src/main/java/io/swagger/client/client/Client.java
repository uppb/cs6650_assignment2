package io.swagger.client.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import java.awt.SystemTray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
  private static String image;
  private static String output_path;
  private static String IPAddress;
  private static void initializate(int initialNumThreads, String IPAddr, String image_file){
    ExecutorService executorService = Executors.newFixedThreadPool(initialNumThreads);
    for(int i = 0; i < initialNumThreads; i++){
      Runnable thread = () -> {
        ApiClient client = new ApiClient();
        client.setBasePath(IPAddr);
        DefaultApi apiInstance = new DefaultApi(client);
        try {
          String id = Producer.sendPost(apiInstance, image_file);
          for (int j = 0; j < 100; j++) {
            Producer.sendGet(apiInstance, id);
          }
        }catch (ApiException e){
          System.err.println(e.getMessage());
        }
      };
      executorService.submit(thread);
    }
    executorService.shutdown();
    try {
      executorService.awaitTermination(30, TimeUnit.SECONDS);
      System.out.println("Finished Initializing");
    } catch (InterruptedException e) {
      System.err.println("Initialization Timed Out");
    }
  }
  private static void startLoading(int threadGroupSize, int numThreadGroups, int delay, String IPAddr)
      throws InterruptedException {
    BlockingQueue<Record> queue = new LinkedBlockingQueue<>(100);
    CountDownLatch completed = new CountDownLatch(threadGroupSize*numThreadGroups);
    AtomicInteger success = new AtomicInteger(0);
    AtomicInteger failure = new AtomicInteger(0);
    long startTime = System.currentTimeMillis();
    Consumer consumer = new Consumer(output_path, queue, completed);
    Thread consumerThread = new Thread(consumer);
    consumerThread.start();

    ExecutorService executorService = Executors.newFixedThreadPool(300);
    for(int i = 0; i < numThreadGroups; i++){
      for(int j = 0; j < threadGroupSize; j++) {
        Producer producer = new Producer(IPAddr, image, queue, completed, success, failure);
        executorService.submit(producer);
      }
      Thread.sleep(1000L*delay);
    }
    completed.await();
    executorService.shutdown();
    long endTime = System.currentTimeMillis();
    double wallTime = (double) ((endTime - startTime) - 1000L * delay * numThreadGroups) /1000L;
    double throughput = (1000L *threadGroupSize*numThreadGroups) / wallTime;
    System.out.println("Successful Requests: " + success.get());
    System.out.println("Failed Requests: " + failure.get());
    System.out.println("wallTime: " + wallTime);
    System.out.println("throughput: " + throughput);
  }

  private static void findStatsfromArrayList(ArrayList<Double> list){
    Collections.sort(list);
    int n = list.size();
    double min = list.get(0);
    double max = list.get(n-1);
    double mean = list.stream().mapToDouble(val -> val).average().orElse(0.0);
    double median;
    if (n % 2 == 0) {
      median = (list.get((n / 2) - 1) + list.get(n / 2)) / 2.0;
    } else {
      median = list.get(n / 2);
    }
    int index = (int) Math.ceil((99.0 / 100.0) * n) - 1;
    double p99 =  list.get(index);
    System.out.println("min: " + min);
    System.out.println("max: " + max);
    System.out.println("mean: " + mean);
    System.out.println("median: " + median);
    System.out.println("p99: " + p99);
  }

  private static void calculateStats(String csv_file){
    ArrayList<Double> post_latency = new ArrayList<>();
    ArrayList<Double> get_latency = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csv_file))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(",");
        double latency = Double.parseDouble(values[1]);
        String type = values[2];
        if(type.equals("POST")) {post_latency.add(latency);}else get_latency.add(latency);
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    System.out.println("Response Time Stats for POST Requests: ");
    findStatsfromArrayList(post_latency);
    System.out.println("Response Time Stats for GET Requests: ");
    findStatsfromArrayList(get_latency);
  }

  public static void main(String[] args){
    image = "/Users/sunny/Downloads/nmtb.png";
    output_path = "/Users/sunny/Downloads/resources.csv";
    IPAddress = "http://cs6650-balancer-1431079427.us-west-2.elb.amazonaws.com:8080/java_servlet-1.0-SNAPSHOT/";
    //IPAddress = "http://34.219.175.41:8080/java_servlet-1.0-SNAPSHOT/";
    //IPAddress = "http://localhost:8080/java_servlet_war_exploded/";
    try{
      initializate(10, IPAddress, image);
      startLoading(10,30, 2, IPAddress);
      calculateStats(output_path);
    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
