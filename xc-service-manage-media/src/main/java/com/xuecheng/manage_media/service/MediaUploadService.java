package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Value("${xc-service-manage-media.upload-location}")
    private String upload_location;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //路由key  routingkey
    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    private String routingkeyMediaVideo;


    /**
     * 文件注册
     * @param fileMd5
     * @param fileName
     * @param fileSize
     * @param mimetype
     * @param fileExt
     * @return
     */
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //检验磁盘路径是否存在
        //获得文件路径
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        //获得文件
        String filePath = this.getFilePath(fileMd5, fileExt);
        File file = new File(filePath);
        boolean exists = file.exists();
        //检验mongo文件是否存在
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        if (exists && optional.isPresent()){
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        //准备文件上传环境
        File fileFolder = new File(fileFolderPath);
        if(!fileFolder.exists()){
            fileFolder.mkdirs();
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 文件分块检查
     * @param fileMd5
     * @param chunk
     * @param chunkSize
     * @return
     */
    public CheckChunkResult checkchunk(String fileMd5, int chunk, int chunkSize) {
        //得到分块文件路径
        String fileFolderChunkPath = this.getFileFolderChunkPath(fileMd5);
        //得到分块文件
        File file = new File(fileFolderChunkPath+chunk);
        if (file.exists()){
            return new CheckChunkResult(CommonCode.SUCCESS,true);
        }else{
            return new CheckChunkResult(CommonCode.SUCCESS,false);
        }
    }

    /**
     * 分块文件上传
     * @param file
     * @param chunk
     * @param fileMd5
     * @return
     */
    public ResponseResult uploadchunk(MultipartFile file, int chunk, String fileMd5) {
        //创建chunk目录
        String fileFolderChunkPath = this.getFileFolderChunkPath(fileMd5);
        File chunkFilePath = new File(fileFolderChunkPath);
        if (!chunkFilePath.exists()){
            chunkFilePath.mkdirs();
        }
        //块文件
        String chunkFile = fileFolderChunkPath+chunk;
        //得到文件输入流
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = file.getInputStream();
            fileOutputStream = new FileOutputStream(chunkFile);
            //IO工具类拷贝文件，输入流拷贝到输出流
            IOUtils.copy(inputStream,fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 合并文件
     * @param fileMd5
     * @param fileName
     * @param fileSize
     * @param mimetype
     * @param fileExt
     * @return
     */
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //1.合并文件
        //合并文件路径
        String fileFolderChunkPath = this.getFileFolderChunkPath(fileMd5);
        File chunkFileder = new File(fileFolderChunkPath);
        //获得分块路径下的所有文件
        File[] files = chunkFileder.listFiles();
        //文件数组传成集合排序
        List<File> fileList = Arrays.asList(files);

        //创建合并文件
        String filePath = this.getFilePath(fileMd5, fileExt);
        File mergeFile = new File(filePath);

        //执行合并
        mergeFile = this.mergecFile(fileList, mergeFile);
        if (mergeFile == null){
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        //2.校验合并文件和上传文件的md5值
        boolean checkFileMd5 = this.checkFileMd5(mergeFile, fileMd5);
        if (!checkFileMd5){
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }

        // 3.保存文件信息到mongo数据库中
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileOriginalName(fileName);
        mediaFile.setFileName(fileMd5+"."+fileExt);
        String filePath1 = fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
        mediaFile.setFilePath(filePath1);
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //保存成功状态码
        mediaFile.setFileStatus("301002");
        //保存文件信息
        mediaFileRepository.save(mediaFile);
        this.sendProcessVideoMsg(mediaFile.getFileId());
        return new ResponseResult(CommonCode.SUCCESS);
    }




    //-------------------------------------------------------------------------------------------------




    private String getFileFolderPath(String fileMd5){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
    }

    private String getFilePath(String fileMd5,String fileExt){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+"."+fileExt;
    }

    private String getFileFolderChunkPath(String fileMd5){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+"/chunk/";
    }

    private File mergecFile(List<File> fileList,File mergeFile){
        try {
            //判断合并文件是否存在
            if (mergeFile.exists()){
                //存在删除
                mergeFile.delete();
            }else{
                //不存在创建
                mergeFile.createNewFile();
            }
            //排序块文件
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if(Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                        return 1;
                    }
                    return -1;
                }
            });

            //创建写文件对象
            RandomAccessFile rafWrite = new RandomAccessFile(mergeFile,"rw");
            //创建文件缓冲区
            byte[] b = new byte[1024];
            for (File chunkFile:fileList){
                //创建文件读对象
                RandomAccessFile rafRead = new RandomAccessFile(chunkFile,"r");
                int len = -1;
                while ((len=rafRead.read(b))!=-1){
                    rafWrite.write(b,0,len);
                }
                rafRead.close();
            }
            rafWrite.close();
            return mergeFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkFileMd5(File mergeFile, String fileMd5) {
        try {
            FileInputStream fileInputStream = new FileInputStream(mergeFile);
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            //校验MD5值不区分大小写
            if (fileMd5.equalsIgnoreCase(md5Hex)){
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送消息
     *
     * @param mediaId
     * @return
     */
    private ResponseResult sendProcessVideoMsg(String mediaId){
        //查询数据库验证是否有文件信息
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if (!optional.isPresent()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //组装消息把消息转换成json
        Map<String,String> map = new HashMap<String,String>();
        map.put("mediaId",mediaId);
        String msg = JSON.toJSONString(map);
        //发送消息
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkeyMediaVideo,msg);
        } catch (Exception e){
            //发送消息失败
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

}
