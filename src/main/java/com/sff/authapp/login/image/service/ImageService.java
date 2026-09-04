package com.sff.authapp.login.image.service;

import com.sff.authapp.login.image.dto.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    ImageUploadResponse uploadImage(
            MultipartFile file
    );

    void deleteImage(
            String publicId
    );
}