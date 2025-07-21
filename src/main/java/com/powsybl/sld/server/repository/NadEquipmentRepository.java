package com.powsybl.sld.server.repository;

import com.powsybl.sld.server.entities.nad.NadEquipmentPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@Repository
public interface NadEquipmentRepository extends JpaRepository<NadEquipmentPositionEntity, UUID> {
}
