package org.example.youngnam.domain.image.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.youngnam.domain.image.dto.response.ImageResponseDTO;
import org.example.youngnam.domain.image.entity.Image;
import org.example.youngnam.domain.image.mapper.ImageMapper;
import org.example.youngnam.domain.image.repository.ImageRepository;
import org.example.youngnam.global.exception.exceptions.EntityNotFoundException;
import org.example.youngnam.global.exception.ErrorCode;
import org.example.youngnam.global.exception.exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ImageServiceImpl implements ImageService {
    private final ImageRepository imageRepository;
    private final AmazonS3Client amazonS3Client;
    private final ImageMapper imageMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.s3.preImgFolder}")
    private String preImgFolder;

    @Value("${cloud.aws.s3.finalImgFolder}")
    private String finalImgFolder;


    @Override
    @Transactional
    public ImageResponseDTO.ImagePreUrlSaveDTO uploadAndResizeAndSavePreImage(final Long userId, MultipartFile preImage) throws IOException {
        BufferedImage preThumbnail = resizeThumbnail(preImage);
        String preThumbnailUrl = uploadImageToS3(preImage, preImgFolder);
        String preThumbnailPath = preImgFolder + "/" + preImage.getOriginalFilename();

        return imageMapper.toPreUrlDTO(
                imageRepository.save(
                        Image.from(preImage, preThumbnailUrl, preThumbnailPath, preThumbnail, userId)
                )
        );
    }

    @Override
    @Transactional
    public ImageResponseDTO.ImageFinalUrlSaveDTO uploadAndResizeAndSaveFinalImage(final Long userId, Long imageId, MultipartFile finalImage) throws IOException {
        Image findImage = getImageByImageId(imageId);
        checkUnauthorized(imageId, userId);

        BufferedImage finalThumbnail = resizeThumbnail(finalImage);
        String finalThumbnailUrl = uploadImageToS3(finalImage, finalImgFolder);
        String finalThumbnailPath = finalImgFolder + "/" + finalImage.getOriginalFilename();

        findImage.updateFinalThumbnail(finalThumbnailUrl, finalThumbnailPath, finalThumbnail, finalImage);

        return imageMapper.toFinalUrlDTO(findImage);
    }

    private String uploadImageToS3(MultipartFile file, String folder) throws IOException {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String filePath = folder + "/" + fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        amazonS3Client.putObject(new PutObjectRequest(bucket, filePath, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        return amazonS3Client.getUrl(bucket, filePath).toString();
    }

    private BufferedImage resizeThumbnail(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        BufferedImage thumbnail = new BufferedImage(200, 150, originalImage.getType());
        Graphics2D g = thumbnail.createGraphics();
        g.drawImage(originalImage, 0, 0, 200, 150, null);
        g.dispose();
        return thumbnail;
    }

    private Image getImageByImageId(final Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NOT_FOUND_IMAGE));
    }

    private void checkUnauthorized(final Long imageId, final Long userId) {
        if (!imageRepository.existsByImageIdAndUserId(imageId, userId)) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_IMAGE);
        }
    }
}
