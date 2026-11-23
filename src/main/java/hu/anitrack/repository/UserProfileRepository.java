package hu.anitrack.repository;

import hu.anitrack.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

// the id of a profile is the id of its user (1:1 with shared primary key)
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}