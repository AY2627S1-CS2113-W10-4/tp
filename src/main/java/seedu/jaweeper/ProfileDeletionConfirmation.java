package seedu.jaweeper;

/**
 * Remembers the inactive profile shown in the latest deletion warning for one session.
 * A list number alone is insufficient because removing a profile renumbers later entries.
 */
public final class ProfileDeletionConfirmation {

    private String profileId;
    private String profileName;
    private int profileNumber;

    /**
     * Accepts an explicit confirmation only for the same ID, displayed name and number
     * as the previous warning. Otherwise remembers the new target and asks the caller
     * to show a warning. An accepted request is consumed before the registry is written,
     * so repeating a command cannot silently delete the next player or retry a failed write.
     *
     * @return True when the caller may delete the previously displayed profile.
     */
    public boolean confirm(Profile target, int number, boolean explicitConfirmation) {
        boolean matchesWarning = target.getId().equals(profileId)
                && target.getName().equals(profileName) && number == profileNumber;
        clear();
        if (explicitConfirmation && matchesWarning) {
            return true;
        }
        profileId = target.getId();
        profileName = target.getName();
        profileNumber = number;
        return false;
    }

    /**
     * Forgets the warning, for example when a different player is selected.
     */
    public void clear() {
        profileId = null;
        profileName = null;
        profileNumber = 0;
    }
}
