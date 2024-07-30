package io.lcalmsky.app.modules.zone.application;

import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;

    @PostConstruct
    public void initZoneData() throws IOException {
        if (zoneRepository.count() == 0) {
            Resource resource = new ClassPathResource("zones_kr.csv");
            List<String> allLines = Files.readAllLines(resource.getFile().toPath(), StandardCharsets.UTF_8);
            List<Zone> zones = allLines.stream().map(Zone::map).collect(Collectors.toList());
            zoneRepository.saveAll(zones);
        }
    }

    public Zone findOrCreateNew(String cityName, String provinceName, String localNameOfCity) {
        return zoneRepository.findByCityAndProvinceAndLocalNameOfCity(cityName, provinceName, localNameOfCity)
                .orElseGet(() -> zoneRepository.save(
                        Zone.builder()
                                .city(cityName)
                                .province(provinceName)
                                .localNameOfCity(localNameOfCity)
                                .build()
                ));
    }
}

