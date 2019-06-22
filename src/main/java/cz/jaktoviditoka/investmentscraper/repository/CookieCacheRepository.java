package cz.jaktoviditoka.investmentscraper.repository;

import cz.jaktoviditoka.investmentscraper.entity.CookieCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CookieCacheRepository extends JpaRepository<CookieCache, Long> {

}
