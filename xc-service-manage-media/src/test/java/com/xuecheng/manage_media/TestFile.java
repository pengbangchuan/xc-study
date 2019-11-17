package com.xuecheng.manage_media;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;

public class TestFile {

    //测试文件分块
    @Test
    public void testChunks() throws Exception {
        //源文件位置
        File sourceFile = new File("D:\\lucene.avi");

        //分块文件位置
        String chunkFileFolder = "D:\\develop\\chunks\\";

        //定义分块大小
        long chunkFileSize = 1*1024*1024;

        //文件的块数
        long chunkFileNum =(long) Math.ceil(sourceFile.length()*1.0/chunkFileSize);

        //定义读文件对象
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile,"r");

        //定义文件缓冲区
        byte[] buff = new byte[1024];
        for (int i = 0 ;i<chunkFileNum ;i++){
            int len = -1;
            File chunkFile = new File(chunkFileFolder+i);
            RandomAccessFile raf_witer = new RandomAccessFile(chunkFile,"rw");
            while ((len=raf_read.read(buff))!=-1){
                raf_witer.write(buff,0,len);
                if (raf_witer.length()>=chunkFileSize){
                    break;
                }
            }
            raf_witer.close();
        }
        raf_read.close();
    }

    //测试文件合并
    @Test
    public void testMergeFile() throws Exception {

    }
}
