package com.hajimi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.hajimi.yupicturebackend.exception.BusinessException;
import com.hajimi.yupicturebackend.exception.ErrorCode;
import com.hajimi.yupicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static cn.hutool.http.HttpStatus.HTTP_OK;

@Slf4j
@Service
public class UrlUploadImpl extends PictureUploadTemplate {
    @Override
    protected File processFile(Object source, File file) {
        String fileUrl = (String) source;
        HttpUtil.downloadFile(fileUrl, file);
        return file;
    }

    @Override
    protected void validPicture(Object source) {
        String fileUrl = (String) source;
        // 校验url非空
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "url不能为空");
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "url格式错误");
        }
        // 检验url协议是否为http或https
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持http和https协议");
        //发送header请求查询是否存在此图片
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();) {
            if (httpResponse.getStatus() != HTTP_OK) {
                return;
            }
            //文件存在文件类型检验
            String contentType = httpResponse.header("Content-Type");
            log.info("contentType:{}", contentType);
            if (contentType!=null){
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                final long TWO_MB = 2*1024 * 1024L;
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }
    }

    @Override
    protected String getOriginFilename(Object source) {
        String fileUrl = (String) source;

        return FileUtil.mainName(fileUrl)+"." + FileUtil.getSuffix(fileUrl);
    }
}
