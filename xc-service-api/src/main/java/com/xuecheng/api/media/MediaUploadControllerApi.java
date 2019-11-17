package com.xuecheng.api.media;

import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

@Api(value="媒资管理接口",description = "媒资资管理接口，提供文件上传，文件处理等接")
public interface MediaUploadControllerApi {

    @ApiOperation("文件上传注册")
    public ResponseResult registerFile(String fileMd5,
                                       String fileName,
                                       Long fileSize,
                                       String mimetype,
                                       String fileExt);

    @ApiOperation("检查分块文件是否存在")
    public CheckChunkResult checkchunk(String fileMd5,
                                       int chunk,
                                       int chunkSize);

    @ApiOperation("上传分块")
    public ResponseResult uploadchunk(MultipartFile file,
                                      int chunk,
                                      String fileMd5);

    @ApiOperation("合并分块")
    public ResponseResult mergechunks(String fileMd5,
                                      String fileName,
                                      Long fileSize,
                                      String mimetype,
                                      String fileExt);


}
