package cn.haodian.demowidget.filedownload;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by 立才 on 2017/8/28.
 */

public class FileDownload {

    private String file_url="http://mpge.5nd.com/2016/2016-11-15/74847/1.mp3";
    private String target_path="D:/";
    private int threadCount=3;


    private int connectionLength=0;

    private int size=0;

    public FileDownload(String file_url, String target_path, int threadCount) {
//        this.file_url = file_url;
//        this.target_path = target_path;
        this.threadCount = threadCount;
    }


    public void download() throws Exception {
        URL url=new URL(file_url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);

        int code = connection.getResponseCode();
        if(code == 200){
            //获取资源大小
             connectionLength = connection.getContentLength();
            System.out.println(connectionLength);
            //在本地创建一个与资源同样大小的文件来占位
            RandomAccessFile randomAccessFile = new RandomAccessFile(new File(target_path,getFileName(url)), "rw");
            randomAccessFile.setLength(connectionLength);
            /*
             * 将下载任务分配给每个线程
             */
            int blockSize = connectionLength/threadCount;//计算每个线程理论上下载的数量.
            for(int threadId = 0; threadId < threadCount; threadId++){//为每个线程分配任务
                int startIndex = threadId * blockSize; //线程开始下载的位置
                int endIndex = (threadId+1) * blockSize -1; //线程结束下载的位置
                if(threadId == (threadCount - 1)){  //如果是最后一个线程,将剩下的文件全部交给这个线程完成
                    endIndex = connectionLength - 1;
                }
                new DownloadThread(threadId, startIndex, endIndex).start();//开启线程下载

            }
//          randomAccessFile.close();
        }
    }




    //下载的线程
    private class DownloadThread extends Thread{

        private int threadId;
        private int startIndex;
        private int endIndex;

        public DownloadThread(int threadId, int startIndex, int endIndex) {
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public void run() {
            System.out.println("线程"+ threadId + "开始下载");
            try {
                //分段请求网络连接,分段将文件保存到本地.
                URL url = new URL(file_url);

                //加载下载位置的文件
                File downThreadFile = new File(target_path,"downThread_" + threadId+".dt");
                RandomAccessFile downThreadStream = null;
                if(downThreadFile.exists()){//如果文件存在
                    downThreadStream = new RandomAccessFile(downThreadFile,"rwd");
                    String startIndex_str = downThreadStream.readLine();
                    if(null==startIndex_str||"".equals(startIndex_str)){
                        this.startIndex=startIndex;
                    }else{
                        this.startIndex = Integer.parseInt(startIndex_str)-1;//设置下载起点
                    }
                }else{
                    downThreadStream = new RandomAccessFile(downThreadFile,"rwd");
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);

                //设置分段下载的头信息。  Range:做分段数据请求用的。格式: Range bytes=0-1024  或者 bytes:0-1024
                connection.setRequestProperty("Range", "bytes="+ startIndex + "-" + endIndex);

                System.out.println("线程_"+threadId + "的下载起点是 " + startIndex + "  下载终点是: " + endIndex);

                if(connection.getResponseCode() == 206){//200：请求全部资源成功， 206代表部分资源请求成功
                    InputStream inputStream = connection.getInputStream();//获取流
                    RandomAccessFile randomAccessFile = new RandomAccessFile(
                            new File(target_path,getFileName(url)), "rw");//获取前面已创建的文件.
                    randomAccessFile.seek(startIndex);//文件写入的开始位置.


                    /*
                     * 将网络流中的文件写入本地
                     */
                    byte[] buffer = new byte[1024];
                    int length = -1;
                    int total = 0;//记录本次下载文件的大小
                    while((length = inputStream.read(buffer)) > 0){
                        randomAccessFile.write(buffer, 0, length);
                        System.out.println("已下载:" +size+"".getBytes("UTF-8")+ "+++++"+size*100/connectionLength);
                        total += length;
                        size+=length;
                        /*
                         * 将当前现在到的位置保存到文件中
                         */
                        downThreadStream.seek(0);
                        downThreadStream.write((startIndex + total + "").getBytes("UTF-8"));
                    }

                    downThreadStream.close();
                    inputStream.close();
                    randomAccessFile.close();
                    cleanTemp(downThreadFile);//删除临时文件
                    System.out.println("线程"+ threadId + "下载完毕");

                }else{
                    System.out.println("响应码是" +connection.getResponseCode() + ". 服务器不支持多线程下载");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    //删除线程产生的临时文件
    private synchronized void cleanTemp(File file){
        file.delete();
    }


    //获取下载文件的名称
    private String getFileName(URL url){
        String filename = url.getFile();
        return filename.substring(filename.lastIndexOf("/")+1);
    }

    public static void main(String[] args) {
        try {
            new FileDownload(null, null, 3).download();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
