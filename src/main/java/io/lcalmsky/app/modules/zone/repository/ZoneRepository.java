package io.lcalmsky.app.modules.zone.repository;

import io.lcalmsky.app.modules.account.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByCityAndProvinceAndLocalNameOfCity(String city, String province, String localNameOfCity);

}
