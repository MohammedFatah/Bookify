package com.bookify.repository;

import com.bookify.entity.ShowCategoryPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShowCategoryPriceRepository extends JpaRepository<ShowCategoryPrice, UUID> {

}
