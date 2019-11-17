package com.xuecheng.manage_cms_client.service;

import org.springframework.stereotype.Service;

@Service
public class PageService {


    /**
     * 保存页面到服务器物理路径
     * @param pageId
     */
    public void savePageToServerPath(String pageId){
        String s = "D:/Server/nginx-1.14.2/xc-ui-pc-static-portal/include";
        //从gridfs下载文件
        //拼装服务器物理路径
        //保存文件
    }

}
