package com.disney.app.cruisesearchservice.mapper;

import com.disney.app.cruisesearchservice.dto.CruiseRequest;
import com.disney.app.cruisesearchservice.dto.CruiseResponse;
import com.disney.app.cruisesearchservice.entity.CruiseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CruiseMapper {

    CruiseResponse toCruiseResponse(CruiseEntity cruiseEntity);

    @Mapping(target = "id", ignore = true)
    CruiseEntity toCruiseEntity(CruiseRequest cruiseRequest);
}
