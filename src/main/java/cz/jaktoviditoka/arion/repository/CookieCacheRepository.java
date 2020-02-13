package cz.jaktoviditoka.arion.repository;

import cz.jaktoviditoka.arion.entity.CookieCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CookieCacheRepository extends JpaRepository<CookieCache, Long> {

}
