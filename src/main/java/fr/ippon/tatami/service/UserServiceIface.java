package fr.ippon.tatami.service;

import java.util.Collection;
import java.util.List;

import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.service.dto.UserDTO;

public interface UserServiceIface {

    public abstract User getUserByLogin(String login);

    public abstract String getLoginByRssUid(String rssUid);

    public abstract User getUserByUsername(String username);

    /**
     * Return a collection of Users based on their username (ie : uid)
     *
     * @param logins the collection : must not be null
     * @return a Collection of User
     */
    public abstract Collection<User> getUsersByLogin(Collection<String> logins);

    public abstract List<User> getUsersForCurrentDomain(int pagination);

    public abstract void updateUser(User user);

    public abstract void updatePassword(User user);

    public abstract void updateThemePreferences(String theme);

    public abstract void updateEmailPreferences(boolean preferencesMentionEmail);

    public abstract void createUser(User user);

    public abstract void createTatamibot(String domain);

    public abstract void deleteUser(User user);

    /**
     * Creates a User and sends a registration e-mail.
     */
    public abstract void registerUser(User user);

    public abstract void lostPassword(User user);

    public abstract String validateRegistration(String key);

    /**
     * update registration to weekly digest email.
     */
    public abstract void updateWeeklyDigestRegistration(boolean registration);

    /**
     * Update registration to daily digest email.
     */
    public abstract void updateDailyDigestRegistration(boolean registration);

    /**
     * Activate of de-activate rss publication for the timeline.
     *
     * @return the rssUid used for rss publication, empty if no publication
     */
    public abstract String updateRssTimelinePreferences(boolean booleanPreferencesRssTimeline);

    /**
     * Is the domain managed by a LDAP repository?
     */
    public abstract boolean isDomainHandledByLDAP(String domain);

    public abstract Collection<UserDTO> buildUserDTOList(Collection<User> users);

    public abstract UserDTO buildUserDTO(User user);

}