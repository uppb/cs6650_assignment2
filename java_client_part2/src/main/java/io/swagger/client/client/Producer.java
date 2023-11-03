package io.swagger.client.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.AlbumInfo;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable{
  private String IPAddr;
  private String image_file;
  private BlockingQueue<Record> queue;
  private CountDownLatch completed;
  private int success;
  private int failure;
  private AtomicInteger success_global;
  private AtomicInteger failure_global;
  public Producer(String IPAddr,
      String image_file, BlockingQueue queue, CountDownLatch completed, AtomicInteger success, AtomicInteger failure) {
    this.IPAddr = IPAddr;
    this.image_file = image_file;
    this.queue = queue;
    this.completed = completed;
    this.success_global = success;
    this.failure_global = failure;
  }

  public static String sendPost(DefaultApi api, String img) throws ApiException {
    File image = new File(img);
    AlbumsProfile profile = new AlbumsProfile();
    profile.setArtist("Me");
    profile.setTitle("Some Title");
    profile.setYear("2018");
    ImageMetaData result = api.newAlbum(image, profile);
    return result.getAlbumID();
  }
  public static void sendGet(DefaultApi api, String albumID) throws ApiException {
    AlbumInfo result = api.getAlbumByKey(albumID);
  }

  private String getRuntime(DefaultApi api, String type, String albumID) throws InterruptedException{
    long start_time = System.currentTimeMillis();
    String id = "-1";
    if(type.equals("POST")) {
      for(int i = 0; i < 5; i++) {
        try {
          id = sendPost(api, this.image_file);
          success++;
          i = 5;
        } catch (ApiException e) {
          failure++;
          System.out.println(e.getMessage());
          System.err.println("Exception when calling DefaultApi#newAlbum");
          e.printStackTrace();
        }
      }
    }else{
      for(int i = 0; i < 5; i++) {
        try {
          sendGet(api, albumID);
          success++;
          i = 5;
        } catch (ApiException e) {
          failure++;
          System.err.println("Exception when calling DefaultApi#getAlbumByKey");
          e.printStackTrace();
        }
      }
    }
    long end_time = System.currentTimeMillis();
    Record record = new Record(start_time, end_time, type, 200);
    queue.put(record);
    return id;
  }

  @Override
  public void run(){
    ApiClient client = new ApiClient();
    client.setBasePath(IPAddr);
    DefaultApi apiInstance = new DefaultApi(client);
    for(int i = 0; i < 1000; i++) {
      try {
        String id = getRuntime(apiInstance, "POST", null);
        if(id == null){
          throw new InterruptedException();
        }
        getRuntime(apiInstance, "GET", id);
      } catch (InterruptedException e) {
        System.out.println(e.getMessage());
      }
    }
    this.success_global.getAndAdd(success);
    this.failure_global.getAndAdd(failure);
    this.completed.countDown();
  }
}
