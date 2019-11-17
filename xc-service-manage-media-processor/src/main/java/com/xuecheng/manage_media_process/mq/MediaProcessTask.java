package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessTask.class);

    @Value("${xc-service-manage-media.ffmpeg-path}")
    private String ffmpegPath;

    @Value("${xc-service-manage-media.video-location}")
    private String videoLocation;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @RabbitListener(queues = "${xc-service-manage-media.mq.queue-media-video-processor}",containerFactory = "customContainerFactory")
    public void receiveMediaProcessTask(String msg, Message message, Channel channel){
        //1.接受rabbitmq的消息
        Map map = JSON.parseObject(msg, Map.class);
        String mediaId = (String) map.get("mediaId");
        //2.通过消息查询需要处理文件的信息
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if (!optional.isPresent()){
            LOGGER.error("rabbitmq msg error");
            return;
        }
        MediaFile mediaFile = optional.get();
        //3.通过文件信息处理文件
        String fileType = mediaFile.getFileType();
        if(!"avi".equalsIgnoreCase(fileType)){
            //不是avi文件无需处理
            mediaFile.setProcessStatus("303004");
            mediaFileRepository.save(mediaFile);
            return;
        }else{
            //修改状态为处理中
            mediaFile.setProcessStatus("303001");
            mediaFileRepository.save(mediaFile);
        }
        //使用工具类转换成mp4格式文件
        // String ffmpeg_path, String video_path, String mp4_name, String mp4folder_path
        String videoPath = videoLocation + mediaFile.getFilePath() + mediaFile.getFileName();
        String mp4Name = mediaFile.getFileId() + ".mp4";
        String mp4FolderPath = videoLocation + mediaFile.getFilePath();
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegPath,videoPath,mp4Name,mp4FolderPath);
        String generateMp4 = mp4VideoUtil.generateMp4();
        if(!"success".equalsIgnoreCase(generateMp4) || generateMp4 == null){
            //处理失败
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(generateMp4);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        // 处理成功MP4生成m3u8和ts文件
        // String ffmpeg_path, String video_path, String m3u8_name,String m3u8folder_path
        String videoPathMp4 = videoLocation + mediaFile.getFilePath() + mp4Name;
        String m3u8Name = mediaFile.getFileId()+".m3u8";
        String m3u8FolderPath = videoLocation + mediaFile.getFilePath()+"hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpegPath,videoPathMp4,m3u8Name,m3u8FolderPath);
        String generateM3u8 = hlsVideoUtil.generateM3u8();
        if(!"success".equalsIgnoreCase(generateM3u8) || generateM3u8 == null){
            //处理失败
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(generateM3u8);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //更新文件信息
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        mediaFile.setProcessStatus("303002");
        //定义mediaFileProcess_m3u8
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);

        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        mediaFile.setFileUrl(mediaFile.getFilePath()+"hls/" + m3u8Name);
        mediaFileRepository.save(mediaFile);
    }
}
