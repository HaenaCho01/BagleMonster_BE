package com.example.baglemonster.store.service;

import com.example.baglemonster.common.exception.NotFoundException;
import com.example.baglemonster.common.exception.UnauthorizedException;
import com.example.baglemonster.common.s3.service.S3UploadService;
import com.example.baglemonster.product.entity.Product;
import com.example.baglemonster.store.dto.StoreRequestDto;
import com.example.baglemonster.store.dto.StoreResponseDto;
import com.example.baglemonster.store.dto.StoresResponseDto;
import com.example.baglemonster.store.entity.Store;
import com.example.baglemonster.store.repository.StoreRepository;
import com.example.baglemonster.user.entity.User;
import com.example.baglemonster.user.entity.UserRoleEnum;
import com.example.baglemonster.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final S3UploadService s3UploadService;
    private final UserRepository userRepository;

    // 가게 전체 조회
    @Transactional(readOnly = true)
    public StoresResponseDto selectStores() {
        List<Store> stores = storeRepository.findAll().stream().toList();
        return StoresResponseDto.of(stores);
    }

    // 가게 단일 조회
    @Transactional(readOnly = true)
    public StoreResponseDto selectStore(Long storeId) {
        Store store = findStore(storeId);
        return StoreResponseDto.of(store, true);
    }

    // 가게 등록
    @Transactional
    public void createStore(StoreRequestDto storeRequestDto, MultipartFile file, User user) throws IOException {
        findStoreByUserId(user.getId());

        if (!user.getRole().getAuthority().equals(UserRoleEnum.STORE.getAuthority())) {
            throw new UnauthorizedException("가게 등록에 대한 권한이 없습니다.");
        }

        String storePictureUrl;
        if (file == null) {
            storePictureUrl = null;
        } else {
            storePictureUrl = s3UploadService.uploadFile(file);
        }

        Store store = storeRequestDto.toEntity(user, storePictureUrl);
        storeRepository.save(store);
    }

    // 내 가게 조회
    @Transactional(readOnly = true)
    public StoreResponseDto selectMyStore(User user) {
        User storeUser = findUser(user.getId());
        Store store = storeRepository.findByUser(storeUser);

        if (store == null) {
            throw new NotFoundException("가게가 존재하지 않습니다.");
        }

        return StoreResponseDto.of(store, true);
    }

    // 가게 수정
    @Transactional
    public void modifyStore(Long storeId, StoreRequestDto storeRequestDto, MultipartFile file, User user) throws IOException {
        // 관리자 수정 권한 협의 필요
        Store store = findStore(storeId);

        if (!store.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("가게 수정에 대한 권한이 없습니다.");
        }

        String currentPictureUrl = store.getStorePictureUrl();
        String storePictureUrl = currentPictureUrl;
        if (file != null) {
            s3UploadService.deleteFile(currentPictureUrl);
            storePictureUrl = s3UploadService.uploadFile(file);
        }

        store.editStore(storeRequestDto, storePictureUrl);
    }

    // 가게 삭제
    @Transactional
    public void deleteStore(Long storeId, User user) {
        // 관리자 삭제 권한 협의 필요
        Store store = findStore(storeId);

        if (!store.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("가게 삭제에 대한 권한이 없습니다.");
        }

        String storePictureUrl = store.getStorePictureUrl();
        if (storePictureUrl != null) {
            s3UploadService.deleteFile(storePictureUrl);
        }

        List<Product> products = store.getProducts();
        for (Product product : products) {
            s3UploadService.deleteFile(product.getProductPictureUrl());
        }

        storeRepository.delete(store);
    }

    // ID로 가게 찾기
    public Store findStore(Long storeId) {
        return storeRepository.findById(storeId).orElseThrow(() ->
                new NotFoundException("선택한 가게는 존재하지 않습니다.")
        );
    }

    public void findStoreByUserId(Long userId) {
        Store store = storeRepository.findByUserId(userId).orElse(null);

        if (store != null)
            throw new IllegalArgumentException("이미 등록한 가게가 존재합니다.");
    }

    // ID로 유저 찾기
    private User findUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
