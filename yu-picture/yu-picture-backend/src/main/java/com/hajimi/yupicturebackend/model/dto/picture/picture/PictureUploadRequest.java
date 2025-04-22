package com.hajimi.yupicturebackend.model.dto.picture.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;

    private String fileUrl;

    private Long spaceId;

    /**
     * 名称前缀
     */
    private String picName;

    private static final long serialVersionUID = 1L;  
}
