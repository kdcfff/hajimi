package com.hajimi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {

    /***
     *
     */
    private String searchText;

    private Integer count=10;

    private Long spaceId;

    /**
     * 名称前缀
     */
    private String namePrefix;


    private static final long serialVersionUID = 1L;  
}
