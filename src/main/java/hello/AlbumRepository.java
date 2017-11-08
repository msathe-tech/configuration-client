package hello;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

public interface AlbumRepository extends CrudRepository<Album, String> {
	
}
