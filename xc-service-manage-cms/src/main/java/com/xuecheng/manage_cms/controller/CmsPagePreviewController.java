package com.xuecheng.manage_cms.controller;

import com.xuecheng.framework.web.BaseController;
import com.xuecheng.manage_cms.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
 * 页面预览
 */
@Controller
public class CmsPagePreviewController extends BaseController {

    @Autowired
    private PageService pageService;

    /**
     * 页面预览
     * @param pageId
     */
    @RequestMapping(value ="/cms/preview/{id}" ,method = RequestMethod.GET)
    public void preview(@PathVariable("id") String pageId){
        try {
            String pageHtml = pageService.getPageHtml(pageId);
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("text/html;charset=UTF-8");
            outputStream.write(pageHtml.getBytes("utf-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
