package cz.tomastokamrazek.arion.repository;

import cz.tomastokamrazek.arion.entity.CookieCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CookieCacheRepository extends JpaRepository<CookieCache, Long> {

}
