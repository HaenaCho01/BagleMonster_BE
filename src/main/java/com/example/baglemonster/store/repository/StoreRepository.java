package com.example.baglemonster.store.repository;

import com.example.baglemonster.store.entity.Store;
import com.example.baglemonster.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Store findByUser(User user);
}
