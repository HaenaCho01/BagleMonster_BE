package com.example.baglemonster.common.s3.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;
	private final AmazonS3 amazonS3;

	// AWS S3 업로드
	public String uploadFile(MultipartFile multipartFile) throws IOException {
		// S3에 저장되는 파일의 이름이 중복되지 않기 위해서 UUID로 생성한 랜덤 값과 파일 이름을 연결하여 S3에 업로드
		String s3FileName = UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();

		// Spring Server에서 S3로 파일을 업로드해야 하는데,
		// 이 때 파일의 사이즈를 ContentLength로 S3에 알려주기 위해서 ObjectMetadata를 사용
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentLength(multipartFile.getInputStream().available());

		// S3 API 메소드인 putObject를 이용하여 파일 Stream을 열어서 S3에 파일을 업로드
		amazonS3.putObject(bucket, s3FileName, multipartFile.getInputStream(), objMeta);

		// getUrl 메소드를 통해서 S3에 업로드된 사진 URL을 가져오는 방식
		return amazonS3.getUrl(bucket, s3FileName).toString();
	}

	// AWS S3 삭제
	public void deleteFile(String mediaUrl) {
		if(mediaUrl == null) return;

		StringBuilder address = new StringBuilder("https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/");
		String fileName = mediaUrl.replace(address, "");
		String decodedUrl = null;
		try {
			decodedUrl = URLDecoder.decode(fileName, "UTF-8");
			System.out.println(decodedUrl);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		amazonS3.deleteObject(bucket, decodedUrl);
	}
}