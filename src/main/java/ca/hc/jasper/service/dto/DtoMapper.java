package ca.hc.jasper.service.dto;

import ca.hc.jasper.domain.*;
import ca.hc.jasper.security.Auth;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class DtoMapper {

	@Autowired
	Auth auth;

	@Mapping(target = "qualifiedTags", ignore = true)
	public abstract RefDto domainToDto(Ref ref);

	@AfterMapping
	protected void filterTags(@MappingTarget RefDto refDto) {
		refDto.setTags(auth.filterTags(refDto.getTags()));
	}

	@Mapping(target = "qualifiedTags", ignore = true)
	public abstract FeedDto domainToDto(Feed ref);

	@AfterMapping
	protected void filterTags(@MappingTarget FeedDto feedDto) {
		feedDto.setTags(auth.filterTags(feedDto.getTags()));
	}

	public abstract UserDto domainToDto(User user);

	@AfterMapping
	protected void filterTags(@MappingTarget UserDto userDto) {
		userDto.setSubscriptions(auth.filterTags(userDto.getSubscriptions()));
		userDto.setReadAccess(auth.filterTags(userDto.getReadAccess()));
		userDto.setWriteAccess(auth.filterTags(userDto.getWriteAccess()));
	}

	public abstract OriginNameDto domainToDto(Origin origin);
}
