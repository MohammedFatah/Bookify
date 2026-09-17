package com.bookify.service;

import com.bookify.entity.Show;
import com.bookify.entity.ShowCategoryPrice;
import com.bookify.enums.SeatCategory;
import com.bookify.repository.ShowCategoryPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowCategoryPriceService {
    private final ShowCategoryPriceRepository showCategoryPriceRepository;

    public void addShowCategoryPrices(Show show, Map<SeatCategory, BigDecimal> prices) {
        List<ShowCategoryPrice> showCategoryPriceList = prices.entrySet().stream()
                .map(entry -> ShowCategoryPrice.builder()
                        .show(show)
                        .seatCategory(entry.getKey())
                        .price(entry.getValue())
                        .build())
                .toList();

        showCategoryPriceRepository.saveAll(showCategoryPriceList);
    }

    public List<ShowCategoryPrice> getShowCategoryPrices(UUID showId) {
        return showCategoryPriceRepository.findAllByShowId(showId);
    }

}
