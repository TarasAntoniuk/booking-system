package com.tarasantoniuk.unit.service;

import com.tarasantoniuk.common.exception.ResourceNotFoundException;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import com.tarasantoniuk.unit.dto.CreateUnitRequestDto;
import com.tarasantoniuk.unit.dto.UnitResponseDto;
import com.tarasantoniuk.unit.dto.UnitSearchCriteriaDto;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UnitService {

    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final UnitStatisticsService unitStatisticsService;

    @Transactional
    public UnitResponseDto createUnit(CreateUnitRequestDto request) {
        log.info("Creating unit: type={}, rooms={}, ownerId={}",
                request.getAccommodationType(), request.getNumberOfRooms(), request.getOwnerId());

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found with id: " + request.getOwnerId()));

        Unit unit = new Unit();
        unit.setNumberOfRooms(request.getNumberOfRooms());
        unit.setAccommodationType(request.getAccommodationType());
        unit.setFloor(request.getFloor());
        unit.setBaseCost(request.getBaseCost());
        unit.setDescription(request.getDescription());
        unit.setOwner(owner);

        Unit saved = unitRepository.save(unit);

        // Create event for unit creation
        eventService.createEvent(EventType.UNIT_CREATED, saved.getId());

        unitStatisticsService.invalidateAvailableUnitsCache();

        log.info("Unit created successfully: unitId={}, ownerId={}", saved.getId(), owner.getId());

        return UnitResponseDto.from(saved);
    }

    public UnitResponseDto getUnitById(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));
        return UnitResponseDto.from(unit);
    }

    public Page<UnitResponseDto> searchUnits(UnitSearchCriteriaDto criteria, Pageable pageable) {
        Specification<Unit> spec = UnitSpecification.withCriteria(criteria);
        return unitRepository.findAll(spec, pageable)
                .map(UnitResponseDto::from);
    }

    public Page<UnitResponseDto> getAllUnits(Pageable pageable) {
        return unitRepository.findAll(pageable)
                .map(UnitResponseDto::from);
    }
}